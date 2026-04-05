package com.radiko.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.radiko.station.logoUrl
import com.radiko.ui.theme.RadikoColors
import com.radiko.ui.viewmodel.NowPlayingState
import com.radiko.ui.viewmodel.OnAirHistoryMode
import com.radiko.ui.viewmodel.PlayerState

@Composable
fun NowPlayingSheet(
    isVisible: Boolean,
    playerState: PlayerState,
    nowPlayingState: NowPlayingState,
    onDismiss: () -> Unit,
    onToggleShowAllSongs: () -> Unit,
    onHistoryModeChange: (OnAirHistoryMode) -> Unit,
    onSelectDay: (Int) -> Unit,
    onTogglePlayback: () -> Unit,
) {
    val currentSong = nowPlayingState.onAirSongs.firstOrNull()
    val scrollState = rememberScrollState()

    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(initialOffsetY = { it }),
        exit = slideOutVertically(targetOffsetY = { it }),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            RadikoColors.NowPlayingGradientTop,
                            RadikoColors.NowPlayingGradientMid,
                            RadikoColors.NowPlayingGradientBottom,
                        ),
                    ),
                ),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .edgeSwipeAction(
                        enabled = isVisible,
                        direction = EdgeSwipeDirection.Down,
                        canTrigger = { scrollState.value == 0 },
                        onTrigger = onDismiss,
                    )
                    .navigationBarsPadding()
                    .padding(bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 1240.dp)
                        .padding(horizontal = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(32.dp),
                ) {
                    NowPlayingTopBar(
                        stationName = playerState.currentStation?.name ?: "",
                        stationLogoUrl = playerState.currentStation?.logoUrl,
                        onDismiss = onDismiss,
                    )

                    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                        val wideLayout = maxWidth >= 980.dp
                        if (wideLayout) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(24.dp),
                                verticalAlignment = Alignment.Top,
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(20.dp),
                                ) {
                                    NowPlayingProgramInfo(
                                        program = playerState.currentProgram,
                                        stationId = playerState.currentStation?.id ?: "",
                                        isPlaying = playerState.isPlaying,
                                        isLoading = playerState.isLoading,
                                        onTogglePlayback = onTogglePlayback,
                                    )
                                    ProgramDescription(program = playerState.currentProgram)
                                }

                                CurrentOnAirSongCard(
                                    song = currentSong,
                                    isLoading = nowPlayingState.isLoadingSongs,
                                    modifier = Modifier.widthIn(min = 280.dp, max = 320.dp),
                                )
                            }
                        } else {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(20.dp),
                            ) {
                                NowPlayingProgramInfo(
                                    program = playerState.currentProgram,
                                    stationId = playerState.currentStation?.id ?: "",
                                    isPlaying = playerState.isPlaying,
                                    isLoading = playerState.isLoading,
                                    onTogglePlayback = onTogglePlayback,
                                )
                                CurrentOnAirSongCard(
                                    song = currentSong,
                                    isLoading = nowPlayingState.isLoadingSongs,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                ProgramDescription(program = playerState.currentProgram)
                            }
                        }
                    }

                    OnAirSongsList(
                        songs = nowPlayingState.onAirSongs,
                        currentProgram = playerState.currentProgram,
                        historyMode = nowPlayingState.historyMode,
                        isLoading = nowPlayingState.isLoadingSongs,
                        showAll = nowPlayingState.showAllSongs,
                        onHistoryModeChange = onHistoryModeChange,
                        onToggleShowAll = onToggleShowAllSongs,
                    )

                    WeeklySchedule(
                        programs = nowPlayingState.todayPrograms,
                        weeklyPrograms = nowPlayingState.weeklyPrograms,
                        selectedDayIndex = nowPlayingState.selectedDayIndex,
                        currentProgram = playerState.currentProgram,
                        isLoading = nowPlayingState.isLoadingSchedule,
                        onSelectDay = onSelectDay,
                    )
                }
            }
        }
    }
}
