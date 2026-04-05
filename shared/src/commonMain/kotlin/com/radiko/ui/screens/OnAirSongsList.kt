package com.radiko.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.radiko.i18n.appStrings
import com.radiko.station.OnAirSong
import com.radiko.station.ProgramEntry
import com.radiko.ui.theme.RadikoColors
import com.radiko.ui.theme.radikoPanelColor
import com.radiko.ui.theme.radikoPrimaryTextColor
import com.radiko.ui.theme.radikoSecondaryTextColor
import com.radiko.ui.viewmodel.OnAirHistoryMode

@Composable
fun CurrentOnAirSongCard(
    song: OnAirSong?,
    isLoading: Boolean,
    modifier: Modifier = Modifier,
) {
    val strings = appStrings()
    Column(
        modifier = modifier
            .background(
                color = radikoPanelColor(),
                shape = RoundedCornerShape(24.dp),
            )
            .border(
                width = 1.dp,
                color = RadikoColors.PrimaryBlue.copy(alpha = 0.16f),
                shape = RoundedCornerShape(24.dp),
            )
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = strings.currentOnAirSongTitle,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = radikoPrimaryTextColor(),
        )

        when {
            isLoading -> {
                Text(
                    text = strings.loading,
                    style = MaterialTheme.typography.bodyMedium,
                    color = radikoSecondaryTextColor(0.78f),
                )
            }

            song == null -> {
                Text(
                    text = strings.currentOnAirSongUnavailable,
                    style = MaterialTheme.typography.bodyMedium,
                    color = radikoSecondaryTextColor(0.78f),
                )
            }

            else -> {
                SongCardContent(
                    song = song,
                    compact = false,
                    onLightSurface = true,
                    artworkSize = 108.dp,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OnAirSongsList(
    songs: List<OnAirSong>,
    currentProgram: ProgramEntry?,
    historyMode: OnAirHistoryMode,
    isLoading: Boolean,
    showAll: Boolean,
    onHistoryModeChange: (OnAirHistoryMode) -> Unit,
    onToggleShowAll: () -> Unit,
) {
    val strings = appStrings()
    val currentSong = songs.firstOrNull()
    val baseSongs = when (historyMode) {
        OnAirHistoryMode.CURRENT_PROGRAM -> songs.filter { currentProgram == null || it.isInProgram(currentProgram) }
        OnAirHistoryMode.FULL_STATION -> songs
    }
    val historySongs = baseSongs.filterNot { it == currentSong }
    val visibleSongs = if (showAll) historySongs else historySongs.take(4)

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = strings.onAirHistoryTitle,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = RadikoColors.White,
            )
            Text(
                text = when (historyMode) {
                    OnAirHistoryMode.CURRENT_PROGRAM -> strings.currentProgramSongsDescription
                    OnAirHistoryMode.FULL_STATION -> strings.recentStationSongsDescription
                },
                style = MaterialTheme.typography.titleSmall,
                color = RadikoColors.White.copy(alpha = 0.82f),
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            GlowChip(
                text = strings.currentProgramLabel,
                selected = historyMode == OnAirHistoryMode.CURRENT_PROGRAM,
                onClick = { onHistoryModeChange(OnAirHistoryMode.CURRENT_PROGRAM) },
                selectedContainerColor = RadikoColors.White,
                selectedContentColor = RadikoColors.NowPlayingBlue,
                containerColor = RadikoColors.White.copy(alpha = 0.9f),
                contentColor = RadikoColors.DarkText,
                glowColor = RadikoColors.White,
            )
            GlowChip(
                text = strings.fullStationLabel,
                selected = historyMode == OnAirHistoryMode.FULL_STATION,
                onClick = { onHistoryModeChange(OnAirHistoryMode.FULL_STATION) },
                selectedContainerColor = RadikoColors.White,
                selectedContentColor = RadikoColors.NowPlayingBlue,
                containerColor = RadikoColors.White.copy(alpha = 0.9f),
                contentColor = RadikoColors.DarkText,
                glowColor = RadikoColors.White,
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = radikoPanelColor(),
                    shape = RoundedCornerShape(24.dp),
                )
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            when {
                isLoading -> {
                    Text(
                        text = strings.loading,
                        style = MaterialTheme.typography.bodyMedium,
                        color = radikoSecondaryTextColor(0.7f),
                    )
                }

                historySongs.isEmpty() -> {
                    Text(
                        text = when (historyMode) {
                            OnAirHistoryMode.CURRENT_PROGRAM -> strings.noSongsForCurrentProgram
                            OnAirHistoryMode.FULL_STATION -> strings.noRecentStationSongs
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = radikoSecondaryTextColor(0.7f),
                    )
                }

                else -> {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        visibleSongs.forEach { song ->
                            HistoricalSongCard(song = song)
                        }
                    }

                    if (historySongs.size > 4) {
                        TextButton(
                            onClick = onToggleShowAll,
                            modifier = Modifier.align(Alignment.End),
                        ) {
                            Text(
                                text = if (showAll) strings.collapse else strings.showMore,
                                color = RadikoColors.PrimaryBlue,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoricalSongCard(song: OnAirSong) {
    Box(
        modifier = Modifier
            .width(360.dp)
            .background(
                color = radikoPanelColor(),
                shape = RoundedCornerShape(16.dp),
            )
            .border(
                width = 1.dp,
                color = RadikoColors.PrimaryBlue.copy(alpha = 0.12f),
                shape = RoundedCornerShape(16.dp),
            )
            .padding(18.dp),
    ) {
        SongCardContent(
            song = song,
            compact = true,
            onLightSurface = true,
            artworkSize = 120.dp,
        )
    }
}

@Composable
private fun SongCardContent(
    song: OnAirSong,
    compact: Boolean,
    onLightSurface: Boolean,
    artworkSize: androidx.compose.ui.unit.Dp,
) {
    val textColor = if (onLightSurface) radikoPrimaryTextColor() else RadikoColors.White
    val secondaryColor = textColor.copy(alpha = 0.76f)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.Top,
    ) {
        SongArtwork(song = song, artworkSize = artworkSize)

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = song.artist,
                style = if (compact) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyLarge,
                color = secondaryColor,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = song.title,
                style = if (compact) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = textColor,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = formatSongDate(song.stampDate),
                style = MaterialTheme.typography.bodyMedium,
                color = secondaryColor,
            )
            Text(
                text = formatSongTime(song.stampDate),
                style = MaterialTheme.typography.bodyLarge,
                color = secondaryColor,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun SongArtwork(
    song: OnAirSong,
    artworkSize: androidx.compose.ui.unit.Dp,
) {
    if (song.imageUrl != null) {
        AsyncImage(
            model = song.imageUrl,
            contentDescription = song.title,
            modifier = Modifier
                .size(artworkSize)
                .clip(RoundedCornerShape(12.dp))
                .background(RadikoColors.White),
            contentScale = ContentScale.Fit,
        )
    } else {
        Box(
            modifier = Modifier
                .size(artworkSize)
                .clip(RoundedCornerShape(12.dp))
                .background(RadikoColors.White.copy(alpha = 0.18f)),
        )
    }
}

private fun formatSongDate(stampDate: String): String {
    if (stampDate.length < 10) {
        return stampDate
    }
    val year = stampDate.substring(0, 4)
    val month = stampDate.substring(5, 7).trimStart('0')
    val day = stampDate.substring(8, 10).trimStart('0')
    return "$year/$month/$day"
}

private fun formatSongTime(stampDate: String): String {
    if (stampDate.length < 16) {
        return ""
    }
    return stampDate.substring(11, 16)
}

private fun OnAirSong.isInProgram(program: ProgramEntry): Boolean {
    val compactStamp = stampDate.filter(Char::isDigit).take(14)
    return compactStamp >= program.startAt && compactStamp < program.endAt
}
