package com.radiko.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.radiko.platform.PlatformEnvironment
import com.radiko.ui.theme.RadikoColors
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun TopBarActionButton(
    onClick: () -> Unit,
    drawIcon: DrawScope.(iconColor: Color) -> Unit,
    modifier: Modifier = Modifier,
    buttonSize: Dp = 40.dp,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    val pressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = when {
            pressed -> 0.88f
            hovered -> 1.08f
            else -> 1f
        },
        label = "topbar_action_button_scale",
    )
    val backgroundColor by animateColorAsState(
        targetValue = if (hovered) RadikoColors.AccentRed else Color.Transparent,
        label = "topbar_action_button_background",
    )
    val iconColor by animateColorAsState(
        targetValue = if (hovered) Color.White else RadikoColors.AccentRed,
        label = "topbar_action_button_icon",
    )
    val hoverModifier = if (PlatformEnvironment.supportsPointerHover) {
        Modifier.hoverable(interactionSource = interactionSource)
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .size(buttonSize)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .then(hoverModifier)
            .clip(CircleShape)
            .background(backgroundColor)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(buttonSize)) {
            drawIcon(iconColor)
        }
    }
}

fun DrawScope.drawSettingsIcon(color: Color) {
    val center = Offset(size.width / 2f, size.height / 2f)
    val ringRadius = size.minDimension * 0.22f
    val innerRadius = size.minDimension * 0.09f
    val toothStart = size.minDimension * 0.22f
    val toothEnd = size.minDimension * 0.34f
    val strokeWidth = size.minDimension * 0.075f

    repeat(8) { index ->
        val angleRadians = (index * 45f) * PI.toFloat() / 180f
        val cosAngle = cos(angleRadians)
        val sinAngle = sin(angleRadians)
        drawLine(
            color = color,
            start = Offset(
                x = center.x + toothStart * cosAngle,
                y = center.y + toothStart * sinAngle,
            ),
            end = Offset(
                x = center.x + toothEnd * cosAngle,
                y = center.y + toothEnd * sinAngle,
            ),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round,
        )
    }

    drawCircle(
        color = color,
        radius = ringRadius,
        style = Stroke(width = strokeWidth),
    )
    drawCircle(
        color = color,
        radius = innerRadius,
        style = Stroke(width = strokeWidth),
    )
}

fun DrawScope.drawBackArrowIcon(color: Color) {
    val strokeWidth = size.minDimension * 0.1f
    drawLine(
        color = color,
        start = Offset(size.width * 0.72f, size.height * 0.2f),
        end = Offset(size.width * 0.28f, size.height * 0.5f),
        strokeWidth = strokeWidth,
        cap = StrokeCap.Round,
    )
    drawLine(
        color = color,
        start = Offset(size.width * 0.28f, size.height * 0.5f),
        end = Offset(size.width * 0.72f, size.height * 0.8f),
        strokeWidth = strokeWidth,
        cap = StrokeCap.Round,
    )
}
