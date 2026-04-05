package com.radiko.ui.viewmodel

import com.radiko.auth.RadikoAuth
import com.radiko.auth.TokenManager
import com.radiko.platform.PlatformEnvironment
import com.radiko.program.ProgramRepository
import com.radiko.settings.SettingsRepository
import com.radiko.stream.StreamUrlResolver

object RadikoAppGraph {
    private val httpClient by lazy { PlatformEnvironment.httpClient }
    private val auth by lazy { RadikoAuth(httpClient) }
    private val tokenManager by lazy { TokenManager(auth) }
    private val streamUrlResolver by lazy { StreamUrlResolver(httpClient) }
    private val programRepository by lazy { ProgramRepository(httpClient) }
    val settingsRepository: SettingsRepository by lazy { SettingsRepository() }

    val playerViewModel: PlayerViewModel by lazy {
        PlayerViewModel(
            tokenManager = tokenManager,
            streamUrlResolver = streamUrlResolver,
            programRepository = programRepository,
            radioPlayer = PlatformEnvironment.createPlayer(),
            settingsRepository = settingsRepository,
        )
    }

    val nowPlayingViewModel: NowPlayingViewModel by lazy {
        NowPlayingViewModel(programRepository = programRepository)
    }

    fun createStationListViewModel(): StationListViewModel = StationListViewModel()
}
