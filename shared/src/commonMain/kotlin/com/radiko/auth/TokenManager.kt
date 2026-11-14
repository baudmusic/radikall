package com.radiko.auth

import com.radiko.station.Station
import kotlin.time.Duration.Companion.minutes
import kotlinx.datetime.Clock

class TokenManager(
    private val auth: RadikoAuth,
    private val clock: Clock = Clock.System,
) {
    private val maxAge = 70.minutes
    private val sessions = mutableMapOf<String, AuthSession>()

    suspend fun getSession(areaId: String): AuthSession {
        val existing = sessions[areaId]
        if (existing != null && clock.now() - existing.requestedAt < maxAge) {
            return existing
        }

        return auth.authenticate(areaId).also { session ->
            sessions[session.areaId] = session
            if (session.areaId != areaId) {
                sessions[areaId] = session
            }
        }
    }

    suspend fun getSessionForStation(station: Station, preferredAreaId: String): AuthSession {
        val candidateAreas = buildList {
            if (station.areaIds.contains(preferredAreaId)) {
                add(preferredAreaId)
            }
            addAll(station.areaIds.filterNot { it == preferredAreaId })
        }

        for (areaId in candidateAreas) {
            val existing = sessions[areaId]
            if (existing != null && clock.now() - existing.requestedAt < maxAge) {
                return existing
            }
        }

        return getSession(candidateAreas.first())
    }

    /** 丢掉这个地区的会话；同一个会话可能还以别的地区为键存了一份，一并丢掉。 */
    fun invalidate(areaId: String) {
        val session = sessions.remove(areaId) ?: return
        sessions.values.removeAll { it == session }
    }
}

