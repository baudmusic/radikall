package com.radiko.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.radiko.i18n.AppStrings
import com.radiko.i18n.appStrings
import com.radiko.station.ProgramEntry
import com.radiko.ui.theme.RadikoColors
import com.radiko.ui.theme.radikoPanelColor
import com.radiko.ui.theme.radikoPrimaryTextColor
import com.radiko.ui.theme.radikoSecondaryTextColor
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.coroutines.flow.first

@Composable
fun WeeklySchedule(
    programs: List<ProgramEntry>,
    weeklyPrograms: List<ProgramEntry>,
    selectedDayIndex: Int,
    currentProgram: ProgramEntry?,
    isLoading: Boolean,
    onSelectDay: (Int) -> Unit,
) {
    val strings = appStrings()
    val useDarkThemeStyling = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val listState = rememberLazyListState()
    val currentProgramIndex = programs.indexOfFirst { program ->
        currentProgram != null &&
            currentProgram.startAt == program.startAt &&
            currentProgram.stationId == program.stationId
    }
    var hasAutoScrolledToCurrentProgram by remember(
        selectedDayIndex,
        currentProgram?.stationId,
        currentProgram?.startAt,
        programs,
    ) {
        mutableStateOf(false)
    }

    LaunchedEffect(selectedDayIndex, currentProgram?.startAt, currentProgramIndex, programs, isLoading) {
        if (isLoading || programs.isEmpty()) {
            return@LaunchedEffect
        }

        if (currentProgramIndex >= 0 && !hasAutoScrolledToCurrentProgram) {
            snapshotFlow { listState.layoutInfo.totalItemsCount }
                .first { totalItems -> totalItems > currentProgramIndex }
            listState.scrollToItem((currentProgramIndex - 1).coerceAtLeast(0))
            hasAutoScrolledToCurrentProgram = true
        } else if (currentProgramIndex < 0) {
            listState.scrollToItem(0)
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            text = strings.weeklyScheduleTitle,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = RadikoColors.White,
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = radikoPanelColor(),
                    shape = RoundedCornerShape(24.dp),
                )
                .padding(24.dp),
        ) {
            val days = weeklyPrograms.map { it.startAt.take(8) }.distinct().sorted()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                days.forEachIndexed { index, dateStr ->
                    GlowChip(
                        text = formatDateLabel(dateStr, strings),
                        selected = index == selectedDayIndex,
                        onClick = { onSelectDay(index) },
                        selectedContainerColor = RadikoColors.ScheduleHighlight,
                        selectedContentColor = RadikoColors.White,
                        containerColor = if (useDarkThemeStyling) {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.92f)
                        } else {
                            RadikoColors.White.copy(alpha = 0.92f)
                        },
                        contentColor = if (useDarkThemeStyling) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            radikoPrimaryTextColor()
                        },
                        glowColor = RadikoColors.ScheduleHighlight,
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            when {
                isLoading -> {
                    Text(
                        text = strings.loading,
                        style = MaterialTheme.typography.bodyMedium,
                        color = radikoSecondaryTextColor(0.65f),
                    )
                }

                programs.isEmpty() -> {
                    Text(
                        text = strings.noScheduleAvailable,
                        style = MaterialTheme.typography.bodyMedium,
                        color = radikoSecondaryTextColor(0.65f),
                    )
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 460.dp),
                        state = listState,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        itemsIndexed(
                            items = programs,
                            key = { _, program -> "${program.stationId}-${program.startAt}" },
                        ) { _, program ->
                            val isCurrent = currentProgram?.startAt == program.startAt &&
                                currentProgram.stationId == program.stationId
                            val isPast = currentProgram != null &&
                                program.startAt.take(8) == currentProgram.startAt.take(8) &&
                                program.endAt <= currentProgram.startAt

                            ScheduleProgramItem(
                                program = program,
                                isCurrent = isCurrent,
                                isPast = isPast,
                                modifier = Modifier,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScheduleProgramItem(
    program: ProgramEntry,
    isCurrent: Boolean,
    isPast: Boolean,
    modifier: Modifier = Modifier,
) {
    val useDarkThemeStyling = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val backgroundColor = when {
        isCurrent -> RadikoColors.ScheduleHighlight
        isPast -> if (useDarkThemeStyling) {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.98f)
        } else {
            RadikoColors.AccentRed.copy(alpha = 0.06f)
        }
        else -> if (useDarkThemeStyling) {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.92f)
        } else {
            RadikoColors.White.copy(alpha = 0.88f)
        }
    }
    val textColor = when {
        isCurrent -> RadikoColors.White
        isPast -> RadikoColors.PastProgramRed
        else -> radikoPrimaryTextColor()
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            text = formatTimeRange(program.startAt, program.endAt),
            style = MaterialTheme.typography.bodyMedium,
            color = textColor,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.width(110.dp),
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = program.title,
                style = MaterialTheme.typography.titleMedium,
                color = textColor,
                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            program.performer?.takeIf { it.isNotBlank() }?.let { performer ->
                Text(
                    text = performer,
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor.copy(alpha = 0.78f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

fun formatDateLabel(dateStr: String, strings: AppStrings): String {
    if (dateStr.length < 8) {
        return dateStr
    }

    val year = dateStr.substring(0, 4).toIntOrNull() ?: return dateStr
    val month = dateStr.substring(4, 6).toIntOrNull() ?: return dateStr
    val day = dateStr.substring(6, 8).toIntOrNull() ?: return dateStr
    val weekday = strings.weekdayShort(LocalDate(year, month, day).dayOfWeek)
    return "$month/$day($weekday)"
}
