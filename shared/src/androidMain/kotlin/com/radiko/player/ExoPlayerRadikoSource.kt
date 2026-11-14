package com.radiko.player

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import android.util.Log
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.HttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.upstream.DefaultLoadErrorHandlingPolicy
import androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy
import androidx.media3.session.MediaSession
import com.radiko.platform.PlatformPreferences
import com.radiko.settings.AudioFocusBehavior
import com.radiko.settings.SettingsKeys
import com.radiko.station.ProgramEntry
import com.radiko.station.Station
import com.radiko.station.logoUrl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class AndroidExoRadioPlayer(
    private val context: Context,
) : RadioPlayer {
    private val playbackState = MutableStateFlow(false)
    override val isPlaying: StateFlow<Boolean> = playbackState

    private val failureEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    override val playbackFailures: Flow<Unit> = failureEvents.asSharedFlow()
    private var lastLiveWindowRecoveryAt = 0L

    private val metadataScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val logoArtworkCache = mutableMapOf<String, ByteArray?>()

    private var exoPlayer: ExoPlayer? = null
    private var mediaSession: MediaSession? = null

    override suspend fun play(request: StreamPlaybackRequest) {
        withContext(Dispatchers.Main) {
            val player = ensurePlayer()
            applyAudioFocusBehavior(player)

            val dataSourceFactory = DefaultHttpDataSource.Factory()
                .setAllowCrossProtocolRedirects(true)
                .setDefaultRequestProperties(request.headers)

            val mediaItem = MediaItem.Builder()
                .setUri(request.streamUrl)
                .setMediaMetadata(
                    buildMetadata(
                        station = request.station,
                        program = null,
                        artworkData = logoArtworkCache[request.station.id],
                    ),
                )
                .build()

            val mediaSource = HlsMediaSource.Factory(dataSourceFactory)
                .setLoadErrorHandlingPolicy(LiveStreamLoadErrorPolicy())
                .createMediaSource(mediaItem)

            player.setMediaSource(mediaSource)
            mediaSession?.let(AndroidAudioService::registerSession)
            ensureServiceRunning()

            player.prepare()
            player.playWhenReady = true
        }
    }

    override fun updateMetadata(station: Station, program: ProgramEntry?) {
        metadataScope.launch {
            val artworkData = withContext(Dispatchers.IO) {
                logoArtworkCache.getOrPut(station.id) {
                    loadPaddedLogoArtwork(station.logoUrl)
                }
            }

            val player = exoPlayer ?: return@launch
            val currentItem = player.currentMediaItem ?: return@launch
            val index = player.currentMediaItemIndex
            if (index < 0) {
                return@launch
            }

            val updatedItem = currentItem.buildUpon()
                .setMediaMetadata(buildMetadata(station, program, artworkData))
                .build()
            player.replaceMediaItem(index, updatedItem)
        }
    }

    override fun stop() {
        releasePlayer()
    }

    private fun buildMetadata(
        station: Station,
        program: ProgramEntry?,
        artworkData: ByteArray?,
    ): MediaMetadata =
        MediaMetadata.Builder()
            .setTitle(station.name)
            .setDisplayTitle(station.name)
            .setArtist(program?.title ?: station.name)
            .setArtworkUri(Uri.parse(station.logoUrl))
            .apply {
                if (artworkData != null) {
                    setArtworkData(artworkData, MediaMetadata.PICTURE_TYPE_FRONT_COVER)
                }
            }
            .build()

    private fun releasePlayer() {
        metadataScope.coroutineContext.cancelChildren()
        val session = mediaSession
        AndroidAudioService.unregisterSession(session)
        exoPlayer?.stop()
        exoPlayer?.release()
        exoPlayer = null
        mediaSession?.release()
        mediaSession = null
        context.stopService(Intent(context, AndroidAudioService::class.java))
        playbackState.value = false
    }

    private fun ensurePlayer(): ExoPlayer {
        exoPlayer?.let { player ->
            mediaSession?.let(AndroidAudioService::registerSession)
            return player
        }

        val player = ExoPlayer.Builder(context)
            // 锁屏挂机时让 CPU 和 Wi-Fi 保持清醒，否则网络流过几分钟就可能被省电断掉。
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .build()
            .also { exoPlayer = it }
        applyAudioFocusBehavior(player)
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                playbackState.value = isPlaying
            }

            override fun onPlayerError(error: PlaybackException) {
                handlePlayerError(player, error)
            }
        })

        val session = MediaSession.Builder(context, player).build().also { mediaSession = it }
        AndroidAudioService.registerSession(session)
        return player
    }

    /**
     * 直播落后太多（断网恢复后常见）就跳回直播点接着放，这个不用惊动上层。
     * 其余错误：只要用户本来就想听（playWhenReady），交给 ViewModel 换令牌重连。
     */
    private fun handlePlayerError(player: ExoPlayer, error: PlaybackException) {
        Log.w(TAG, "playback error ${error.errorCodeName}", error)
        val now = SystemClock.elapsedRealtime()
        if (error.errorCode == PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW &&
            now - lastLiveWindowRecoveryAt > LIVE_WINDOW_RECOVERY_INTERVAL_MS
        ) {
            lastLiveWindowRecoveryAt = now
            player.seekToDefaultPosition()
            player.prepare()
            return
        }
        if (player.playWhenReady) {
            failureEvents.tryEmit(Unit)
        }
    }

    /**
     * App 在前台时用普通 startService 把服务拉起来，等真正开始缓冲时由 media3 自己转前台；
     * 这样就算流一上来就失败，也不会因为「startForegroundService 之后没调 startForeground」被系统杀掉。
     * 在后台（闹钟唤起）才用 startForegroundService；系统不允许时宁可没有通知也不闪退。
     */
    private fun ensureServiceRunning() {
        if (AndroidAudioService.instance != null) {
            return
        }
        val intent = Intent(context, AndroidAudioService::class.java)
        runCatching {
            if (isAppVisible() || Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                context.startService(intent)
            } else {
                context.startForegroundService(intent)
            }
        }.onFailure { Log.w(TAG, "could not start playback service", it) }
    }

    private fun isAppVisible(): Boolean {
        val info = ActivityManager.RunningAppProcessInfo()
        ActivityManager.getMyMemoryState(info)
        return info.importance <= ActivityManager.RunningAppProcessInfo.IMPORTANCE_VISIBLE
    }

    private fun applyAudioFocusBehavior(player: ExoPlayer) {
        val behavior = audioFocusBehavior()
        val contentType = if (behavior == AudioFocusBehavior.PAUSE) {
            C.AUDIO_CONTENT_TYPE_SPEECH
        } else {
            C.AUDIO_CONTENT_TYPE_MUSIC
        }
        player.setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(contentType)
                .build(),
            true,
        )
    }

    private fun audioFocusBehavior(): AudioFocusBehavior {
        val rawValue = PlatformPreferences.getString(
            SettingsKeys.AUDIO_FOCUS_BEHAVIOR,
            AudioFocusBehavior.DUCK.name,
        )
        return AudioFocusBehavior.entries.firstOrNull { it.name == rawValue }
            ?: AudioFocusBehavior.DUCK
    }

    private fun loadPaddedLogoArtwork(logoUrl: String): ByteArray? {
        val connection = (URL(logoUrl).openConnection() as? HttpURLConnection) ?: return null
        return runCatching {
            connection.connectTimeout = 5_000
            connection.readTimeout = 5_000
            connection.instanceFollowRedirects = true
            connection.inputStream.use { input ->
                val source = BitmapFactory.decodeStream(input) ?: return null
                val padded = source.toNotificationArtwork()
                ByteArrayOutputStream().use { output ->
                    padded.compress(Bitmap.CompressFormat.PNG, 100, output)
                    output.toByteArray()
                }
            }
        }.getOrNull().also {
            connection.disconnect()
        }
    }

    private fun Bitmap.toNotificationArtwork(): Bitmap {
        val canvasSize = max(width, height).coerceAtLeast(256)
        val target = Bitmap.createBitmap(canvasSize, canvasSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(target)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

        val safeSize = canvasSize * 0.76f
        val scale = min(safeSize / width.toFloat(), safeSize / height.toFloat())
        val scaledWidth = (width * scale).roundToInt()
        val scaledHeight = (height * scale).roundToInt()
        val left = ((canvasSize - scaledWidth) / 2f)
        val top = ((canvasSize - scaledHeight) / 2f)
        val destination = android.graphics.RectF(
            left,
            top,
            left + scaledWidth,
            top + scaledHeight,
        )
        canvas.drawBitmap(this, null, destination, paint)
        return target
    }

    private companion object {
        const val TAG = "RadikallPlayer"
        const val LIVE_WINDOW_RECOVERY_INTERVAL_MS = 10_000L
    }
}

