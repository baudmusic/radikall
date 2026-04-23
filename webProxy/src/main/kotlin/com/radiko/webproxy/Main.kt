package com.radiko.webproxy

import com.radiko.auth.RadikoAuth
import com.radiko.auth.TokenManager
import com.radiko.program.ProgramRepository
import com.radiko.station.PrefectureData
import com.radiko.station.ProgramEntry
import com.radiko.station.StationRegistry
import com.radiko.stream.StreamUrlResolver
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO as ClientCIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.cio.CIO as ServerCIO
import io.ktor.server.engine.embeddedServer
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation as ServerContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.path
import io.ktor.server.request.receive
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.response.respondBytes
import io.ktor.server.response.respondFile
import io.ktor.server.response.respondOutputStream
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.serialization.kotlinx.json.json
import java.io.File
import java.io.IOException
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.json.Json

private const val DEFAULT_AREA_ID = "JP13"
private val supportedLanguages = listOf("zh-CN", "zh-TW", "en", "ja", "ko")
private val allowedUpstreamHostSuffixes = listOf("radiko.jp", "smartstream.ne.jp")

fun main() {
    val port = System.getenv("RADIKALL_WEB_PROXY_PORT")?.toIntOrNull() ?: 8787
    embeddedServer(ServerCIO, port = port, host = "0.0.0.0") {
        module()
    }.start(wait = true)
}

