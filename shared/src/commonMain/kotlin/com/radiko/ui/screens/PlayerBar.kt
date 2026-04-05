package com.radiko.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.radiko.i18n.appStrings
import com.radiko.ui.theme.RadikoColors
import com.radiko.ui.viewmodel.PlayerState

@Composable
fun PlayerBar(
    playerState: PlayerState,
    onTogglePlayback: () -> Unit,
    onClick: () -> Unit,
    onSwipeUp: (() -> Unit)? = null,
    drivingModeEnabled: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val strings = appStrings()
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        shadowElevation = 4.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            RadikoColors.PlayerBarGradientStart,
                            RadikoColors.PlayerBarGradientEnd,
                        ),
                    ),
                )
                .drawBehind {
                    drawLine(
                        color = Color.White.copy(alpha = 0.1f),
                        start = Offset(0f, 0f),
                        end = Offset(size.width, 0f),
                        strokeWidth = 0.5.dp.toPx(),
                    )
                }
                .edgeSwipeAction(
                    enabled = onSwipeUp != null && playerState.currentStation != null,
                    direction = EdgeSwipeDirection.Up,
                    canTrigger = { true },
                    onTrigger = { onSwipeUp?.invoke() },
                )
                .clickable(enabled = playerState.currentStation != null) { onClick() }
                .padding(
                    horizontal = if (drivingModeEnabled) 26.dp else 22.dp,
                    vertical = if (drivingModeEnabled) 18.dp else 14.dp,
                ),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = playerState.currentStation?.name ?: strings.playerBarIdleTitle,
                    style = if (drivingModeEnabled) {
                        MaterialTheme.typography.titleLarge
                    } else {
                        MaterialTheme.typography.titleMedium
                    },
                    color = RadikoColors.White,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                ScrollingText(
                    text = playerState.currentProgram?.title
                        ?: strings.playerBarIdleSubtitle,
                    style = if (drivingModeEnabled) {
                        MaterialTheme.typography.titleMedium
                    } else {
                        MaterialTheme.typography.bodyMedium
                    },
                    color = RadikoColors.White.copy(alpha = 0.88f),
                )
            }

            GlowActionButton(
                text = when {
                    playerState.isLoading -> strings.loading
                    playerState.isPlaying -> strings.stop
                    else -> strings.play
                },
                enabled = playerState.currentStation != null && !playerState.isLoading,
                onClick = onTogglePlayback,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    horizontal = if (drivingModeEnabled) 30.dp else 24.dp,
                    vertical = if (drivingModeEnabled) 16.dp else 12.dp,
                ),
            )
        }
    }
}
