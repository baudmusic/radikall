package com.radiko.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.radiko.ui.theme.RadikoColors

@Composable
fun GlowChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    minWidth: Dp = 0.dp,
    selectedContainerColor: Color = RadikoColors.ScheduleHighlight,
    selectedContentColor: Color = RadikoColors.White,
    containerColor: Color = RadikoColors.White.copy(alpha = 0.94f),
    contentColor: Color = RadikoColors.DarkText,
    glowColor: Color = RadikoColors.NowPlayingBlue,
) {
    val shape = RoundedCornerShape(16.dp)
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()

    val scale by animateFloatAsState(
        targetValue = when {
            hovered -> 1.03f
            selected -> 1.015f
            else -> 1f
        },
        label = "glow_chip_scale",
    )
    val glowElevation by animateDpAsState(
        targetValue = if (hovered) 8.dp else 0.dp,
        label = "glow_chip_elevation",
    )
    val animatedContainerColor by animateColorAsState(
        targetValue = when {
            selected -> selectedContainerColor
            hovered -> containerColor.copy(alpha = 1f)
            else -> containerColor
        },
        label = "glow_chip_container",
    )
    val animatedContentColor by animateColorAsState(
        targetValue = if (selected) selectedContentColor else contentColor,
        label = "glow_chip_content",
    )
    val borderColor by animateColorAsState(
        targetValue = when {
            selected -> selectedContainerColor.copy(alpha = 0.96f)
            hovered -> glowColor.copy(alpha = 0.6f)
            else -> RadikoColors.White.copy(alpha = 0.22f)
        },
        label = "glow_chip_border",
    )

    Box(
        modifier = modifier
            .padding(4.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .shadow(
                elevation = glowElevation,
                shape = shape,
                ambientColor = glowColor.copy(alpha = 0.26f),
                spotColor = glowColor.copy(alpha = 0.38f),
            )
            .border(1.dp, borderColor, shape)
            .background(animatedContainerColor, shape)
            .hoverable(interactionSource = interactionSource)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .defaultMinSize(minWidth = minWidth)
            .padding(horizontal = 18.dp, vertical = 11.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = animatedContentColor,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
        )
    }
}