/**
 * 默认策略断网 3 次（几秒）就判死刑，播放随之停下、服务退出前台。这里放宽到约 3 分钟，
 * 期间播放器一直处于缓冲状态，通知和前台服务都还在，网络一回来就接着放。
 * 次数要留足余量：失败次数在断网期间一直累加，网络刚恢复时（VPN 重连等）常再断一两下，
 * 实机上断网 40 秒、额度 50 秒时，恢复后的第一下抖动就把播放判死了。
 *
 * 但这份宽容只给「连不上」这类错误。服务器已经回了错误码就不一样了：
 * - 401/403 说明令牌失效，重试没用，立刻报错交给 ViewModel 换令牌；
 * - 其它（典型是 404：断网一分钟后，直播窗口已经往前走，手里那段音频在服务器上被删了）
 *   最多重试 [MAX_HTTP_STATUS_RETRIES] 次，然后交给 ViewModel 重新拉流。
 *   实机上曾经对着同一个 404 重试满 3 分钟，网络恢复后过了 3 分钟才出声。
 */
private class LiveStreamLoadErrorPolicy : DefaultLoadErrorHandlingPolicy(MAX_RETRIES) {
    override fun getRetryDelayMsFor(loadErrorInfo: LoadErrorHandlingPolicy.LoadErrorInfo): Long {
        val error = loadErrorInfo.exception
        if (error is HttpDataSource.InvalidResponseCodeException) {
            if (error.responseCode == 401 || error.responseCode == 403) {
                return C.TIME_UNSET
            }
            if (loadErrorInfo.errorCount > MAX_HTTP_STATUS_RETRIES) {
                return C.TIME_UNSET
            }
        }
        if (super.getRetryDelayMsFor(loadErrorInfo) == C.TIME_UNSET) {
            return C.TIME_UNSET
        }
        return min(loadErrorInfo.errorCount * 1_000L, 5_000L)
    }

    private companion object {
        const val MAX_RETRIES = 40
        const val MAX_HTTP_STATUS_RETRIES = 3
    }
}
