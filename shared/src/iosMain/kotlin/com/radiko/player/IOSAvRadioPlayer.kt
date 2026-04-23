@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.radiko.player

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import platform.AVFoundation.AVPlayer
import platform.AVFoundation.AVPlayerItem
import platform.AVFoundation.AVURLAsset
import platform.Foundation.NSURL

class IOSAvRadioPlayer : RadioPlayer {
    private val playbackState = MutableStateFlow(false)
    override val isPlaying: StateFlow<Boolean> = playbackState

    private var player: AVPlayer? = null

    override suspend fun play(request: StreamPlaybackRequest) {
        stop()

        val url = NSURL(string = request.streamUrl)
            ?: error("Invalid stream URL: ${request.streamUrl}")
        val assetOptions = request.headers.takeIf { it.isNotEmpty() }?.let { headers ->
            mapOf<Any?, Any>("AVURLAssetHTTPHeaderFieldsKey" to headers)
        }
        val asset = if (assetOptions == null) {
            AVURLAsset(uRL = url)
        } else {
            AVURLAsset(uRL = url, options = assetOptions)
        }
        val item = AVPlayerItem(asset = asset)
        val avPlayer = AVPlayer(playerItem = item)
        avPlayer.automaticallyWaitsToMinimizeStalling = true
        avPlayer.play()

        player = avPlayer
        playbackState.value = true
    }

    override fun updateMetadata(station: com.radiko.station.Station, program: com.radiko.station.ProgramEntry?) {
        // Lock screen metadata and remote controls can be layered on once device signing is available.
    }

    override fun stop() {
        player?.pause()
        player?.replaceCurrentItemWithPlayerItem(null)
        player = null
        playbackState.value = false
    }
}
