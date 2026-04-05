package com.radiko.platform

import com.radiko.player.DesktopRadikoPlayer
import com.radiko.player.RadioPlayer
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

actual object PlatformEnvironment {
    actual val httpClient: HttpClient by lazy {
        HttpClient(CIO) {
            install(ContentNegotiation) {
                json(
                    Json {
                        ignoreUnknownKeys = true
                        isLenient = true
                    }
                )
            }
            install(Logging) {
                level = LogLevel.NONE
            }
        }
    }

    actual val platformType: PlatformType = PlatformType.DESKTOP
    actual val supportsPointerHover: Boolean = true

    actual fun createPlayer(): RadioPlayer = DesktopRadikoPlayer(httpClient)
}
