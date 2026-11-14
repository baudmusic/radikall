package com.radiko.desktop

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.radiko.i18n.appStrings
import com.radiko.ui.theme.radikoPalette
import java.util.prefs.Preferences

enum class DesktopCloseBehavior {
    ASK,
    MINIMIZE_TO_TRAY,
    EXIT,
}

class DesktopWindowPreferences {
    private val preferences = Preferences.userRoot().node("com.radiko.radikall.desktop")

    fun closeBehavior(): DesktopCloseBehavior {
        val stored = preferences.get(KEY_CLOSE_BEHAVIOR, DesktopCloseBehavior.ASK.name)
        return DesktopCloseBehavior.entries.firstOrNull { it.name == stored } ?: DesktopCloseBehavior.ASK
    }

    fun saveCloseBehavior(behavior: DesktopCloseBehavior) {
        preferences.put(KEY_CLOSE_BEHAVIOR, behavior.name)
    }

    private companion object {
        const val KEY_CLOSE_BEHAVIOR = "close_behavior"
    }
}

@Composable
fun CloseToTrayPrompt(
    rememberChoice: Boolean,
    onRememberChoiceChange: (Boolean) -> Unit,
    onMinimizeToTray: () -> Unit,
    onExit: () -> Unit,
    onCancel: () -> Unit,
) {
    val strings = appStrings()
    val palette = radikoPalette()
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.scrim),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.44f)
                .clip(RoundedCornerShape(30.dp))
                .background(palette.surface)
                .border(
                    width = 1.dp,
                    color = palette.line,
                    shape = RoundedCornerShape(30.dp),
                )
                .padding(horizontal = 28.dp, vertical = 24.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .background(
                                color = palette.infoSoft,
                                shape = CircleShape,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = strings.trayIconLabel,
                            style = MaterialTheme.typography.labelLarge,
                            color = palette.info,
                            fontWeight = FontWeight.Bold,
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = strings.minimizeToTrayPromptTitle,
                            style = MaterialTheme.typography.headlineSmall,
                            color = palette.ink,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = strings.minimizeToTrayPromptBody,
                            style = MaterialTheme.typography.bodyMedium,
                            color = palette.ink2,
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(palette.surfaceSunken)
                        .clickable(onClick = { onRememberChoiceChange(!rememberChoice) })
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        checked = rememberChoice,
                        onCheckedChange = onRememberChoiceChange,
                        colors = CheckboxDefaults.colors(
                            checkedColor = palette.accent,
                            checkmarkColor = palette.onAccent,
                            uncheckedColor = palette.ink3,
                        ),
                    )
                    Text(
                        text = strings.doNotShowAgain,
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.ink,
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                ) {
                    PromptActionButton(
                        text = strings.yes,
                        modifier = Modifier.width(116.dp),
                        emphasis = PromptActionEmphasis.Primary,
                        onClick = onMinimizeToTray,
                    )
                    PromptActionButton(
                        text = strings.no,
                        modifier = Modifier.width(116.dp),
                        emphasis = PromptActionEmphasis.Secondary,
                        onClick = onExit,
                    )
                    PromptActionButton(
                        text = strings.cancel,
                        modifier = Modifier.width(116.dp),
                        emphasis = PromptActionEmphasis.Ghost,
                        onClick = onCancel,
                    )
                }
            }
        }
    }
}

private enum class PromptActionEmphasis {
    Primary,
    Secondary,
    Ghost,
}

@Composable
private fun PromptActionButton(
    text: String,
    modifier: Modifier = Modifier,
    emphasis: PromptActionEmphasis,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    val pressed by interactionSource.collectIsPressedAsState()
    val palette = radikoPalette()

    val scale by animateFloatAsState(
        targetValue = when {
            pressed -> 0.985f
            hovered -> 1.02f
            else -> 1f
        },
        label = "prompt_action_scale",
    )
    val containerColor by animateColorAsState(
        targetValue = when (emphasis) {
            PromptActionEmphasis.Primary -> when {
                pressed -> palette.stage.copy(alpha = 0.9f)
                else -> palette.stage
            }
            PromptActionEmphasis.Secondary -> when {
                pressed || hovered -> palette.surfaceSunken
                else -> palette.surface
            }
            PromptActionEmphasis.Ghost -> when {
                pressed || hovered -> palette.surfaceSunken
                else -> Color.Transparent
            }
        },
        label = "prompt_action_container",
    )
    val borderColor by animateColorAsState(
        targetValue = when (emphasis) {
            PromptActionEmphasis.Primary -> Color.Transparent
            PromptActionEmphasis.Secondary -> palette.lineStrong
            PromptActionEmphasis.Ghost -> if (hovered) palette.line else Color.Transparent
        },
        label = "prompt_action_border",
    )
    val textColor by animateColorAsState(
        targetValue = when (emphasis) {
            PromptActionEmphasis.Primary -> palette.onStage
            PromptActionEmphasis.Secondary -> palette.info
            PromptActionEmphasis.Ghost -> palette.ink2
        },
        label = "prompt_action_text",
    )

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(18.dp))
            .hoverable(interactionSource = interactionSource)
            .background(containerColor)
            .border(
                width = 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(18.dp),
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 18.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            color = textColor,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
