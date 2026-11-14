package com.radiko.ui.viewmodel

import com.radiko.auth.TokenManager
import com.radiko.i18n.AppLocalizer
import com.radiko.platform.ConnectionType
import com.radiko.platform.PlatformConnectionInfo
import com.radiko.player.RadioPlayer
import com.radiko.player.StreamPlaybackRequest
import com.radiko.program.NowOnAirRepository
import com.radiko.program.ProgramRepository
import com.radiko.settings.SettingsRepository
import com.radiko.station.ProgramEntry
import com.radiko.station.Station
import com.radiko.station.StationRegistry
import com.radiko.stream.StreamUrlResolver
import kotlinx.coroutines.CancellationException
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
    /** 用户正在浏览的地区（地区选择器和电台列表），只由 [PlayerViewModel.switchArea] 改变。 */
    val currentAreaId: String = "JP13",
    /** 当前电台实际用来认证的地区，可以和浏览地区不同；还没播放成功过时为 null。 */
    val playbackAreaId: String? = null,
    val pendingMobileDataStationId: String? = null,
    val sleepTimerEndsAtMillis: Long? = null,
    val error: String? = null,
)

class PlayerViewModel(
    private val tokenManager: TokenManager,
    private val streamUrlResolver: StreamUrlResolver,
    private val programRepository: ProgramRepository,
    private val nowOnAirRepository: NowOnAirRepository,
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
    private var recoveryJob: Job? = null
    /** 最近一次交给播放器的电台。换台时 [PlayerState.currentStation] 要等新台开播成功才更新，重连得用这个。 */
    private var requestedStationId: String? = null
    private var recoveryAttempts = 0
    private var lastFailureAtMillis = 0L
    private var hasAttemptedStartupPlayback = false

    init {
        scope.launch {
            radioPlayer.isPlaying.collect { isPlaying ->
                _state.update { it.copy(isPlaying = isPlaying) }
            }
        }
        scope.launch {
            radioPlayer.playbackFailures.collect { recoverFromPlaybackFailure() }
        }
    }

    /**
     * 播放器自己停了（网络断太久、令牌过期等）：丢掉旧令牌重新连，间隔逐次拉长。
     * 重连本身失败（刚恢复的网络还不稳，认证请求也断了）同样走这里，排下一次，而不是立刻放弃。
     * 连续失败超过 [RECOVERY_DELAYS_MS] 的次数（约 1 分钟）才放弃：彻底停下并提示，
     * 这样留下的是干净的「已停止」状态，之后再按播放会从头开始。
     */
    private fun recoverFromPlaybackFailure() {
        val stationId = requestedStationId ?: return
        val now = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
        if (now - lastFailureAtMillis > RECOVERY_RESET_AFTER_MS) {
            recoveryAttempts = 0
        }
        lastFailureAtMillis = now

        if (recoveryAttempts >= RECOVERY_DELAYS_MS.size) {
            stop()
            val strings = AppLocalizer.strings(settingsRepository.state.value.language)
            _state.update { it.copy(error = strings.playbackConnectionLost) }
            return
        }

        val delayMillis = RECOVERY_DELAYS_MS[recoveryAttempts++]
        _state.value.playbackAreaId?.let(tokenManager::invalidate)
        recoveryJob?.cancel()
        recoveryJob = scope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            delay(delayMillis)
            startPlayback(stationId, isRecovery = true)
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

    /**
     * @param isRecovery 自动重连发起的。这时失败不立刻停，而是交回 [recoverFromPlaybackFailure] 排下一次。
     */
    private fun startPlayback(stationId: String, isRecovery: Boolean = false) {
        val strings = AppLocalizer.strings(settingsRepository.state.value.language)
        val station = StationRegistry.getStation(stationId)
            ?: run {
                _state.update { it.copy(error = "${strings.unknownStationFallback}: $stationId") }
                return
            }

        // 用户换台或重连本身都会走到这里；不管哪种，之前排着的重连都作废。
        recoveryJob?.cancel()
        recoveryJob = null
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
                    preferredAreaId = preferredAuthAreaFor(station),
                )
                val streamUrl = streamUrlResolver.buildLiveStreamUrl(station.id)
                val headers = mapOf(
                    "X-Radiko-AuthToken" to authSession.token,
                    "X-Radiko-AreaId" to authSession.areaId,
                )
                requestedStationId = station.id
                radioPlayer.play(
                    StreamPlaybackRequest(
                        station = station,
                        streamUrl = streamUrl,
                        headers = headers,
                    )
                )
                // 节目信息取不到不影响收听：流已经在放了，别因为它把播放停掉。
                val program = runCatching { currentProgramFor(station, authSession.areaId) }
                    .onFailure { if (it is CancellationException) throw it }
                    .getOrNull()
                authSession.areaId to program
            }.onSuccess { (areaId, program) ->
                // 只记录播放地区；浏览地区和"上次地区"设置都保持用户自己的选择。
                _state.update {
                    it.copy(
                        currentStation = station,
                        currentProgram = program,
                        playbackAreaId = areaId,
                        isLoading = false,
                        error = null,
                    )
                }
                radioPlayer.updateMetadata(station, program)
                settingsRepository.rememberLastStation(station.id)
                startProgramRefresh(station)
            }.onFailure { throwable ->
                // 被换台/停止取消的旧任务，什么都别动（以前这里会把刚换上的播放器一起停掉）。
                if (throwable is CancellationException) return@onFailure
                if (isRecovery) {
                    recoverFromPlaybackFailure()
                    return@onFailure
                }
                requestedStationId = null
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

    /** 只切换浏览的地区。正在播放的电台继续播放，即使它不属于新地区。 */
    fun switchArea(areaId: String) {
        _state.update { it.copy(currentAreaId = areaId, error = null) }
        settingsRepository.rememberLastArea(areaId)
    }

    /**
     * 认证地区必须覆盖该电台才能放流：优先用正在浏览的地区，其次沿用上次播放成功的地区，
     * 都不覆盖时用电台自己的第一个收听地区。[TokenManager] 仍会优先复用已缓存的会话。
     */
    private fun preferredAuthAreaFor(station: Station): String {
        val state = _state.value
        return listOfNotNull(state.currentAreaId, state.playbackAreaId)
            .firstOrNull { it in station.areaIds }
            ?: station.areaIds.firstOrNull()
            ?: state.currentAreaId
    }

    fun stop() {
        recoveryJob?.cancel()
        recoveryJob = null
        requestedStationId = null
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

    /**
     * 先从地区 now 数据取（约 60KB，和首页卡片共用缓存，5 分钟内不重复请求），
     * 取不到再退回该台的周节目表（约 700KB）。
     */
    private suspend fun currentProgramFor(station: Station, areaId: String?): ProgramEntry? =
        nowOnAirRepository.currentProgram(station, listOfNotNull(areaId))
            ?: programRepository.currentProgram(station.id)

    /**
     * 每 30 秒看一次节目有没有换。大多数时候只是读缓存（缓存里已有下一档），
     * 不再像以前那样每分钟重新下载整份周节目表；只有节目真的变了才更新状态和系统媒体信息。
     */
    private fun startProgramRefresh(station: Station) {
        programRefreshJob?.cancel()
        programRefreshJob = scope.launch {
            while (isActive) {
                delay(30_000L)
                runCatching { currentProgramFor(station, _state.value.playbackAreaId) }
                    .onSuccess { program ->
                        if (program != _state.value.currentProgram) {
                            _state.update { current -> current.copy(currentProgram = program) }
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
            runCatching { currentProgramFor(station, _state.value.playbackAreaId) }
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
        // 上一台/下一台在播放地区的电台列表里循环，不受浏览地区影响。
        val cycleAreaId = listOfNotNull(currentState.playbackAreaId, currentState.currentAreaId)
            .firstOrNull { it in currentStation.areaIds }
            ?: currentStation.areaIds.firstOrNull()
            ?: return
        val stations = StationRegistry.getStationsForArea(cycleAreaId)
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

    private companion object {
        /** 自动重连的等待时间（合计约 1 分钟），用完就放弃。 */
        val RECOVERY_DELAYS_MS = longArrayOf(2_000L, 4_000L, 8_000L, 15_000L, 30_000L)

        /** 距上次失败超过这么久，说明中间已经好好放了一阵，重连次数重新算。 */
        const val RECOVERY_RESET_AFTER_MS = 2 * 60_000L
    }
}

private fun Int.floorMod(size: Int): Int {
    if (size <= 0) {
        return 0
    }
    val result = this % size
    return if (result < 0) result + size else result
}
