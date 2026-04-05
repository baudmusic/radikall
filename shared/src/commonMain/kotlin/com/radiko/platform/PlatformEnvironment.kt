package com.radiko.platform

import com.radiko.player.RadioPlayer
import io.ktor.client.HttpClient

enum class PlatformType {
    ANDROID,
    DESKTOP,
}

expect object PlatformEnvironment {
    val httpClient: HttpClient
    val platformType: PlatformType
    val supportsPointerHover: Boolean

    fun createPlayer(): RadioPlayer
}