fun Application.module() {
    val json = Json {
        prettyPrint = false
        explicitNulls = false
        ignoreUnknownKeys = true
        isLenient = true
    }
    val httpClient = createHttpClient(json)
    val clock = Clock.System
    val tokenManager = TokenManager(RadikoAuth(httpClient, clock), clock)
    val programRepository = ProgramRepository(httpClient, clock)
    val streamUrlResolver = StreamUrlResolver(httpClient)
    val liveSessionStore = LivePlaybackSessionStore(clock)
    val distDir = resolveWebDistDir()

    install(ServerContentNegotiation) {
        json(json)
    }
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            call.respond(
                HttpStatusCode.InternalServerError,
                ErrorResponse(cause.message ?: "Unexpected server error"),
            )
        }
    }

    routing {
        get("/health") {
            call.respond(mapOf("status" to "ok"))
        }

        route("/api") {
            get("/bootstrap") {
                val bootstrap = BootstrapResponse(
                    defaultAreaId = DEFAULT_AREA_ID,
                    supportedLanguages = supportedLanguages,
                    featureFlags = FeatureFlagsResponse(
                        autoPlayOnLaunch = false,
                        backgroundPlaybackBestEffort = true,
                        alarms = false,
                        pushReminder = false,
                        sleepTimer = false,
                        wifiOnlyPlayback = false,
                        confirmMobileDataPlayback = false,
                    ),
                    regions = PrefectureData.regions.map { it.toSummary() },
                    prefectures = PrefectureData.allPrefectures.mapNotNull { prefecture ->
                        val region = PrefectureData.regionForArea(prefecture.id) ?: return@mapNotNull null
                        val areaCode = PrefectureData.areaCodeMap[prefecture.id].orEmpty()
                        prefecture.toSummary(region.id, areaCode)
                    },
                    stations = StationRegistry.allStations.toStationSummaries(),
                )
                call.respond(bootstrap)
            }

            get("/stations") {
                val areaId = call.request.queryParameters["areaId"]?.ifBlank { null }
                val query = call.request.queryParameters["query"].orEmpty().trim()
                val stations = when {
                    !query.isBlank() && areaId != null -> StationRegistry.search(query = query, areaId = areaId)
                    !query.isBlank() -> StationRegistry.search(query = query)
                    areaId != null -> StationRegistry.getStationsForArea(areaId)
                    else -> StationRegistry.allStations
                }
                call.respond(stations.toStationSummaries())
            }

            get("/stations/{stationId}/now-playing") {
                val stationId = call.parameters["stationId"] ?: return@get call.respond(
                    HttpStatusCode.BadRequest,
                    ErrorResponse("Missing stationId"),
                )

                val currentProgram = programRepository.currentProgram(stationId)
                val recentSongs = programRepository.fetchOnAirSongs(stationId, size = 50)
                val response = NowPlayingResponse(
                    stationId = stationId,
                    currentProgram = currentProgram,
                    currentSong = recentSongs.firstOrNull(),
                    currentProgramSongs = filterSongsForProgram(recentSongs, currentProgram),
                    recentSongs = recentSongs,
                )
                call.respond(response)
            }

            get("/stations/{stationId}/schedule") {
                val stationId = call.parameters["stationId"] ?: return@get call.respond(
                    HttpStatusCode.BadRequest,
                    ErrorResponse("Missing stationId"),
                )
                val weeklyPrograms = filterProgramsFromToday(programRepository.fetchWeeklyPrograms(stationId), clock)
                    .map(::compactScheduleProgram)
                val todayPrograms = weeklyPrograms.filter { it.startAt.startsWith(currentTokyoDate(clock)) }
                call.respond(
                    ScheduleResponse(
                        stationId = stationId,
                        todayPrograms = todayPrograms,
                        weeklyPrograms = weeklyPrograms,
                    ),
                )
            }

            post("/playback/live-session") {
                val request = call.receive<LiveSessionRequest>()
                val station = StationRegistry.getStation(request.stationId) ?: return@post call.respond(
                    HttpStatusCode.NotFound,
                    ErrorResponse("Unknown station: ${request.stationId}"),
                )
                val authSession = tokenManager.getSessionForStation(station, request.preferredAreaId)
                val upstreamMasterUrl = streamUrlResolver.buildLiveStreamUrl(station.id)
                val currentProgram = programRepository.currentProgram(station.id)
                val liveSession = liveSessionStore.create(
                    stationId = station.id,
                    resolvedAreaId = authSession.areaId,
                    authToken = authSession.token,
                    userAgent = authSession.deviceInfo.userAgent,
                    upstreamMasterUrl = upstreamMasterUrl,
                )

                call.respond(
                    LiveSessionResponse(
                        sessionId = liveSession.id,
                        stationId = station.id,
                        resolvedAreaId = authSession.areaId,
                        streamUrl = "/api/playback/live/${liveSession.id}/master.m3u8",
                        fallbackStreamUrl = "/api/playback/live/${liveSession.id}/audio.mp3",
                        currentProgram = currentProgram,
                        expiresAtEpochMillis = liveSession.expiresAt.toEpochMilliseconds(),
                    ),
                )
            }

            get("/playback/live/{sessionId}/audio.mp3") {
                val session = resolveLiveSession(call.parameters["sessionId"], liveSessionStore)
                    ?: return@get call.respond(
                        HttpStatusCode.Gone,
                        ErrorResponse("Playback session expired"),
                    )

                streamTranscodedAudio(call, session)
            }

            get("/playback/live/{sessionId}/master.m3u8") {
                val session = resolveLiveSession(call.parameters["sessionId"], liveSessionStore)
                    ?: return@get call.respond(
                        HttpStatusCode.Gone,
                        ErrorResponse("Playback session expired"),
                    )

                val upstreamResponse = fetchUpstream(
                    httpClient = httpClient,
                    targetUrl = session.upstreamMasterUrl,
                    session = session,
                )
                proxyResponse(call, upstreamResponse, session, session.upstreamMasterUrl)
            }

            get("/playback/live/{sessionId}/proxy") {
                val session = resolveLiveSession(call.parameters["sessionId"], liveSessionStore)
                    ?: return@get call.respond(
                        HttpStatusCode.Gone,
                        ErrorResponse("Playback session expired"),
                    )

                val targetUrl = call.request.queryParameters["target"] ?: return@get call.respond(
                    HttpStatusCode.BadRequest,
                    ErrorResponse("Missing target"),
                )
                if (!isAllowedUpstreamUrl(targetUrl)) {
                    return@get call.respond(
                        HttpStatusCode.BadRequest,
                        ErrorResponse("Unsupported upstream host"),
                    )
                }

                val upstreamResponse = fetchUpstream(
                    httpClient = httpClient,
                    targetUrl = targetUrl,
                    session = session,
                )
                proxyResponse(call, upstreamResponse, session, targetUrl)
            }
        }

        get("/") {
            serveStaticAsset(call.request.path(), distDir)?.let { file ->
                call.respondFile(file)
            } ?: call.respond(
                HttpStatusCode.NotFound,
                ErrorResponse("webApp build not found. Run the Vite dev server or build webApp/dist."),
            )
        }

        get("/{...}") {
            val requestPath = call.request.path()
            if (requestPath.startsWith("/api/") || requestPath == "/health") {
                return@get call.respond(HttpStatusCode.NotFound)
            }

            serveStaticAsset(requestPath, distDir)?.let { file ->
                call.respondFile(file)
            } ?: call.respond(
                HttpStatusCode.NotFound,
                ErrorResponse("webApp build not found. Run the Vite dev server or build webApp/dist."),
            )
        }
    }
}

