package com.radiko.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.radiko.i18n.appStrings
import com.radiko.ui.theme.radikoPalette
import com.radiko.ui.theme.radikoPrimaryTextColor

@Composable
fun NowPlayingTopBar(
    stationName: String,
    stationLogoUrl: String?,
    onDismiss: () -> Unit,
) {
    val strings = appStrings()
    val barInteraction = remember { MutableInteractionSource() }
    val barHovered by barInteraction.collectIsHoveredAsState()
    val closeInteraction = remember { MutableInteractionSource() }
    val closeHovered by closeInteraction.collectIsHoveredAsState()
    val logoInteraction = remember { MutableInteractionSource() }
    val logoHovered by logoInteraction.collectIsHoveredAsState()
    val palette = radikoPalette()

    val barOnlyHovered = barHovered && !closeHovered && !logoHovered
    val barScale by animateFloatAsState(
        targetValue = if (barOnlyHovered) 1.004f else 1f,
        label = "top_bar_scale",
    )
    val barGlowElevation by animateDpAsState(
        targetValue = if (barOnlyHovered) 10.dp else 0.dp,
        label = "top_bar_glow_elevation",
    )
    val barContainerColor = palette.panel
    val barBorderColor by animateColorAsState(
        targetValue = if (barOnlyHovered) palette.lineStrong else palette.line,
        label = "top_bar_border",
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .width(48.dp)
                    .height(4.dp)
                    .background(
                        color = palette.onStage.copy(alpha = 0.34f),
                        shape = RoundedCornerShape(999.dp),
                    ),
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    scaleX = barScale
                    scaleY = barScale
                }
                .edgeSwipeAction(
                    enabled = true,
                    direction = EdgeSwipeDirection.Down,
                    canTrigger = { true },
                    onTrigger = onDismiss,
                )
                .hoverable(interactionSource = barInteraction)
                .shadow(
                    elevation = barGlowElevation,
                    shape = RoundedCornerShape(30.dp),
                    ambientColor = palette.shadow,
                    spotColor = palette.shadow,
                )
                .background(
                    color = barContainerColor,
                    shape = RoundedCornerShape(30.dp),
                )
                .border(
                    width = 1.dp,
                    color = barBorderColor,
                    shape = RoundedCornerShape(30.dp),
                )
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CollapseButton(
                interactionSource = closeInteraction,
                onClick = onDismiss,
            )
            Spacer(modifier = Modifier.width(14.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                // 「再生中」做成韓紅花底的小徽章（像电台的 ON AIR 灯）：红字直接压在面板上，深色模式下看不清。
                Text(
                    text = strings.nowPlayingTitle,
                    style = MaterialTheme.typography.labelSmall.copy(
                        letterSpacing = 0.8.sp,
                    ),
                    color = palette.onAccent,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .background(color = palette.accent, shape = RoundedCornerShape(999.dp))
                        .padding(horizontal = 9.dp, vertical = 2.dp),
                )
                Text(
                    text = stationName.ifBlank { strings.unknownStationFallback },
                    style = MaterialTheme.typography.titleMedium,
                    color = radikoPrimaryTextColor(),
                    fontWeight = FontWeight.Bold,
                )
            }

            StationLogoChip(
                logoUrl = stationLogoUrl,
                interactionSource = logoInteraction,
            )
        }
    }
}

@Composable
private fun StationLogoChip(
    logoUrl: String?,
    interactionSource: MutableInteractionSource,
) {
    val hovered by interactionSource.collectIsHoveredAsState()
    val scale by animateFloatAsState(
        targetValue = if (hovered) 1.028f else 1f,
        label = "top_bar_logo_scale",
    )
    val glowElevation by animateDpAsState(
        targetValue = if (hovered) 12.dp else 0.dp,
        label = "top_bar_logo_glow_elevation",
    )
    val palette = radikoPalette()
    val containerColor = palette.plate
    val borderColor by animateColorAsState(
        targetValue = if (hovered) palette.lineStrong else palette.line,
        label = "top_bar_logo_border",
    )

    Box(
        modifier = Modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .hoverable(interactionSource = interactionSource)
            .width(124.dp)
            .height(52.dp)
            .shadow(
                elevation = glowElevation,
                shape = RoundedCornerShape(18.dp),
                ambientColor = palette.shadow,
                spotColor = palette.shadow,
            )
            .background(
                color = containerColor,
                shape = RoundedCornerShape(18.dp),
            )
            .border(
                width = 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(18.dp),
            )
            .padding(horizontal = 12.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (!logoUrl.isNullOrBlank()) {
            AsyncImage(
                model = logoUrl,
                contentDescription = "station logo",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun CollapseButton(
    interactionSource: MutableInteractionSource,
    onClick: () -> Unit,
) {
    val hovered by interactionSource.collectIsHoveredAsState()
    val scale by animateFloatAsState(
        targetValue = if (hovered) 1.04f else 1f,
        label = "top_bar_close_scale",
    )
    val glowElevation by animateDpAsState(
        targetValue = if (hovered) 14.dp else 0.dp,
        label = "top_bar_close_glow_elevation",
    )
    val palette = radikoPalette()
    val containerColor = palette.surfaceSunken
    val borderColor by animateColorAsState(
        targetValue = if (hovered) palette.lineStrong else palette.line,
        label = "top_bar_close_border",
    )

    Box(
        modifier = Modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .hoverable(interactionSource = interactionSource)
            .size(52.dp)
            .shadow(
                elevation = glowElevation,
                shape = CircleShape,
                ambientColor = palette.shadow,
                spotColor = palette.shadow,
            )
            .background(
                color = containerColor,
                shape = CircleShape,
            )
            .border(
                width = 1.dp,
                color = borderColor,
                shape = CircleShape,
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(width = 20.dp, height = 12.dp)) {
            val strokeWidth = 3.8.dp.toPx()
            drawLine(
                color = palette.ink2,
                start = Offset(0f, 1.dp.toPx()),
                end = Offset(size.width / 2f, size.height),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round,
            )
            drawLine(
                color = palette.ink2,
                start = Offset(size.width, 1.dp.toPx()),
                end = Offset(size.width / 2f, size.height),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round,
            )
        }
    }
}
