package com.radiko.player

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.net.Uri
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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

        val player = ExoPlayer.Builder(context).build().also { exoPlayer = it }
        applyAudioFocusBehavior(player)
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                playbackState.value = isPlaying
            }
        })

        val session = MediaSession.Builder(context, player).build().also { mediaSession = it }
        AndroidAudioService.registerSession(session)
        return player
    }

    private fun ensureServiceRunning() {
        if (AndroidAudioService.instance != null) {
            return
        }
        val intent = Intent(context, AndroidAudioService::class.java)
        context.startForegroundService(intent)
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
}