private fun createHttpClient(json: Json): HttpClient = HttpClient(ClientCIO) {
    followRedirects = true
    install(ContentNegotiation) {
        json(json)
    }
    install(Logging) {
        level = LogLevel.NONE
    }
}

private suspend fun proxyResponse(
    call: ApplicationCall,
    upstreamResponse: HttpResponse,
    session: LivePlaybackSession,
    currentTargetUrl: String,
) {
    val parsedContentType = upstreamResponse.headers[HttpHeaders.ContentType]?.let(ContentType::parse)
    val contentType = parsedContentType?.toString().orEmpty()
    val cacheControl = "no-store, no-cache, max-age=0"
    call.response.status(upstreamResponse.status)
    call.response.header(HttpHeaders.CacheControl, cacheControl)
    upstreamResponse.headers[HttpHeaders.ContentRange]?.let {
        call.response.header(HttpHeaders.ContentRange, it)
    }
    upstreamResponse.headers[HttpHeaders.AcceptRanges]?.let {
        call.response.header(HttpHeaders.AcceptRanges, it)
    }

    if (isPlaylistResponse(currentTargetUrl, contentType)) {
        val playlist = upstreamResponse.bodyAsText()
        val rewritten = rewritePlaylist(session.id, currentTargetUrl, playlist)
        call.respondText(
            text = rewritten,
            contentType = ContentType.parse(if (contentType.isBlank()) "application/vnd.apple.mpegurl" else contentType),
            status = upstreamResponse.status,
        )
        return
    }

    call.respondBytes(
        bytes = upstreamResponse.body<ByteArray>(),
        contentType = parsedContentType,
        status = upstreamResponse.status,
    )
}

private suspend fun streamTranscodedAudio(
    call: ApplicationCall,
    session: LivePlaybackSession,
) {
    val ffmpeg = startFfmpegTranscoder(
        proxyMasterUrl = "http://127.0.0.1:${currentProxyPort()}/api/playback/live/${session.id}/master.m3u8",
    )
    try {
        call.response.header(HttpHeaders.CacheControl, "no-store, no-cache, max-age=0")
        call.respondOutputStream(
            contentType = ContentType.Audio.MPEG,
            status = HttpStatusCode.OK,
        ) {
            ffmpeg.inputStream.use { input ->
                input.copyTo(this)
            }
        }
    } finally {
        terminateProcess(ffmpeg)
    }
}

private suspend fun fetchUpstream(
    httpClient: HttpClient,
    targetUrl: String,
    session: LivePlaybackSession,
): HttpResponse = httpClient.get(targetUrl) {
    header("X-Radiko-AuthToken", session.authToken)
    header("X-Radiko-AreaId", session.resolvedAreaId)
    header(HttpHeaders.UserAgent, session.userAgent)
}

private fun startFfmpegTranscoder(proxyMasterUrl: String): Process {
    val command = listOf(
        "ffmpeg",
        "-hide_banner",
        "-loglevel",
        "error",
        "-nostats",
        "-fflags",
        "nobuffer",
        "-flags",
        "low_delay",
        "-probesize",
        "32768",
        "-analyzeduration",
        "0",
        "-i",
        proxyMasterUrl,
        "-vn",
        "-ac",
        "2",
        "-ar",
        "44100",
        "-c:a",
        "libmp3lame",
        "-b:a",
        "128k",
        "-flush_packets",
        "1",
        "-f",
        "mp3",
        "pipe:1",
    )

    return try {
        ProcessBuilder(command)
            .redirectError(ProcessBuilder.Redirect.DISCARD)
            .start()
    } catch (exception: IOException) {
        throw IllegalStateException(
            "ffmpeg is required for browser playback but was not found on PATH.",
            exception,
        )
    }
}

