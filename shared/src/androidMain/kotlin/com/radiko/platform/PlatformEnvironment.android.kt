package com.radiko.platform

import com.radiko.player.AndroidExoRadioPlayer
import com.radiko.player.RadioPlayer
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

actual object PlatformEnvironment {
    actual val httpClient: HttpClient by lazy {
        HttpClient(OkHttp) {
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

    actual val platformType: PlatformType = PlatformType.ANDROID
    actual val supportsPointerHover: Boolean = false

    actual fun createPlayer(): RadioPlayer = AndroidExoRadioPlayer(
        context = AndroidPlatformContext.requireContext(),
    )
}
