package com.radiko.ui.viewmodel

import com.radiko.auth.TokenManager
import com.radiko.i18n.AppLocalizer
import com.radiko.platform.ConnectionType
import com.radiko.platform.PlatformConnectionInfo
import com.radiko.player.RadioPlayer
import com.radiko.player.StreamPlaybackRequest
import com.radiko.program.ProgramRepository
import com.radiko.settings.SettingsRepository
import com.radiko.station.ProgramEntry
import com.radiko.station.Station
import com.radiko.station.StationRegistry
import com.radiko.stream.StreamUrlResolver
import kotlinx.coroutines.Job
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class PlayerState(
    val currentStation: Station? = null,
    val currentProgram: ProgramEntry? = null,
    val isPlaying: Boolean = false,
    val isLoading: Boolean = false,
    val currentAreaId: String = "JP13",
    val pendingMobileDataStationId: String? = null,
    val sleepTimerEndsAtMillis: Long? = null,
    val error: String? = null,
)

class PlayerViewModel(
    private val tokenManager: TokenManager,
    private val streamUrlResolver: StreamUrlResolver,
    private val programRepository: ProgramRepository,
    private val radioPlayer: RadioPlayer,
    private val settingsRepository: SettingsRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val _state = MutableStateFlow(
        PlayerState(currentAreaId = settingsRepository.state.value.resolvedStartupAreaId()),
    )
    val state: StateFlow<PlayerState> = _state.asStateFlow()

    private var activePlayJob: Job? = null
    private var programRefreshJob: Job? = null
    private var sleepTimerJob: Job? = null
    private var hasAttemptedStartupPlayback = false

    init {
        scope.launch {
            radioPlayer.isPlaying.collect { isPlaying ->
                _state.update { it.copy(isPlaying = isPlaying) }
            }
        }
    }

    fun playStation(
        stationId: String,
        bypassCellularConfirmation: Boolean = false,
    ) {
        val currentSettings = settingsRepository.state.value
        val strings = AppLocalizer.strings(currentSettings.language)
        val connectionType = PlatformConnectionInfo.currentConnectionType()
        if (currentSettings.wifiOnlyPlayback && connectionType == ConnectionType.CELLULAR) {
            _state.update {
                it.copy(
                    isLoading = false,
                    pendingMobileDataStationId = null,
                    error = strings.wifiOnlyPlaybackError,
                )
            }
            return
        }
        if (!bypassCellularConfirmation &&
            currentSettings.confirmMobileDataPlayback &&
            connectionType == ConnectionType.CELLULAR
        ) {
            _state.update {
                it.copy(
                    isLoading = false,
                    pendingMobileDataStationId = stationId,
                    error = null,
                )
            }
            return
        }
        startPlayback(stationId)
    }

    fun confirmMobileDataPlayback() {
        val stationId = _state.value.pendingMobileDataStationId ?: return
        _state.update { it.copy(pendingMobileDataStationId = null, error = null) }
        startPlayback(stationId)
    }

    fun dismissMobileDataPlaybackPrompt() {
        _state.update { it.copy(pendingMobileDataStationId = null) }
    }

    fun restorePlaybackIfNeeded() {
        if (hasAttemptedStartupPlayback) {
            return
        }
        hasAttemptedStartupPlayback = true
        val settings = settingsRepository.state.value
        val lastStationId = settings.lastStationId ?: return
        if (!settings.autoPlayOnLaunch) {
            return
        }
        playStation(lastStationId)
    }

    fun setSleepTimer(durationMinutes: Int?) {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        if (durationMinutes == null) {
            _state.update { it.copy(sleepTimerEndsAtMillis = null) }
            return
        }

        val endsAt = kotlinx.datetime.Clock.System.now().toEpochMilliseconds() +
            durationMinutes * 60_000L
        _state.update { it.copy(sleepTimerEndsAtMillis = endsAt) }
        sleepTimerJob = scope.launch {
            delay(durationMinutes * 60_000L)
            stop()
            _state.update { current ->
                current.copy(sleepTimerEndsAtMillis = null)
            }
        }
    }

    private fun startPlayback(stationId: String) {
        val strings = AppLocalizer.strings(settingsRepository.state.value.language)
        val station = StationRegistry.getStation(stationId)
            ?: run {
                _state.update { it.copy(error = "${strings.unknownStationFallback}: $stationId") }
                return
            }

        activePlayJob?.cancel()
        activePlayJob = scope.launch {
            _state.update {
                it.copy(
                    isLoading = true,
                    pendingMobileDataStationId = null,
                    error = null,
                )
            }

            runCatching {
                val authSession = tokenManager.getSessionForStation(
                    station = station,
                    preferredAreaId = _state.value.currentAreaId,
                )
                val streamUrl = streamUrlResolver.buildLiveStreamUrl(station.id)
                val headers = mapOf(
                    "X-Radiko-AuthToken" to authSession.token,
                    "X-Radiko-AreaId" to authSession.areaId,
                )
                radioPlayer.play(
                    StreamPlaybackRequest(
                        station = station,
                        streamUrl = streamUrl,
                        headers = headers,
                    )
                )
                val program = programRepository.currentProgram(station.id)
                authSession.areaId to program
            }.onSuccess { (areaId, program) ->
                _state.update {
                    it.copy(
                        currentStation = station,
                        currentProgram = program,
                        currentAreaId = areaId,
                        isLoading = false,
                        error = null,
                    )
                }
                radioPlayer.updateMetadata(station, program)
                settingsRepository.rememberLastArea(areaId)
                settingsRepository.rememberLastStation(station.id)
                startProgramRefresh(station.id)
            }.onFailure { throwable ->
                radioPlayer.stop()
                _state.update {
                    it.copy(
                        isLoading = false,
                        isPlaying = false,
                        error = throwable.message ?: strings.genericPlaybackError,
                    )
                }
            }
        }
    }

    fun switchArea(areaId: String) {
        _state.update { it.copy(currentAreaId = areaId, error = null) }
        settingsRepository.rememberLastArea(areaId)
        val currentStation = _state.value.currentStation ?: return
        if (areaId !in currentStation.areaIds) {
            stop()
            _state.update { it.copy(currentStation = null, currentProgram = null) }
        }
    }

    fun stop() {
        activePlayJob?.cancel()
        programRefreshJob?.cancel()
        programRefreshJob = null
        radioPlayer.stop()
        _state.update {
            it.copy(
                isPlaying = false,
                isLoading = false,
                pendingMobileDataStationId = null,
            )
        }
    }

    private fun startProgramRefresh(stationId: String) {
        programRefreshJob?.cancel()
        programRefreshJob = scope.launch {
            while (isActive) {
                delay(60_000L)
                runCatching { programRepository.currentProgram(stationId) }
                    .onSuccess { program ->
                        _state.update { current ->
                            current.copy(currentProgram = program)
                        }
                        _state.value.currentStation?.let { station ->
                            radioPlayer.updateMetadata(station, program)
                        }
                    }
            }
        }
    }

    fun togglePlayback() {
        if (_state.value.isPlaying) {
            stop()
        } else {
            _state.value.currentStation?.let { playStation(it.id) }
        }
    }

    fun playPreviousStation() {
        cycleStation(step = -1)
    }

    fun playNextStation() {
        cycleStation(step = 1)
    }

    fun refreshProgram() {
        val station = _state.value.currentStation ?: return
        scope.launch {
            runCatching { programRepository.currentProgram(station.id) }
                .onSuccess { currentProgram ->
                    _state.update { it.copy(currentProgram = currentProgram) }
                    radioPlayer.updateMetadata(station, currentProgram)
                }
        }
    }

    fun dispose() {
        stop()
        sleepTimerJob?.cancel()
        scope.coroutineContext[Job]?.cancel()
    }

    private fun cycleStation(step: Int) {
        val currentState = _state.value
        val currentStation = currentState.currentStation ?: return
        val stations = StationRegistry.getStationsForArea(currentState.currentAreaId)
        if (stations.isEmpty()) {
            return
        }

        val currentIndex = stations.indexOfFirst { it.id == currentStation.id }
        if (currentIndex < 0) {
            return
        }

        val nextIndex = (currentIndex + step).floorMod(stations.size)
        playStation(stations[nextIndex].id)
    }
}

private fun Int.floorMod(size: Int): Int {
    if (size <= 0) {
        return 0
    }
    val result = this % size
    return if (result < 0) result + size else result
}
