package com.radiko.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.radiko.i18n.appStrings
import com.radiko.platform.PlatformEnvironment
import com.radiko.station.PrefectureData
import com.radiko.ui.theme.RadikoColors

private val AreaSelectorHorizontalPadding = 18.dp
private val AreaSelectorVerticalPadding = 10.dp
private val AreaSelectorChevronSpacing = 8.dp
private val AreaSelectorChevronWidth = 10.dp
private val AreaSelectorChevronHeight = 6.dp
private val AreaSelectorChevronStroke = 2.dp
private val AreaSelectorMenuMaxHeight = 420.dp

@Composable
fun AreaSelector(
    currentAreaId: String,
    onAreaSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = appStrings()
    val currentPrefecture = PrefectureData.prefecture(currentAreaId)
    var expanded by remember { mutableStateOf(false) }
    val pillShape = RoundedCornerShape(999.dp)
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    val isHovered = PlatformEnvironment.supportsPointerHover && hovered

    val scale by animateFloatAsState(
        targetValue = if (isHovered) 1.02f else 1f,
        label = "area_selector_scale",
    )
    val containerColor by animateColorAsState(
        targetValue = if (isHovered) RadikoColors.AccentRed else Color.White,
        label = "area_selector_container",
    )
    val borderColor by animateColorAsState(
        targetValue = RadikoColors.AccentRed,
        label = "area_selector_border",
    )
    val contentColor by animateColorAsState(
        targetValue = if (isHovered) Color.White else RadikoColors.AccentRed,
        label = "area_selector_content",
    )
    val hoverModifier = if (PlatformEnvironment.supportsPointerHover) {
        Modifier.hoverable(interactionSource = interactionSource)
    } else {
        Modifier
    }

    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .then(hoverModifier)
                .background(color = containerColor, shape = pillShape)
                .border(width = 1.dp, color = borderColor, shape = pillShape)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = { expanded = true },
                )
                .padding(
                    horizontal = AreaSelectorHorizontalPadding,
                    vertical = AreaSelectorVerticalPadding,
                ),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = strings.prefectureName(currentAreaId, currentPrefecture?.name ?: currentAreaId),
                    style = MaterialTheme.typography.labelLarge,
                    color = contentColor,
                    fontWeight = FontWeight.Medium,
                )
                Spacer(modifier = Modifier.width(AreaSelectorChevronSpacing))
                Canvas(
                    modifier = Modifier.size(
                        width = AreaSelectorChevronWidth,
                        height = AreaSelectorChevronHeight,
                    ),
                ) {
                    val strokeWidth = AreaSelectorChevronStroke.toPx()
                    drawLine(
                        color = contentColor,
                        start = Offset(0f, 0f),
                        end = Offset(size.width / 2f, size.height),
                        strokeWidth = strokeWidth,
                        cap = StrokeCap.Round,
                    )
                    drawLine(
                        color = contentColor,
                        start = Offset(size.width, 0f),
                        end = Offset(size.width / 2f, size.height),
                        strokeWidth = strokeWidth,
                        cap = StrokeCap.Round,
                    )
                }
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.heightIn(max = AreaSelectorMenuMaxHeight),
            shape = RoundedCornerShape(20.dp),
            containerColor = RadikoColors.AccentRed,
            shadowElevation = 0.dp,
            border = androidx.compose.foundation.BorderStroke(1.dp, RadikoColors.AccentRed),
        ) {
            Column(
                modifier = Modifier
                    .heightIn(max = AreaSelectorMenuMaxHeight)
                    .background(color = RadikoColors.AccentRed)
                    .verticalScroll(rememberScrollState()),
            ) {
                PrefectureData.regions.forEachIndexed { index, region ->
                    if (index > 0) {
                        HorizontalDivider(color = Color.White.copy(alpha = 0.18f))
                    }
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = strings.regionName(region.id, region.name),
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White.copy(alpha = 0.74f),
                            )
                        },
                        onClick = {},
                        enabled = false,
                        colors = MenuDefaults.itemColors(
                            disabledTextColor = Color.White.copy(alpha = 0.74f),
                        ),
                    )
                    PrefectureData.prefecturesForRegion(region.id).forEach { prefecture ->
                        DropdownMenuItem(
                            modifier = Modifier.background(
                                color = if (prefecture.id == currentAreaId) {
                                    Color.White.copy(alpha = 0.14f)
                                } else {
                                    Color.Transparent
                                },
                                shape = RoundedCornerShape(16.dp),
                            ),
                            text = {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 8.dp),
                                ) {
                                    Text(
                                        text = strings.prefectureName(prefecture.id, prefecture.name),
                                        color = Color.White,
                                    )
                                }
                            },
                            onClick = {
                                expanded = false
                                onAreaSelected(prefecture.id)
                            },
                            colors = MenuDefaults.itemColors(
                                textColor = Color.White,
                            ),
                        )
                    }
                }
            }
        }
    }
}
