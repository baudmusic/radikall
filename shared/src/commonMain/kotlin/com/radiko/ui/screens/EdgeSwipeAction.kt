package com.radiko.ui.screens

import androidx.compose.runtime.remember
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.abs

enum class EdgeSwipeDirection {
    Up,
    Down,
}

fun Modifier.edgeSwipeAction(
    enabled: Boolean,
    direction: EdgeSwipeDirection,
    canTrigger: () -> Boolean,
    onTrigger: () -> Unit,
    threshold: Dp = 72.dp,
): Modifier = composed {
    val density = LocalDensity.current
    val thresholdPx = remember(density, threshold) { with(density) { threshold.toPx() } }

    pointerInput(enabled, direction, thresholdPx, canTrigger, onTrigger) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            var pointerId = down.id
            var lastY = down.position.y
            var accumulatedDrag = 0f
            var hasTriggered = false

            while (true) {
                val event = awaitPointerEvent(pass = PointerEventPass.Final)
                val change = event.changes.firstOrNull { it.id == pointerId }
                    ?: event.changes.firstOrNull()
                    ?: break

                pointerId = change.id
                val currentY = change.position.y
                val deltaY = currentY - lastY
                lastY = currentY

                if (!change.pressed) {
                    break
                }

                if (!enabled || !canTrigger() || hasTriggered) {
                    accumulatedDrag = 0f
                    continue
                }

                val directionalDelta = when (direction) {
                    EdgeSwipeDirection.Up -> -deltaY
                    EdgeSwipeDirection.Down -> deltaY
                }

                if (directionalDelta > 0f) {
                    accumulatedDrag += directionalDelta
                    if (accumulatedDrag >= thresholdPx) {
                        hasTriggered = true
                        onTrigger()
                    }
                } else if (abs(deltaY) > 0f) {
                    accumulatedDrag = 0f
                }
            }
        }
    }
}
