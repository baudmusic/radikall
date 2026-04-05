package com.radiko.player

import com.radiko.station.Station
import com.radiko.station.ProgramEntry
import kotlinx.coroutines.flow.StateFlow

data class StreamPlaybackRequest(
    val station: Station,
    val streamUrl: String,
    val headers: Map<String, String>,
)

interface RadioPlayer {
    val isPlaying: StateFlow<Boolean>

    suspend fun play(request: StreamPlaybackRequest)
    fun updateMetadata(station: Station, program: ProgramEntry?)

    fun stop()
}
