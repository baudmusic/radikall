package com.radiko.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.rememberAsyncImagePainter
import com.radiko.i18n.AppStrings
import com.radiko.i18n.appStrings
import com.radiko.station.ProgramEntry
import com.radiko.ui.theme.RadikoColors
import com.radiko.ui.theme.radikoPanelBorderColor
import com.radiko.ui.theme.radikoPanelColor
import com.radiko.ui.theme.radikoPrimaryTextColor
import com.radiko.ui.theme.radikoSecondaryTextColor

@Composable
fun NowPlayingProgramInfo(
    program: ProgramEntry?,
    stationId: String,
    isPlaying: Boolean,
    isLoading: Boolean,
    onTogglePlayback: () -> Unit,
) {
    val fallbackImageUrl = stationId
        .takeIf { it.isNotBlank() }
        ?.let { "https://radiko.jp/v2/static/station/logo/$it/224x100.png" }
    val strings = appStrings()

    val imageModel = program?.imageUrl ?: fallbackImageUrl

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = radikoPanelColor(),
                shape = RoundedCornerShape(24.dp),
            )
            .border(
                width = 1.dp,
                color = radikoPanelBorderColor(),
                shape = RoundedCornerShape(24.dp),
            )
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Text(
            text = program?.title ?: strings.programTitleUnavailable,
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = radikoPrimaryTextColor(),
        )

        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val wideLayout = maxWidth >= 860.dp
            if (wideLayout) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    ProgramHeroImage(
                        imageModel = imageModel,
                        modifier = Modifier.weight(1.25f),
                    )
                    ProgramMetaPanel(
                        program = program,
                        strings = strings,
                        isPlaying = isPlaying,
                        isLoading = isLoading,
                        onTogglePlayback = onTogglePlayback,
                        modifier = Modifier.weight(1f),
                    )
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    ProgramHeroImage(
                        imageModel = imageModel,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    ProgramMetaPanel(
                        program = program,
                        strings = strings,
                        isPlaying = isPlaying,
                        isLoading = isLoading,
                        onTogglePlayback = onTogglePlayback,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
private fun ProgramHeroImage(
    imageModel: Any?,
    modifier: Modifier = Modifier,
) {
    val painter = rememberAsyncImagePainter(model = imageModel)
    val aspectRatio = painter.intrinsicSize.toAspectRatioOrDefault(default = 1.6f)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(radikoPanelColor())
            .border(
                width = 1.dp,
                color = radikoPanelBorderColor(),
                shape = RoundedCornerShape(18.dp),
            ),
    ) {
        Image(
            painter = painter,
            contentDescription = "Program artwork",
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(aspectRatio)
                .heightIn(min = 200.dp, max = 360.dp),
            contentScale = ContentScale.Fit,
        )
    }
}

@Composable
private fun ProgramMetaPanel(
    program: ProgramEntry?,
    strings: AppStrings,
    isPlaying: Boolean,
    isLoading: Boolean,
    onTogglePlayback: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.widthIn(min = 220.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        program?.performer?.takeIf { it.isNotBlank() }?.let { performer ->
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = strings.performerLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = radikoSecondaryTextColor(),
                )
                Text(
                    text = performer,
                    style = MaterialTheme.typography.titleMedium,
                    color = radikoPrimaryTextColor(),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        program?.let {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = strings.onAirLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = radikoSecondaryTextColor(),
                )
                Text(
                    text = formatDateTimeRange(it.startAt, it.endAt, strings),
                    style = MaterialTheme.typography.titleMedium,
                    color = radikoPrimaryTextColor(),
                )
            }
        }

        GlowActionButton(
            text = when {
                isLoading -> strings.loading
                isPlaying -> strings.stop
                else -> strings.play
            },
            onClick = onTogglePlayback,
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth(),
            fillMaxWidth = true,
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                horizontal = 28.dp,
                vertical = 16.dp,
            ),
        )
    }
}

private fun Size.toAspectRatioOrDefault(default: Float): Float {
    return if (isSpecified && width > 0f && height > 0f) width / height else default
}

fun formatTimeRange(startAt: String, endAt: String): String {
    fun format(value: String): String {
        if (value.length < 12) {
            return value
        }
        return "${value.substring(8, 10)}:${value.substring(10, 12)}"
    }

    return "${format(startAt)} - ${format(endAt)}"
}

fun formatDateTimeRange(startAt: String, endAt: String, strings: AppStrings): String {
    if (startAt.length < 12 || endAt.length < 12) {
        return formatTimeRange(startAt, endAt)
    }

    val month = startAt.substring(4, 6).trimStart('0')
    val day = startAt.substring(6, 8).trimStart('0')
    return "$month/$day ${formatTimeRange(startAt, endAt)}"
}
