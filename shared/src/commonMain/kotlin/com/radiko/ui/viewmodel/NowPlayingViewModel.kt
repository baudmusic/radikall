package com.radiko.ui.viewmodel

import com.radiko.program.ProgramRepository
import com.radiko.station.OnAirSong
import com.radiko.station.ProgramEntry
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class OnAirHistoryMode {
    CURRENT_PROGRAM,
    FULL_STATION,
}

data class NowPlayingState(
    val isVisible: Boolean = false,
    val stationId: String? = null,
    val onAirSongs: List<OnAirSong> = emptyList(),
    val todayPrograms: List<ProgramEntry> = emptyList(),
    val weeklyPrograms: List<ProgramEntry> = emptyList(),
    val selectedDayIndex: Int = 0,
    val isLoadingSongs: Boolean = false,
    val isLoadingSchedule: Boolean = false,
    val showAllSongs: Boolean = false,
    val historyMode: OnAirHistoryMode = OnAirHistoryMode.CURRENT_PROGRAM,
)

class NowPlayingViewModel(
    private val programRepository: ProgramRepository,
    private val clock: Clock = Clock.System,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val _state = MutableStateFlow(NowPlayingState())
    val state: StateFlow<NowPlayingState> = _state.asStateFlow()

    private var songsRefreshJob: Job? = null
    private var scheduleRefreshJob: Job? = null

    fun show(stationId: String) {
        val stationChanged = _state.value.stationId != stationId
        _state.update { current ->
            current.copy(
                isVisible = true,
                stationId = stationId,
                onAirSongs = if (stationChanged) emptyList() else current.onAirSongs,
                weeklyPrograms = if (stationChanged) emptyList() else current.weeklyPrograms,
                todayPrograms = if (stationChanged) emptyList() else current.todayPrograms,
                selectedDayIndex = if (stationChanged) 0 else current.selectedDayIndex,
                isLoadingSongs = true,
                isLoadingSchedule = stationChanged || current.weeklyPrograms.isEmpty(),
                showAllSongs = false,
            )
        }

        startSongsRefresh(stationId)
        startScheduleRefresh(stationId)
    }

    fun hide() {
        songsRefreshJob?.cancel()
        scheduleRefreshJob?.cancel()
        _state.update {
            it.copy(
                isVisible = false,
                isLoadingSongs = false,
                isLoadingSchedule = false,
            )
        }
    }

    fun toggleShowAllSongs() {
        _state.update { it.copy(showAllSongs = !it.showAllSongs) }
    }

    fun setHistoryMode(mode: OnAirHistoryMode) {
        _state.update {
            it.copy(
                historyMode = mode,
                showAllSongs = false,
            )
        }
    }

    fun selectDay(dayIndex: Int) {
        _state.update { current ->
            val (resolvedIndex, dayPrograms) = selectProgramsForDay(current.weeklyPrograms, dayIndex)
            current.copy(
                selectedDayIndex = resolvedIndex,
                todayPrograms = dayPrograms,
            )
        }
    }

    private fun startSongsRefresh(stationId: String) {
        songsRefreshJob?.cancel()
        songsRefreshJob = scope.launch {
            while (isActive) {
                val songs = programRepository.fetchOnAirSongs(stationId, size = 50)
                _state.update { current ->
                    if (current.stationId != stationId) {
                        current
                    } else {
                        current.copy(
                            onAirSongs = songs,
                            isLoadingSongs = false,
                        )
                    }
                }
                delay(15_000L)
            }
        }
    }

    private fun startScheduleRefresh(stationId: String) {
        scheduleRefreshJob?.cancel()
        scheduleRefreshJob = scope.launch {
            while (isActive) {
                val weeklyPrograms = filterProgramsFromToday(programRepository.fetchWeeklyPrograms(stationId))
                _state.update { current ->
                    if (current.stationId != stationId) {
                        current
                    } else {
                        val (resolvedIndex, dayPrograms) = selectProgramsForDay(
                            weeklyPrograms,
                            current.selectedDayIndex,
                        )
                        current.copy(
                            weeklyPrograms = weeklyPrograms,
                            todayPrograms = dayPrograms,
                            selectedDayIndex = resolvedIndex,
                            isLoadingSchedule = false,
                        )
                    }
                }
                delay(300_000L)
            }
        }
    }

    private fun selectProgramsForDay(
        programs: List<ProgramEntry>,
        preferredIndex: Int,
    ): Pair<Int, List<ProgramEntry>> {
        val days = programs.map { it.startAt.take(8) }.distinct().sorted()
        if (days.isEmpty()) {
            return 0 to emptyList()
        }

        val resolvedIndex = preferredIndex.coerceIn(0, days.lastIndex)
        val targetDate = days[resolvedIndex]
        return resolvedIndex to programs.filter { it.startAt.startsWith(targetDate) }
    }

    private fun filterProgramsFromToday(programs: List<ProgramEntry>): List<ProgramEntry> {
        val today = formatTokyoDate()
        return programs.filter { it.startAt.take(8) >= today }
    }

    private fun formatTokyoDate(): String {
        val dateTime = clock.now().toLocalDateTime(TimeZone.of("Asia/Tokyo"))
        return buildString {
            append(dateTime.year.toString().padStart(4, '0'))
            append(dateTime.monthNumber.toString().padStart(2, '0'))
            append(dateTime.dayOfMonth.toString().padStart(2, '0'))
        }
    }
}