private fun resolveLiveSession(
    sessionId: String?,
    store: LivePlaybackSessionStore,
): LivePlaybackSession? = sessionId?.let(store::get)

private fun currentProxyPort(): Int =
    System.getenv("RADIKALL_WEB_PROXY_PORT")?.toIntOrNull() ?: 8787

private fun terminateProcess(process: Process) {
    if (!process.isAlive) {
        return
    }
    process.destroy()
    if (process.isAlive) {
        process.destroyForcibly()
    }
}

private fun rewritePlaylist(
    sessionId: String,
    currentTargetUrl: String,
    playlist: String,
): String {
    val baseUri = URI(currentTargetUrl)
    return playlist.lineSequence()
        .joinToString(separator = "\n") { line ->
            val trimmed = line.trim()
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                line
            } else {
                val resolved = baseUri.resolve(trimmed).toString()
                "/api/playback/live/$sessionId/proxy?target=${resolved.urlEncode()}"
            }
        }
}

private fun isAllowedUpstreamUrl(url: String): Boolean = runCatching {
    val uri = URI(url)
    uri.scheme == "https" && allowedUpstreamHostSuffixes.any { suffix ->
        uri.host == suffix || uri.host.endsWith(".$suffix")
    }
}.getOrDefault(false)

private fun isPlaylistResponse(targetUrl: String, contentType: String): Boolean {
    val normalizedType = contentType.lowercase()
    return targetUrl.substringBefore('?').endsWith(".m3u8") ||
        normalizedType.contains("mpegurl") ||
        normalizedType.contains("vnd.apple.mpegurl")
}

private fun filterSongsForProgram(
    songs: List<com.radiko.station.OnAirSong>,
    currentProgram: ProgramEntry?,
): List<com.radiko.station.OnAirSong> {
    if (currentProgram == null) {
        return songs
    }
    return songs.filter { song ->
        val compactStamp = song.stampDate.filter(Char::isDigit).take(14)
        compactStamp >= currentProgram.startAt && compactStamp < currentProgram.endAt
    }
}

private fun filterProgramsFromToday(
    programs: List<ProgramEntry>,
    clock: Clock,
): List<ProgramEntry> {
    val today = currentTokyoDate(clock)
    return programs.filter { it.startAt.take(8) >= today }
}

private fun compactScheduleProgram(program: ProgramEntry): ProgramEntry = program.copy(
    description = "",
    info = null,
    imageUrl = null,
    url = null,
)

private fun currentTokyoDate(clock: Clock): String {
    val dateTime = clock.now().toLocalDateTime(TimeZone.of("Asia/Tokyo"))
    return buildString {
        append(dateTime.year.toString().padStart(4, '0'))
        append(dateTime.monthNumber.toString().padStart(2, '0'))
        append(dateTime.dayOfMonth.toString().padStart(2, '0'))
    }
}

private fun resolveWebDistDir(): File {
    val configured = System.getenv("RADIKALL_WEB_DIST")?.takeIf { it.isNotBlank() }
    if (configured != null) {
        return File(configured)
    }
    return File("webApp/dist")
}

private fun serveStaticAsset(requestPath: String, distDir: File): File? {
    if (!distDir.exists() || !distDir.isDirectory) {
        return null
    }
    val normalizedPath = requestPath.removePrefix("/").ifBlank { "index.html" }
    val candidate = File(distDir, normalizedPath).canonicalFile
    val canonicalDist = distDir.canonicalFile
    if (candidate.exists() && candidate.isFile && candidate.path.startsWith(canonicalDist.path)) {
        return candidate
    }

    val fallback = File(distDir, "index.html")
    return fallback.takeIf { it.exists() && it.isFile }
}

private fun String.urlEncode(): String = URLEncoder.encode(this, StandardCharsets.UTF_8)
