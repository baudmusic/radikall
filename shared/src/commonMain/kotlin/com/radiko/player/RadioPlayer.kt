package com.radiko.player

import com.radiko.station.Station
import com.radiko.station.ProgramEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow

data class StreamPlaybackRequest(
    val station: Station,
    val streamUrl: String,
    val headers: Map<String, String>,
)

interface RadioPlayer {
    val isPlaying: StateFlow<Boolean>

    /**
     * 播放器自己放弃了（不是因为调用了 [stop]）：网络断得太久、令牌过期之类。
     * 只在本该出声的时候发（用户暂停期间出的错不算），由 ViewModel 换新令牌重连。
     * 没实现的平台永远不发。
     */
    val playbackFailures: Flow<Unit> get() = emptyFlow()

    suspend fun play(request: StreamPlaybackRequest)
    fun updateMetadata(station: Station, program: ProgramEntry?)

    fun stop()
}
