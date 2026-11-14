package com.radiko.program

import com.radiko.station.ProgramEntry
import com.radiko.station.Station
import com.radiko.station.StationCatalog
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

/**
 * 各电台「正在播 / 下一档」节目的缓存，首页卡片和播放器共用。
 * 数据来自 radiko 按地区的 now 接口：一次请求拿到一个地区所有电台的两档节目；
 * 有效期（radiko 给的 ttl，通常 300 秒）内不重复请求，同一地区的并发请求合并成一次，
 * 过期后旧数据继续可用，直到新数据回来。
 * Web 端的 webApp/src/nowOnAir.ts 是同一套规则。
 */
class NowOnAirRepository(
    private val fetchArea: suspend (areaId: String) -> AreaNowPrograms,
    private val clock: Clock = Clock.System,
) {
    // 请求放在自己的作用域里：发起请求的界面中途离开，也不会取消其他人正在等的同一个请求。
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val mutex = Mutex()
    private val freshUntil = mutableMapOf<String, Instant>()
    private val retryAt = mutableMapOf<String, Instant>()
    private val inFlight = mutableMapOf<String, Deferred<Unit>>()

    private val _programs = MutableStateFlow<Map<String, List<ProgramEntry>>>(emptyMap())

    /** 键是台号；每个电台按开始时间排好的「当前 + 下一档」节目。 */
    val programs: StateFlow<Map<String, List<ProgramEntry>>> = _programs.asStateFlow()

    private val _settledStationIds = MutableStateFlow<Set<String>>(emptySet())

    /** 至少请求过一次（成功或失败）的电台。没数据又不在这里，说明还在加载。 */
    val settledStationIds: StateFlow<Set<String>> = _settledStationIds.asStateFlow()

    /**
     * 确保这些电台有有效期内的节目数据：只为过期或缺失的电台请求，并挑最少的地区去请求。
     * 返回时数据已更新；请求失败时保留旧数据，30 秒内不再重试这些电台。不会抛异常。
     */
    suspend fun ensureStations(stations: Collection<Station>, preferredAreaIds: List<String> = emptyList()) {
        val tasks = mutex.withLock {
            val now = clock.now()
            val stale = stations.filter { station ->
                freshUntil[station.id].let { it == null || now >= it } &&
                    retryAt[station.id].let { it == null || now >= it }
            }
            if (stale.isEmpty()) return
            // 正在请求的地区排在最前，能顺带覆盖的就直接等它，不另发请求。
            StationCatalog.coveringAreas(stale, inFlight.keys.toList() + preferredAreaIds)
                .map { areaId -> inFlight.getOrPut(areaId) { startFetch(areaId, stale) } }
        }
        tasks.awaitAll()
    }

    /** 某台在某一时刻（默认现在）正在播的节目；没有数据时返回 null。数据里有下一档，所以整点切换不需要重新请求。 */
    fun programAt(stationId: String, at: Instant = clock.now()): ProgramEntry? = _programs.value[stationId].programAt(at)

    /** 拿某台现在的节目，必要时先请求它所在地区（优先 [preferredAreaIds]）。 */
    suspend fun currentProgram(station: Station, preferredAreaIds: List<String> = emptyList()): ProgramEntry? {
        ensureStations(listOf(station), preferredAreaIds)
        return programAt(station.id)
    }

    private fun startFetch(areaId: String, requested: List<Station>): Deferred<Unit> = scope.async {
        val result = runCatching { fetchArea(areaId) }
        mutex.withLock {
            inFlight.remove(areaId)
            val now = clock.now()
            result
                .onSuccess { feed ->
                    val until = now + feed.ttlSeconds.seconds
                    feed.programsByStation.keys.forEach { stationId ->
                        freshUntil[stationId] = until
                        retryAt.remove(stationId)
                    }
                    // 按收听地区本该包含、这次却没返回的电台：等下一个有效期再试，免得每次都重新请求。
                    val missing = requested.filter { areaId in it.areaIds && it.id !in feed.programsByStation }
                    missing.forEach { retryAt[it.id] = until }
                    _programs.update { it + feed.programsByStation }
                    _settledStationIds.update { it + feed.programsByStation.keys + missing.map(Station::id) }
                }
                .onFailure {
                    val until = now + RetryAfterFailure
                    val affected = requested.filter { areaId in it.areaIds }
                    affected.forEach { retryAt[it.id] = until }
                    _settledStationIds.update { it + affected.map(Station::id) }
                }
        }
    }

    private companion object {
        /** 请求失败、或地区数据里意外缺了某台时，隔多久再试。 */
        val RetryAfterFailure = 30.seconds
    }
}

/** 节目列表（按开始时间排好）里某一时刻正在播的那一档。 */
fun List<ProgramEntry>?.programAt(at: Instant): ProgramEntry? {
    val timestamp = tokyoCompactTimestamp(at)
    return this?.firstOrNull { timestamp >= it.startAt && timestamp < it.endAt }
}

/** 节目已播出的比例（0–1），用来画卡片底部的进度细线。 */
fun ProgramEntry.progressAt(at: Instant): Float {
    val start = parseTokyoCompactTimestamp(startAt) ?: return 0f
    val end = parseTokyoCompactTimestamp(endAt) ?: return 0f
    if (end <= start) return 0f
    return ((at - start) / (end - start)).toFloat().coerceIn(0f, 1f)
}
