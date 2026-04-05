package com.radiko.player

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.server.cio.CIO
import io.ktor.server.cio.CIOApplicationEngine
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.response.respondBytes
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import java.net.URI
import java.net.ServerSocket
import java.net.URLDecoder
import java.net.URLEncoder

class LocalHlsProxy(
    private val httpClient: HttpClient,
    private val getHeaders: () -> Map<String, String>,
) {
    private var server: EmbeddedServer<CIOApplicationEngine, CIOApplicationEngine.Configuration>? = null
    var port: Int = 0
        private set

    fun start() {
        if (server != null) {
            return
        }

        val bindPort = findAvailablePort()
        port = bindPort

        server = embeddedServer(CIO, host = "127.0.0.1", port = bindPort) {
            routing {
                get("/stream") {
                    val rawUrl = call.request.queryParameters["url"]
                        ?: return@get call.respondText(
                            text = "Missing url parameter",
                            contentType = ContentType.Text.Plain,
                        )
                    val decodedUrl = URLDecoder.decode(rawUrl, "UTF-8")

                    val response = httpClient.get(decodedUrl) {
                        getHeaders().forEach { (name, value) -> header(name, value) }
                    }

                    if (decodedUrl.contains(".m3u8")) {
                        val playlist = response.bodyAsText()
                        val rewritten = rewritePlaylist(playlist = playlist, currentUrl = decodedUrl)
                        call.respondText(
                            text = rewritten,
                            contentType = ContentType.parse("application/vnd.apple.mpegurl"),
                        )
                    } else {
                        call.respondBytes(
                            bytes = response.body(),
                            contentType = response.contentType() ?: ContentType.Application.OctetStream,
                        )
                    }
                }
            }
        }.start(wait = false)
    }

    fun proxiedUrl(realUrl: String): String {
        start()
        val encoded = URLEncoder.encode(realUrl, "UTF-8")
        return "http://127.0.0.1:$port/stream?url=$encoded"
    }

    fun stop() {
        server?.stop(1000, 2000)
        server = null
        port = 0
    }

    private fun rewritePlaylist(playlist: String, currentUrl: String): String {
        val currentUri = URI(currentUrl)
        return playlist.lines().joinToString("\n") { line ->
            val trimmed = line.trim()
            if (trimmed.isNotEmpty() && !trimmed.startsWith("#")) {
                proxiedUrl(currentUri.resolve(trimmed).toString())
            } else {
                line
            }
        }
    }

    private fun findAvailablePort(): Int = ServerSocket(0).use { socket ->
        socket.reuseAddress = true
        socket.localPort
    }
}
