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
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.radiko.ui.theme.RadikoColors

@Composable
fun GlowActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    fillMaxWidth: Boolean = false,
    contentPadding: PaddingValues = PaddingValues(horizontal = 24.dp, vertical = 14.dp),
) {
    val shape = RoundedCornerShape(999.dp)
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    val pressed by interactionSource.collectIsPressedAsState()

    val highlighted = enabled && (hovered || pressed)
    val scale by animateFloatAsState(
        targetValue = when {
            pressed -> 0.985f
            highlighted -> 1.025f
            else -> 1f
        },
        label = "glow_action_button_scale",
    )
    val glowElevation by animateDpAsState(
        targetValue = if (!enabled) 0.dp else if (highlighted) 16.dp else 0.dp,
        label = "glow_action_button_elevation",
    )
    val borderColor by animateColorAsState(
        targetValue = when {
            !enabled -> RadikoColors.White.copy(alpha = 0.08f)
            highlighted -> RadikoColors.White.copy(alpha = 0.34f)
            else -> RadikoColors.White.copy(alpha = 0.18f)
        },
        label = "glow_action_button_border",
    )
    val containerColor by animateColorAsState(
        targetValue = when {
            !enabled -> RadikoColors.White.copy(alpha = 0.7f)
            pressed -> RadikoColors.White.copy(alpha = 0.92f)
            else -> RadikoColors.White
        },
        label = "glow_action_button_container",
    )
    val textColor by animateColorAsState(
        targetValue = if (enabled) RadikoColors.NowPlayingBlue else RadikoColors.NowPlayingBlue.copy(alpha = 0.62f),
        label = "glow_action_button_text",
    )

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .shadow(
                elevation = glowElevation,
                shape = shape,
                ambientColor = RadikoColors.White.copy(alpha = 0.38f),
                spotColor = RadikoColors.White.copy(alpha = 0.38f),
            )
            .then(if (fillMaxWidth) Modifier.fillMaxWidth() else Modifier)
            .hoverable(interactionSource = interactionSource)
            .background(color = containerColor, shape = shape)
            .border(width = 1.dp, color = borderColor, shape = shape)
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(contentPadding),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            color = textColor,
            fontWeight = FontWeight.Bold,
        )
    }
}
