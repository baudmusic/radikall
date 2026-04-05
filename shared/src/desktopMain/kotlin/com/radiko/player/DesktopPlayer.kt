package com.radiko.player

import io.ktor.client.HttpClient
import com.radiko.station.ProgramEntry
import com.radiko.station.Station
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import uk.co.caprica.vlcj.factory.MediaPlayerFactory
import uk.co.caprica.vlcj.player.base.MediaPlayer
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter

class DesktopRadikoPlayer(
    httpClient: HttpClient,
) : RadioPlayer {
    private val playbackState = MutableStateFlow(false)
    override val isPlaying: StateFlow<Boolean> = playbackState

    private var mediaPlayerFactory: MediaPlayerFactory? = null
    private var mediaPlayer: MediaPlayer? = null
    private var activeHeaders: Map<String, String> = emptyMap()
    private val proxy = LocalHlsProxy(
        httpClient = httpClient,
        getHeaders = { activeHeaders },
    )

    override suspend fun play(request: StreamPlaybackRequest) {
        activeHeaders = request.headers
        proxy.start()

        // 释放旧实例
        mediaPlayer?.controls()?.stop()
        mediaPlayer?.release()
        mediaPlayerFactory?.release()

        val factory = MediaPlayerFactory().also { mediaPlayerFactory = it }
        val player = factory.mediaPlayers().newMediaPlayer().also { mediaPlayer = it }

        // 监听 VLC 真实播放状态，而非乐观地立即设为 true
        player.events().addMediaPlayerEventListener(object : MediaPlayerEventAdapter() {
            override fun playing(mediaPlayer: MediaPlayer) {
                playbackState.value = true
            }
            override fun paused(mediaPlayer: MediaPlayer) {
                playbackState.value = false
            }
            override fun stopped(mediaPlayer: MediaPlayer) {
                playbackState.value = false
            }
            override fun error(mediaPlayer: MediaPlayer) {
                playbackState.value = false
            }
        })

        player.media().play(proxy.proxiedUrl(request.streamUrl))
    }

    override fun updateMetadata(station: Station, program: ProgramEntry?) {
        // Desktop playback is driven by VLCJ only; metadata updates are currently UI-local.
    }

    override fun stop() {
        mediaPlayer?.controls()?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        mediaPlayerFactory?.release()
        mediaPlayerFactory = null
        proxy.stop()
        playbackState.value = false
    }
}
