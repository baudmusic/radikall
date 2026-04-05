package com.radiko.stream

import com.radiko.auth.AuthSession
import com.radiko.device.DeviceSpoofing
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.URLBuilder
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Duration.Companion.seconds

class StreamUrlResolver(
    private val httpClient: HttpClient,
) {
    private val liveFallbackUrl = "https://si-f-radiko.smartstream.ne.jp/so/playlist.m3u8"
    private val timefreeFallbackUrl = "https://tf-f-rpaa-radiko.smartstream.ne.jp/tf/playlist.m3u8"

    suspend fun getLivePlaylistUrl(stationId: String): String =
        extractPlaylistUrl(
            stationId = stationId,
            areaFree = false,
            timeFree = false,
            fallbackUrl = liveFallbackUrl,
        )

    suspend fun getTimefreePlaylistUrl(stationId: String): String =
        extractPlaylistUrl(
            stationId = stationId,
            areaFree = false,
            timeFree = true,
            fallbackUrl = timefreeFallbackUrl,
        )

    suspend fun buildLiveStreamUrl(
        stationId: String,
        connectionType: String = "b",
    ): String {
        val baseUrl = getLivePlaylistUrl(stationId)
        return URLBuilder(baseUrl).apply {
            parameters.append("station_id", stationId)
            parameters.append("lsid", DeviceSpoofing.randomHex(32))
            parameters.append("type", connectionType)
            parameters.append("l", "15")
        }.buildString()
    }

    suspend fun resolveTimefreeSegments(
        stationId: String,
        startAt: String,
        endAt: String,
        authSession: AuthSession,
    ): List<String> {
        val baseUrl = getTimefreePlaylistUrl(stationId)
        val collected = linkedSetOf<String>()
        var seekAt = startAt

        while (seekAt < endAt) {
            val playlistUrl = URLBuilder(baseUrl).apply {
                parameters.append("lsid", DeviceSpoofing.randomHex(32))
                parameters.append("station_id", stationId)
                parameters.append("l", "300")
                parameters.append("start_at", startAt)
                parameters.append("end_at", endAt)
                parameters.append("type", "b")
                parameters.append("ft", startAt)
                parameters.append("to", endAt)
                parameters.append("seek", seekAt)
            }.buildString()

            val chunkListResponse = httpClient.get(playlistUrl) {
                header("X-Radiko-AreaId", authSession.areaId)
                header("X-Radiko-AuthToken", authSession.token)
            }.bodyAsText()

            val detailUrl = extractM3uEntries(chunkListResponse).firstOrNull() ?: break
            val segmentList = httpClient.get(detailUrl).bodyAsText()
            collected += extractM3uEntries(segmentList)

            seekAt = incrementTimestamp(seekAt, 300)
        }

        return collected.toList()
    }

    private suspend fun extractPlaylistUrl(
        stationId: String,
        areaFree: Boolean,
        timeFree: Boolean,
        fallbackUrl: String,
    ): String {
        val xml = httpClient
            .get("https://radiko.jp/v3/station/stream/pc_html5/$stationId.xml")
            .bodyAsText()

        val regex = Regex(
            """<url[^>]*areafree="${if (areaFree) 1 else 0}"[^>]*timefree="${if (timeFree) 1 else 0}"[^>]*>.*?<playlist_create_url>(.*?)</playlist_create_url>""",
            setOf(RegexOption.DOT_MATCHES_ALL),
        )
        return regex.find(xml)?.groupValues?.get(1)?.trim() ?: fallbackUrl
    }

    private fun extractM3uEntries(playlistText: String): List<String> = playlistText
        .lineSequence()
        .map { it.trim() }
        .filter { it.isNotEmpty() && !it.startsWith("#") }
        .toList()

    private fun incrementTimestamp(timestamp: String, seconds: Int): String {
        val dateTime = LocalDateTime(
            year = timestamp.substring(0, 4).toInt(),
            monthNumber = timestamp.substring(4, 6).toInt(),
            dayOfMonth = timestamp.substring(6, 8).toInt(),
            hour = timestamp.substring(8, 10).toInt(),
            minute = timestamp.substring(10, 12).toInt(),
            second = timestamp.substring(12, 14).toInt(),
        )

        val shifted = dateTime
            .toInstant(TimeZone.of("Asia/Tokyo"))
            .plus(seconds.seconds)
            .toLocalDateTime(TimeZone.of("Asia/Tokyo"))

        return buildString {
            append(shifted.year.toString().padStart(4, '0'))
            append(shifted.monthNumber.toString().padStart(2, '0'))
            append(shifted.dayOfMonth.toString().padStart(2, '0'))
            append(shifted.hour.toString().padStart(2, '0'))
            append(shifted.minute.toString().padStart(2, '0'))
            append(shifted.second.toString().padStart(2, '0'))
        }
    }
}
