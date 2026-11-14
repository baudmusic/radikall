package com.radiko.program

import com.radiko.station.OnAirSong
import com.radiko.station.ProgramEntry
import com.radiko.text.narrowAlphanumerics
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
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
        return parsePrograms(stationId, xml)
    }

    /**
     * radiko 按地区的 now 接口：一次拿到该地区所有电台的「当前 + 下一档」节目（约 60KB）。
     * 用 api.radiko.jp 而不是 radiko.jp：后者不管请求头都返回 gzip，桌面端的 CIO 引擎不会自动解压。
     */
    suspend fun fetchAreaNowPrograms(areaId: String): AreaNowPrograms {
        val xml = httpClient
            .get("https://api.radiko.jp/program/v3/now/$areaId.xml")
            .bodyAsText()
        return parseAreaNowPrograms(areaId, xml)
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

    private fun formatTokyoNow(): String = tokyoCompactTimestamp(clock.now())

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

/** 一个地区的 now 数据。 */
data class AreaNowPrograms(
    val areaId: String,
    /** radiko 建议的刷新间隔（<ttl>，秒），通常 300。 */
    val ttlSeconds: Int,
    /** 键是台号；每个电台按开始时间排好的「当前 + 下一档」节目。 */
    val programsByStation: Map<String, List<ProgramEntry>>,
)

private val programRegex = Regex("""<prog\b([^>]*)>([\s\S]*?)</prog>""")
private val stationBlockRegex = Regex("""<station id="([^"]+)">([\s\S]*?)</station>""")
private val attributeRegex = Regex("""(\w+)="([^"]*)"""")
private val xmlEntityRegex = Regex("""&(#x[0-9a-fA-F]+|#\d+|amp|lt|gt|quot|apos);""")

internal fun parseAreaNowPrograms(areaId: String, xml: String): AreaNowPrograms {
    val ttlSeconds = Regex("""<ttl>(\d+)</ttl>""").find(xml)?.groupValues?.get(1)?.toIntOrNull()
    val programsByStation = stationBlockRegex.findAll(xml).associate { match ->
        val stationId = match.groupValues[1]
        stationId to parsePrograms(stationId, match.groupValues[2]).sortedBy { it.startAt }
    }
    return AreaNowPrograms(
        areaId = areaId,
        ttlSeconds = ttlSeconds?.takeIf { it > 0 } ?: 300,
        programsByStation = programsByStation,
    )
}

internal fun parsePrograms(stationId: String, xml: String): List<ProgramEntry> =
    programRegex.findAll(xml).mapNotNull { match ->
        val attributes = attributeRegex.findAll(match.groupValues[1])
            .associate { it.groupValues[1] to it.groupValues[2] }
        val payload = match.groupValues[2]
        val startAt = attributes["ft"] ?: return@mapNotNull null
        val endAt = attributes["to"] ?: return@mapNotNull null

        ProgramEntry(
            stationId = stationId,
            title = decodeXmlText(extractTag(payload, "title").orEmpty()).narrowAlphanumerics(),
            description = extractTag(payload, "desc").orEmpty(),
            performer = extractTag(payload, "pfm")?.let { decodeXmlText(it).narrowAlphanumerics() },
            startAt = startAt,
            endAt = endAt,
            info = extractTag(payload, "info"),
            imageUrl = extractTag(payload, "img"),
            url = extractTag(payload, "url"),
        )
    }.toList()

private fun extractTag(payload: String, tag: String): String? {
    val regex = Regex("""<$tag>([\s\S]*?)</$tag>""")
    return regex.find(payload)?.groupValues?.get(1)?.trim()
}

/**
 * 标题和主持人是纯文本，但 radiko 会转义 & 和 '（如 `&amp;`、`&apos;`），这里还原。
 * 简介和 info 本身是转义过的 HTML，交给描述的显示逻辑处理，不在这里动。
 */
internal fun decodeXmlText(value: String): String = xmlEntityRegex.replace(value) { match ->
    val body = match.groupValues[1]
    when {
        body.startsWith("#x", ignoreCase = true) -> body.drop(2).toIntOrNull(16)?.let(::codePointToString)
        body.startsWith("#") -> body.drop(1).toIntOrNull()?.let(::codePointToString)
        else -> when (body) {
            "amp" -> "&"
            "lt" -> "<"
            "gt" -> ">"
            "quot" -> "\""
            "apos" -> "'"
            else -> null
        }
    } ?: match.value
}

private fun codePointToString(codePoint: Int): String? = when {
    codePoint < 0 || codePoint > 0x10FFFF -> null
    codePoint < 0x10000 -> codePoint.toChar().toString()
    else -> {
        val offset = codePoint - 0x10000
        charArrayOf((0xD800 + (offset shr 10)).toChar(), (0xDC00 + (offset and 0x3FF)).toChar()).concatToString()
    }
}

/** 东京时间的 yyyyMMddHHmmss，和 radiko 的 ft / to 同格式。 */
internal fun tokyoCompactTimestamp(instant: Instant): String {
    val dateTime = instant.toLocalDateTime(TimeZone.of("Asia/Tokyo"))
    return buildString {
        append(dateTime.year.toString().padStart(4, '0'))
        append(dateTime.monthNumber.toString().padStart(2, '0'))
        append(dateTime.dayOfMonth.toString().padStart(2, '0'))
        append(dateTime.hour.toString().padStart(2, '0'))
        append(dateTime.minute.toString().padStart(2, '0'))
        append(dateTime.second.toString().padStart(2, '0'))
    }
}

/** [tokyoCompactTimestamp] 的反向；格式不对时返回 null。 */
internal fun parseTokyoCompactTimestamp(value: String): Instant? {
    if (value.length != 14 || !value.all(Char::isDigit)) return null
    return runCatching {
        LocalDateTime(
            year = value.substring(0, 4).toInt(),
            monthNumber = value.substring(4, 6).toInt(),
            dayOfMonth = value.substring(6, 8).toInt(),
            hour = value.substring(8, 10).toInt(),
            minute = value.substring(10, 12).toInt(),
            second = value.substring(12, 14).toInt(),
        ).toInstant(TimeZone.of("Asia/Tokyo"))
    }.getOrNull()
}
