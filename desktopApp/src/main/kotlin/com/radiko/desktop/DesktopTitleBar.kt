package com.radiko.desktop

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.radiko.ui.components.TopBarActionButton
import com.radiko.ui.components.drawSettingsIcon
import com.radiko.ui.theme.RadikoColors
import org.jetbrains.compose.resources.painterResource
import radiko_app.shared.generated.resources.Res
import radiko_app.shared.generated.resources.logo1
import java.awt.MouseInfo
import java.awt.Window

private val TitleBarHorizontalPadding = 30.dp
private val TitleBarVerticalPadding = 20.dp
private val TitleBarLogoHeight = 80.dp
private val TitleBarControlsSpacing = 15.dp
private val TitleBarSectionSpacing = 25.dp
private val TitleBarButtonSize = 25.dp

@Composable
fun DesktopTitleBar(
    window: Window,
    isMaximized: Boolean,
    onOpenSettings: () -> Unit,
    onMinimize: () -> Unit,
    onMaximizeRestore: () -> Unit,
    onClose: () -> Unit,
    areaContent: @Composable () -> Unit,
) {
    var dragAnchor by remember { mutableStateOf(Offset.Zero) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(RadikoColors.White)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = {
                        val mouse = MouseInfo.getPointerInfo().location
                        val win = window.location
                        dragAnchor = Offset(
                            x = (mouse.x - win.x).toFloat(),
                            y = (mouse.y - win.y).toFloat(),
                        )
                    },
                    onDrag = { _, _ ->
                        val mouse = MouseInfo.getPointerInfo().location
                        window.setLocation(
                            mouse.x - dragAnchor.x.toInt(),
                            mouse.y - dragAnchor.y.toInt(),
                        )
                    },
                )
            }
            .padding(horizontal = TitleBarHorizontalPadding, vertical = TitleBarVerticalPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Image(
            painter = painterResource(Res.drawable.logo1),
            contentDescription = "Radikall",
            contentScale = ContentScale.Fit,
            modifier = Modifier.height(TitleBarLogoHeight),
        )

        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(TitleBarSectionSpacing),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(TitleBarControlsSpacing),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TopBarActionButton(
                    onClick = onOpenSettings,
                    buttonSize = TitleBarButtonSize,
                    drawIcon = { iconColor -> drawSettingsIcon(iconColor) },
                )
                TopBarActionButton(
                    onClick = onMinimize,
                    buttonSize = TitleBarButtonSize,
                    drawIcon = { iconColor ->
                        drawLine(
                            color = iconColor,
                            start = Offset(size.width * 0.22f, size.height * 0.5f),
                            end = Offset(size.width * 0.78f, size.height * 0.5f),
                            strokeWidth = size.width * 0.115f,
                            cap = StrokeCap.Round,
                        )
                    },
                )
                TopBarActionButton(
                    onClick = onMaximizeRestore,
                    buttonSize = TitleBarButtonSize,
                    drawIcon = { iconColor ->
                        val strokeWidth = size.width * 0.105f
                        if (isMaximized) {
                            val offset = size.width * 0.14f
                            val side = size.width * 0.46f
                            drawRoundRect(
                                color = iconColor,
                                topLeft = Offset(size.width * 0.18f, size.height * 0.22f + offset),
                                size = Size(side, side),
                                style = Stroke(width = strokeWidth),
                            )
                            drawRoundRect(
                                color = iconColor,
                                topLeft = Offset(size.width * 0.18f + offset, size.height * 0.22f),
                                size = Size(side, side),
                                style = Stroke(width = strokeWidth),
                            )
                        } else {
                            drawRoundRect(
                                color = iconColor,
                                topLeft = Offset(size.width * 0.20f, size.height * 0.20f),
                                size = Size(size.width * 0.60f, size.height * 0.60f),
                                style = Stroke(width = strokeWidth),
                            )
                        }
                    },
                )
                TopBarActionButton(
                    onClick = onClose,
                    buttonSize = TitleBarButtonSize,
                    drawIcon = { iconColor ->
                        val strokeWidth = size.width * 0.115f
                        drawLine(
                            color = iconColor,
                            start = Offset(size.width * 0.25f, size.height * 0.25f),
                            end = Offset(size.width * 0.75f, size.height * 0.75f),
                            strokeWidth = strokeWidth,
                            cap = StrokeCap.Round,
                        )
                        drawLine(
                            color = iconColor,
                            start = Offset(size.width * 0.75f, size.height * 0.25f),
                            end = Offset(size.width * 0.25f, size.height * 0.75f),
                            strokeWidth = strokeWidth,
                            cap = StrokeCap.Round,
                        )
                    },
                )
            }

            areaContent()
        }
    }
}
