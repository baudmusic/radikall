package com.radiko.program

import com.radiko.station.ProgramEntry
import com.radiko.station.StationRegistry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

class NowOnAirRepositoryTest {
    private class MutableClock(var now: Instant) : Clock {
        override fun now(): Instant = now
    }

    // 东京 12:30。
    private val clock = MutableClock(Instant.parse("2026-11-14T03:30:00Z"))
    private val fetched = mutableListOf<String>()
    private var failNext = false

    private val repository = NowOnAirRepository(
        fetchArea = { areaId ->
            synchronized(fetched) { fetched += areaId }
            delay(20)
            if (failNext) {
                failNext = false
                error("boom")
            }
            AreaNowPrograms(
                areaId = areaId,
                ttlSeconds = 300,
                programsByStation = stationsIn(areaId).associate { station ->
                    station.id to listOf(
                        program(station.id, "${station.id} 12時台", "20261114120000", "20261114130000"),
                        program(station.id, "${station.id} 13時台", "20261114130000", "20261114140000"),
                    )
                },
            )
        },
        clock = clock,
    )

    private fun stationsIn(areaId: String) = StationRegistry.allStations.filter { areaId in it.areaIds }

    private fun program(stationId: String, title: String, startAt: String, endAt: String) = ProgramEntry(
        stationId = stationId,
        title = title,
        description = "",
        performer = null,
        startAt = startAt,
        endAt = endAt,
        info = null,
        imageUrl = null,
        url = null,
    )

    @Test
    fun fetchesOneAreaForAnAreaListAndReusesItWithinTtl() = runBlocking<Unit> {
        repository.ensureStations(stationsIn("JP13"), listOf("JP13"))
        assertEquals(listOf("JP13"), fetched)
        assertEquals("TBS 12時台", repository.programAt("TBS")?.title)

        val snapshot = repository.programs.value
        repository.ensureStations(stationsIn("JP13"), listOf("JP13"))
        assertEquals(1, fetched.size)
        assertSame(snapshot, repository.programs.value)
    }

    @Test
    fun rollsOverToNextProgramWithoutRefetching() = runBlocking<Unit> {
        repository.ensureStations(stationsIn("JP13"), listOf("JP13"))
        clock.now = Instant.parse("2026-11-14T04:00:00Z") // 东京 13:00
        assertEquals("TBS 13時台", repository.programAt("TBS")?.title)
        assertEquals(1, fetched.size)
    }

    @Test
    fun mergesConcurrentRequestsForTheSameArea() = runBlocking<Unit> {
        listOf(
            async { repository.ensureStations(stationsIn("JP27"), listOf("JP27")) },
            async { repository.ensureStations(stationsIn("JP27"), listOf("JP27")) },
            async { repository.ensureStations(listOf(StationRegistry.getStation("802")!!)) },
        ).awaitAll()
        assertEquals(listOf("JP27"), fetched)
    }

    @Test
    fun refetchesAfterTtlAndKeepsOldDataMeanwhile() = runBlocking<Unit> {
        repository.ensureStations(stationsIn("JP13"), listOf("JP13"))
        clock.now += 301.seconds
        val pending = async { repository.ensureStations(stationsIn("JP13"), listOf("JP13")) }
        assertNotNull(repository.programs.value["TBS"], "刷新期间旧数据应保留")
        pending.await()
        assertEquals(listOf("JP13", "JP13"), fetched)
    }

    @Test
    fun backsOffForThirtySecondsAfterFailure() = runBlocking<Unit> {
        failNext = true
        repository.ensureStations(stationsIn("JP13"), listOf("JP13"))
        assertNull(repository.programAt("TBS"))
        repository.ensureStations(stationsIn("JP13"), listOf("JP13"))
        assertEquals(1, fetched.size, "30 秒内不应重试")

        clock.now += 31.seconds
        repository.ensureStations(stationsIn("JP13"), listOf("JP13"))
        assertEquals(2, fetched.size)
        assertNotNull(repository.programAt("TBS"))
    }

    @Test
    fun marksStationsSettledAfterSuccessOrFailure() = runBlocking<Unit> {
        assertEquals(emptySet(), repository.settledStationIds.value)
        failNext = true
        repository.ensureStations(stationsIn("JP27"), listOf("JP27"))
        // 失败也算「请求过」：卡片不再显示加载骨架，改为显示台名。
        assertEquals(stationsIn("JP27").map { it.id }.toSet(), repository.settledStationIds.value)
        assertNull(repository.programAt("802"))

        repository.ensureStations(stationsIn("JP13"), listOf("JP13"))
        assertTrue("TBS" in repository.settledStationIds.value)
    }

    @Test
    fun programProgressFollowsTheClock() {
        val program = program("TBS", "x", "20261114120000", "20261114130000")
        assertEquals(0.5f, program.progressAt(Instant.parse("2026-11-14T03:30:00Z")), 0.001f)
        assertEquals(0f, program.progressAt(Instant.parse("2026-11-14T02:00:00Z")))
        assertEquals(1f, program.progressAt(Instant.parse("2026-11-14T05:00:00Z")))
        assertEquals(program, listOf(program).programAt(Instant.parse("2026-11-14T03:59:59Z")))
        assertNull(listOf(program).programAt(Instant.parse("2026-11-14T04:00:00Z")))
    }

    @Test
    fun currentProgramUsesPreferredArea() = runBlocking<Unit> {
        val program = repository.currentProgram(StationRegistry.getStation("TBS")!!, listOf("JP14"))
        assertEquals("TBS 12時台", program?.title)
        assertEquals(listOf("JP14"), fetched)
    }
}
