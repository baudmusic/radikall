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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.radiko.i18n.appStrings
import com.radiko.ui.components.TopBarActionButton
import com.radiko.ui.components.drawSettingsIcon
import com.radiko.ui.theme.RadikoColors
import com.radiko.ui.viewmodel.RadikoAppGraph
import com.radiko.station.StationRegistry
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
    val stationGridState = rememberLazyGridState()
    val strings = appStrings()
    val drivingModeEnabled = settingsState.drivingModeEnabled

    LaunchedEffect(playerState.currentAreaId) {
        stationListViewModel.switchArea(playerState.currentAreaId)
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
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.66f),
                                )
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.46f),
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.62f),
                                unfocusedBorderColor = Color.Transparent,
                                focusedBorderColor = RadikoColors.PrimaryBlue.copy(alpha = 0.3f),
                                cursorColor = RadikoColors.PrimaryBlue,
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
                    color = RadikoColors.BorderLight,
                )

                Box(modifier = Modifier.weight(1f)) {
                    if (stationState.stations.isEmpty()) {
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
                            columns = GridCells.Adaptive(minSize = if (drivingModeEnabled) 280.dp else 220.dp),
                            contentPadding = PaddingValues(if (drivingModeEnabled) 28.dp else 24.dp),
                            horizontalArrangement = Arrangement.spacedBy(if (drivingModeEnabled) 22.dp else 18.dp),
                            verticalArrangement = Arrangement.spacedBy(if (drivingModeEnabled) 22.dp else 18.dp),
                        ) {
                            items(stationState.stations, key = { it.id }) { station ->
                                StationCard(
                                    station = station,
                                    isActive = playerState.currentStation?.id == station.id,
                                    onClick = { playerViewModel.playStation(station.id) },
                                    emphasized = drivingModeEnabled,
                                )
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
                            Text(text = strings.continuePlayback, color = RadikoColors.AccentRed)
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
        }
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
