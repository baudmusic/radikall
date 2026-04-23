package com.radiko.webproxy

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.minutes
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

data class LivePlaybackSession(
    val id: String,
    val stationId: String,
    val resolvedAreaId: String,
    val authToken: String,
    val userAgent: String,
    val upstreamMasterUrl: String,
    val createdAt: Instant,
    val expiresAt: Instant,
)

class LivePlaybackSessionStore(
    private val clock: Clock = Clock.System,
) {
    private val ttl = 55.minutes
    private val sessions = ConcurrentHashMap<String, LivePlaybackSession>()

    fun create(
        stationId: String,
        resolvedAreaId: String,
        authToken: String,
        userAgent: String,
        upstreamMasterUrl: String,
    ): LivePlaybackSession {
        cleanupExpired()
        val createdAt = clock.now()
        val session = LivePlaybackSession(
            id = UUID.randomUUID().toString(),
            stationId = stationId,
            resolvedAreaId = resolvedAreaId,
            authToken = authToken,
            userAgent = userAgent,
            upstreamMasterUrl = upstreamMasterUrl,
            createdAt = createdAt,
            expiresAt = createdAt + ttl,
        )
        sessions[session.id] = session
        return session
    }

    fun get(sessionId: String): LivePlaybackSession? {
        val session = sessions[sessionId] ?: return null
        if (clock.now() >= session.expiresAt) {
            sessions.remove(sessionId)
            return null
        }
        return session
    }

    fun invalidate(sessionId: String) {
        sessions.remove(sessionId)
    }

    private fun cleanupExpired() {
        val now = clock.now()
        sessions.entries.removeIf { (_, session) -> now >= session.expiresAt }
    }
}
