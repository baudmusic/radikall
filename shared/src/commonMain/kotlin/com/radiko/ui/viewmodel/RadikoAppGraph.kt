package com.radiko.ui.viewmodel

import com.radiko.auth.RadikoAuth
import com.radiko.auth.TokenManager
import com.radiko.platform.PlatformEnvironment
import com.radiko.platform.PlatformRuntimeInfo
import com.radiko.program.NowOnAirRepository
import com.radiko.program.ProgramRepository
import com.radiko.settings.SettingsRepository
import com.radiko.stream.StreamUrlResolver
import com.radiko.update.AppUpdateChecker
import com.radiko.update.platformUpdateTarget

object RadikoAppGraph {
    private val httpClient by lazy { PlatformEnvironment.httpClient }
    private val auth by lazy { RadikoAuth(httpClient) }
    private val tokenManager by lazy { TokenManager(auth) }
    private val streamUrlResolver by lazy { StreamUrlResolver(httpClient) }
    private val programRepository by lazy { ProgramRepository(httpClient) }
    val settingsRepository: SettingsRepository by lazy { SettingsRepository() }

    /** 各电台当前节目的缓存，首页卡片和播放器共用。 */
    val nowOnAirRepository: NowOnAirRepository by lazy {
        NowOnAirRepository(fetchArea = programRepository::fetchAreaNowPrograms)
    }

    val playerViewModel: PlayerViewModel by lazy {
        PlayerViewModel(
            tokenManager = tokenManager,
            streamUrlResolver = streamUrlResolver,
            programRepository = programRepository,
            nowOnAirRepository = nowOnAirRepository,
            radioPlayer = PlatformEnvironment.createPlayer(),
            settingsRepository = settingsRepository,
        )
    }

    val nowPlayingViewModel: NowPlayingViewModel by lazy {
        NowPlayingViewModel(programRepository = programRepository)
    }

    /** 启动时查 GitHub Releases 有没有新版安装包（Android / Windows）。 */
    val appUpdateChecker: AppUpdateChecker by lazy {
        val currentVersion = { PlatformRuntimeInfo.versionName }
        AppUpdateChecker(
            currentVersion = currentVersion,
            target = platformUpdateTarget,
            fetchReleases = AppUpdateChecker.githubFetcher(httpClient, currentVersion),
        )
    }

    fun createStationListViewModel(): StationListViewModel = StationListViewModel()
}
