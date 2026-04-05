package com.radiko.auth

import com.radiko.device.DeviceInfo
import com.radiko.device.DeviceSpoofing
import com.radiko.device.GpsSpoofing
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

data class AuthSession(
    val token: String,
    val areaId: String,
    val deviceInfo: DeviceInfo,
    val requestedAt: Instant,
)

class RadikoAuth(
    private val httpClient: HttpClient,
    private val clock: Clock = Clock.System,
) {
    suspend fun authenticate(areaId: String): AuthSession {
        val deviceInfo = DeviceSpoofing.generate()
        val appName = DeviceSpoofing.appNameForVersion(deviceInfo.appVersion)

        val auth1Response = httpClient.get("https://radiko.jp/v2/api/auth1") {
            header("X-Radiko-App", appName)
            header("X-Radiko-App-Version", deviceInfo.appVersion)
            header("X-Radiko-Device", deviceInfo.device)
            header("X-Radiko-User", deviceInfo.userId)
            header(HttpHeaders.UserAgent, deviceInfo.userAgent)
        }

        check(auth1Response.status.isSuccess()) {
            "Auth1 failed with HTTP ${auth1Response.status.value}"
        }

        val token = auth1Response.headers["x-radiko-authtoken"]
            ?: error("Auth1 succeeded but did not return x-radiko-authtoken")
        val keyOffset = auth1Response.headers["x-radiko-keyoffset"]?.toInt()
            ?: error("Auth1 succeeded but did not return x-radiko-keyoffset")
        val keyLength = auth1Response.headers["x-radiko-keylength"]?.toInt()
            ?: error("Auth1 succeeded but did not return x-radiko-keylength")
        val partialKey = PartialKeyGenerator.generate(offset = keyOffset, length = keyLength)

        val auth2Response = httpClient.get("https://radiko.jp/v2/api/auth2") {
            header("X-Radiko-App", appName)
            header("X-Radiko-App-Version", deviceInfo.appVersion)
            header("X-Radiko-Device", deviceInfo.device)
            header("X-Radiko-User", deviceInfo.userId)
            header("X-Radiko-AuthToken", token)
            header("X-Radiko-Partialkey", partialKey)
            header("X-Radiko-Location", GpsSpoofing.generate(areaId))
            header("X-Radiko-Connection", "wifi")
            header(HttpHeaders.UserAgent, deviceInfo.userAgent)
        }

        check(auth2Response.status.isSuccess()) {
            "Auth2 failed with HTTP ${auth2Response.status.value}: ${auth2Response.bodyAsText()}"
        }

        val resolvedArea = Regex("""JP\d{1,2}""")
            .find(auth2Response.bodyAsText())
            ?.value
            ?: areaId

        return AuthSession(
            token = token,
            areaId = resolvedArea,
            deviceInfo = deviceInfo,
            requestedAt = clock.now(),
        )
    }

    suspend fun authCheck(token: String): Boolean {
        val response = httpClient.get("https://radiko.jp/v2/api/auth_check") {
            header("X-Radiko-AuthToken", token)
        }
        return response.status.isSuccess() && response.bodyAsText().contains("OK")
    }
}

