package com.radiko.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.radiko.i18n.appStrings
import com.radiko.platform.PlatformRuntimeInfo
import com.radiko.ui.components.TopBarActionButton
import com.radiko.ui.components.drawSettingsIcon
import com.radiko.ui.theme.radikoPalette
import com.radiko.ui.viewmodel.RadikoAppGraph
import com.radiko.program.programAt
import com.radiko.program.progressAt
import com.radiko.station.ALL_AREAS_ID
import com.radiko.station.StationCatalog
import com.radiko.station.StationRegistry
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.datetime.Clock
import org.jetbrains.compose.resources.painterResource
import radiko_app.shared.generated.resources.Res
import radiko_app.shared.generated.resources.logo1

@Composable
fun HomeScreen(
    showHeader: Boolean = true,
    onOpenSettings: () -> Unit = {},
) {
    val playerViewModel = remember { RadikoAppGraph.playerViewModel }
    val stationListViewModel = remember { RadikoAppGraph.createStationListViewModel() }
    val nowPlayingViewModel = remember { RadikoAppGraph.nowPlayingViewModel }
    val settingsRepository = remember { RadikoAppGraph.settingsRepository }

    val playerState by playerViewModel.state.collectAsState()
    val stationState by stationListViewModel.state.collectAsState()
    val nowPlayingState by nowPlayingViewModel.state.collectAsState()
    val settingsState by settingsRepository.state.collectAsState()
    val appUpdateChecker = remember { RadikoAppGraph.appUpdateChecker }
    val pendingUpdate by appUpdateChecker.pendingUpdate.collectAsState()
    val stationGridState = rememberLazyGridState()
    val strings = appStrings()
    val drivingModeEnabled = settingsState.drivingModeEnabled

    // 卡片上的当前节目：按地区批量取、有效期内复用（见 NowOnAirRepository），时钟每 30 秒走一格，整点自动切到下一档。
    val nowOnAirRepository = remember { RadikoAppGraph.nowOnAirRepository }
    val nowPrograms by nowOnAirRepository.programs.collectAsState()
    val settledStationIds by nowOnAirRepository.settledStationIds.collectAsState()
    val clockNow by produceState(initialValue = Clock.System.now()) {
        while (true) {
            delay(30_000L)
            value = Clock.System.now()
        }
    }
    val preferredAreaIds by rememberUpdatedState(
        listOfNotNull(playerState.currentAreaId.takeIf { it != ALL_AREAS_ID }, playerState.playbackAreaId),
    )

    // 每次启动查一次 GitHub 上有没有新版安装包（内部会等几秒、只查一次，iOS 不查）。
    LaunchedEffect(appUpdateChecker) {
        appUpdateChecker.checkOnLaunch()
    }

    // 只为屏幕上看得到的卡片取节目：滚动停下 150ms 后按可见的台请求，之后每 30 秒确认一次（有效期内不会真的发请求）。
    // 「全国」总表因此不会一打开就把 36 个地区全请求一遍。
    LaunchedEffect(stationGridState, nowOnAirRepository) {
        snapshotFlow { stationGridState.layoutInfo.visibleItemsInfo.mapNotNull { it.key as? String } }
            .distinctUntilChanged()
            .collectLatest { visibleKeys ->
                delay(150L)
                val stations = visibleKeys.mapNotNull(StationRegistry::getStation)
                if (stations.isEmpty()) return@collectLatest
                while (true) {
                    nowOnAirRepository.ensureStations(stations, preferredAreaIds)
                    delay(30_000L)
                }
            }
    }

    LaunchedEffect(playerState.currentAreaId) {
        stationListViewModel.switchArea(playerState.currentAreaId)
    }

    // 换地区或改搜索词后回到列表顶部。LazyGrid 默认按 key 锚住第一张可见的卡，
    // 不处理的话从东京切到「全国」会停在中间的「关东」分组里，看不到开头的「全国播出」。
    LaunchedEffect(stationState.currentAreaId, stationState.searchQuery) {
        stationGridState.scrollToItem(0)
    }

    LaunchedEffect(nowPlayingState.isVisible, playerState.currentStation?.id) {
        val stationId = playerState.currentStation?.id
        if (nowPlayingState.isVisible && stationId != null && stationId != nowPlayingState.stationId) {
            nowPlayingViewModel.show(stationId)
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 24.dp, vertical = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    if (showHeader) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Image(
                                painter = painterResource(Res.drawable.logo1),
                                contentDescription = "Radikall logo",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .height(48.dp)
                                    .padding(vertical = 2.dp),
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                TopBarActionButton(
                                    onClick = onOpenSettings,
                                    buttonSize = 40.dp,
                                    drawIcon = { iconColor -> drawSettingsIcon(iconColor) },
                                )
                                AreaSelector(
                                    currentAreaId = playerState.currentAreaId,
                                    onAreaSelected = { areaId ->
                                        playerViewModel.switchArea(areaId)
                                        stationListViewModel.switchArea(areaId)
                                    },
                                    includeAllAreas = true,
                                )
                            }
                        }
                    }

                    if (!drivingModeEnabled) {
                        OutlinedTextField(
                            modifier = Modifier.fillMaxWidth(),
                            value = stationState.searchQuery,
                            onValueChange = stationListViewModel::updateSearchQuery,
                            singleLine = true,
                            placeholder = {
                                Text(
                                    text = strings.searchStationsPlaceholder,
                                    color = radikoPalette().ink3,
                                )
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedContainerColor = radikoPalette().surfaceSunken,
                                focusedContainerColor = radikoPalette().surfaceSunken,
                                unfocusedBorderColor = radikoPalette().line,
                                focusedBorderColor = radikoPalette().ink3,
                                cursorColor = radikoPalette().info,
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            ),
                        )
                    }

                    playerState.error?.takeIf { it.isNotBlank() }?.let { error ->
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }

                HorizontalDivider(
                    thickness = 1.dp,
                    color = radikoPalette().line,
                )

                Box(modifier = Modifier.weight(1f)) {
                    if (stationState.isEmpty) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = strings.noStationsFound,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                            )
                        }
                    } else {
                        LazyVerticalGrid(
                            modifier = Modifier
                                .fillMaxSize()
                                .edgeSwipeAction(
                                    enabled = !nowPlayingState.isVisible && playerState.currentStation != null,
                                    direction = EdgeSwipeDirection.Up,
                                    canTrigger = { stationGridState.isScrolledToBottom() },
                                    onTrigger = {
                                        playerState.currentStation?.let { station ->
                                            nowPlayingViewModel.show(station.id)
                                        }
                                    },
                                ),
                            state = stationGridState,
                            columns = GridCells.Adaptive(minSize = if (drivingModeEnabled) 340.dp else 300.dp),
                            contentPadding = PaddingValues(if (drivingModeEnabled) 28.dp else 24.dp),
                            horizontalArrangement = Arrangement.spacedBy(if (drivingModeEnabled) 18.dp else 14.dp),
                            verticalArrangement = Arrangement.spacedBy(if (drivingModeEnabled) 18.dp else 14.dp),
                        ) {
                            stationState.sections.forEachIndexed { sectionIndex, section ->
                                strings.stationSectionTitle(section)?.let { title ->
                                    item(
                                        key = "header:${section.key}",
                                        span = { GridItemSpan(maxLineSpan) },
                                        contentType = "header",
                                    ) {
                                        StationSectionHeader(
                                            title = title,
                                            count = section.stations.size,
                                            modifier = Modifier.padding(top = if (sectionIndex > 0) 10.dp else 0.dp),
                                        )
                                    }
                                }
                                // 每个电台在所有分组里只出现一次，所以台号可以直接当 key。
                                items(section.stations, key = { it.id }, contentType = { "station" }) { station ->
                                    val program = nowPrograms[station.id].programAt(clockNow)
                                    StationCard(
                                        station = station,
                                        isActive = playerState.currentStation?.id == station.id,
                                        onClick = { playerViewModel.playStation(station.id) },
                                        program = program,
                                        isPlaying = playerState.isPlaying && playerState.currentStation?.id == station.id,
                                        programState = when {
                                            program != null -> StationProgramState.READY
                                            station.id in settledStationIds -> StationProgramState.UNAVAILABLE
                                            else -> StationProgramState.LOADING
                                        },
                                        progress = program?.progressAt(clockNow) ?: 0f,
                                        endLabel = program?.let { strings.untilTime(it.endAt) },
                                        emphasized = drivingModeEnabled,
                                        areaLabel = if (section.showsCoverage) {
                                            strings.coverageLabel(StationCatalog.coverage(station))
                                        } else {
                                            null
                                        },
                                    )
                                }
                            }
                        }
                    }
                }

                PlayerBar(
                    playerState = playerState,
                    onTogglePlayback = playerViewModel::togglePlayback,
                    onClick = {
                        playerState.currentStation?.let { station ->
                            nowPlayingViewModel.show(station.id)
                        }
                    },
                    onSwipeUp = {
                        playerState.currentStation?.let { station ->
                            nowPlayingViewModel.show(station.id)
                        }
                    },
                    drivingModeEnabled = drivingModeEnabled,
                )
            }

            NowPlayingSheet(
                isVisible = nowPlayingState.isVisible,
                playerState = playerState,
                nowPlayingState = nowPlayingState,
                onDismiss = { nowPlayingViewModel.hide() },
                onToggleShowAllSongs = { nowPlayingViewModel.toggleShowAllSongs() },
                onHistoryModeChange = { mode -> nowPlayingViewModel.setHistoryMode(mode) },
                onSelectDay = { dayIndex -> nowPlayingViewModel.selectDay(dayIndex) },
                onTogglePlayback = playerViewModel::togglePlayback,
                onSelectArea = { areaId ->
                    // 从详情页跳去某个地区浏览：清掉搜索词，让列表直接显示那个地区。
                    playerViewModel.switchArea(areaId)
                    stationListViewModel.updateSearchQuery("")
                    nowPlayingViewModel.hide()
                },
            )

            playerState.pendingMobileDataStationId?.let { stationId ->
                val stationName = StationRegistry.getStation(stationId)?.name ?: stationId
                AlertDialog(
                    onDismissRequest = playerViewModel::dismissMobileDataPlaybackPrompt,
                    title = {
                        Text(
                            text = strings.mobileDataPromptTitle,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    },
                    text = {
                        Text(
                            text = strings.mobileDataPromptBody(stationName),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                        )
                    },
                    confirmButton = {
                        TextButton(onClick = playerViewModel::confirmMobileDataPlayback) {
                            Text(text = strings.continuePlayback, color = radikoPalette().accent)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = playerViewModel::dismissMobileDataPlaybackPrompt) {
                            Text(
                                text = strings.cancel,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                            )
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.surface,
                )
            }

            // 流量确认框优先；它关掉以后再提醒更新，两个弹窗不叠在一起。
            val update = pendingUpdate
            if (update != null && playerState.pendingMobileDataStationId == null) {
                UpdatePromptDialog(
                    update = update,
                    currentVersion = PlatformRuntimeInfo.versionName,
                    language = settingsState.language,
                    onDownload = appUpdateChecker::download,
                    onLater = appUpdateChecker::snooze,
                    onDismiss = appUpdateChecker::dismiss,
                    onOpenReleasePage = appUpdateChecker::openReleasePage,
                )
            }
        }
    }
}

@Composable
private fun StationSectionHeader(
    title: String,
    count: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.56f),
        )
    }
}

private fun LazyGridState.isScrolledToBottom(): Boolean {
    val layoutInfo = layoutInfo
    val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull() ?: return false
    val isLastItemVisible = lastVisibleItem.index >= layoutInfo.totalItemsCount - 1
    val viewportBottom = layoutInfo.viewportEndOffset
    val itemBottom = lastVisibleItem.offset.y + lastVisibleItem.size.height
    return isLastItemVisible && itemBottom <= viewportBottom + 8
}
