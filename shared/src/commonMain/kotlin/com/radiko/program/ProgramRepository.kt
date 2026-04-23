package com.radiko.program

import com.radiko.station.OnAirSong
import com.radiko.station.ProgramEntry
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class ProgramRepository(
    private val httpClient: HttpClient,
    private val clock: Clock = Clock.System,
) {
    private val jsonParser = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    suspend fun fetchWeeklyPrograms(stationId: String): List<ProgramEntry> {
        val xml = httpClient
            .get("https://api.radiko.jp/program/v3/weekly/$stationId.xml")
            .bodyAsText()

        val programRegex = Regex("""<prog\b([^>]*)>([\s\S]*?)</prog>""")
        return programRegex.findAll(xml).mapNotNull { match ->
            val attributes = parseAttributes(match.groupValues[1])
            val payload = match.groupValues[2]
            val startAt = attributes["ft"] ?: return@mapNotNull null
            val endAt = attributes["to"] ?: return@mapNotNull null

            ProgramEntry(
                stationId = stationId,
                title = extractTag(payload, "title").orEmpty(),
                description = extractTag(payload, "desc").orEmpty(),
                performer = extractTag(payload, "pfm"),
                startAt = startAt,
                endAt = endAt,
                info = extractTag(payload, "info"),
                imageUrl = extractTag(payload, "img"),
                url = extractTag(payload, "url"),
            )
        }.toList()
    }

    suspend fun fetchOnAirSongs(stationId: String, size: Int = 20): List<OnAirSong> {
        return runCatching {
            val json = httpClient
                .get("https://api.radiko.jp/music/api/v1/noas/$stationId/latest") {
                    parameter("size", size)
                    parameter("_", clock.now().toEpochMilliseconds())
                    header(HttpHeaders.CacheControl, "no-cache, no-store, max-age=0")
                    header(HttpHeaders.Pragma, "no-cache")
                }
                .bodyAsText()

            parseOnAirSongs(json)
        }.getOrDefault(emptyList())
    }

    suspend fun fetchTodayPrograms(stationId: String): List<ProgramEntry> {
        val todayDate = formatTokyoDate()
        return fetchWeeklyPrograms(stationId).filter { program ->
            program.startAt.startsWith(todayDate)
        }
    }

    suspend fun currentProgram(stationId: String): ProgramEntry? {
        val currentTime = formatTokyoNow()
        return fetchWeeklyPrograms(stationId)
            .firstOrNull { currentTime >= it.startAt && currentTime < it.endAt }
    }

    private fun parseOnAirSongs(json: String): List<OnAirSong> {
        val data = jsonParser
            .parseToJsonElement(json)
            .jsonObject["data"]
            ?.jsonArray
            ?: return emptyList()

        return data.mapNotNull { itemElement ->
            val item = itemElement.jsonObject
            val title = item["title"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
            val artist = item["artist_name"]?.jsonPrimitive?.contentOrNull
                ?: item["artist"]?.jsonObject?.get("name")?.jsonPrimitive?.contentOrNull
                ?: return@mapNotNull null
            val imageUrl = item["music"]
                ?.jsonObject
                ?.get("image")
                ?.jsonObject
                ?.let { image ->
                    image["large"]?.jsonPrimitive?.contentOrNull
                        ?: image["medium"]?.jsonPrimitive?.contentOrNull
                        ?: image["small"]?.jsonPrimitive?.contentOrNull
                }
            val stampDate = item["displayed_start_time"]?.jsonPrimitive?.contentOrNull
                ?: return@mapNotNull null

            OnAirSong(
                title = title,
                artist = artist,
                imageUrl = imageUrl,
                stampDate = stampDate,
            )
        }.sortedByDescending { it.sortKey() }
    }

    private fun parseAttributes(attributes: String): Map<String, String> {
        val regex = Regex("""(\w+)="([^"]*)"""")
        return regex.findAll(attributes).associate { it.groupValues[1] to it.groupValues[2] }
    }

    private fun extractTag(payload: String, tag: String): String? {
        val regex = Regex("""<$tag>([\s\S]*?)</$tag>""")
        return regex.find(payload)?.groupValues?.get(1)?.trim()
    }

    private fun formatTokyoNow(): String {
        val dateTime = clock.now().toLocalDateTime(TimeZone.of("Asia/Tokyo"))
        return buildString {
            append(dateTime.year.toString().padStart(4, '0'))
            append(dateTime.monthNumber.toString().padStart(2, '0'))
            append(dateTime.dayOfMonth.toString().padStart(2, '0'))
            append(dateTime.hour.toString().padStart(2, '0'))
            append(dateTime.minute.toString().padStart(2, '0'))
            append(dateTime.second.toString().padStart(2, '0'))
        }
    }

    private fun formatTokyoDate(): String {
        val dateTime = clock.now().toLocalDateTime(TimeZone.of("Asia/Tokyo"))
        return buildString {
            append(dateTime.year.toString().padStart(4, '0'))
            append(dateTime.monthNumber.toString().padStart(2, '0'))
            append(dateTime.dayOfMonth.toString().padStart(2, '0'))
        }
    }

    private fun OnAirSong.sortKey(): String = stampDate.filter(Char::isDigit).take(14).padEnd(14, '0')
}
