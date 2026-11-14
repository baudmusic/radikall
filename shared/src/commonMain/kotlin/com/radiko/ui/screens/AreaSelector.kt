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
import com.radiko.station.ALL_AREAS_ID
import com.radiko.station.PrefectureData
import com.radiko.ui.theme.radikoPalette

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
    /** 在菜单顶部提供「全国」（[ALL_AREAS_ID]）。只用于浏览，设置里选固定地区时不提供。 */
    includeAllAreas: Boolean = false,
) {
    val strings = appStrings()
    val palette = radikoPalette()
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
        targetValue = if (isHovered) palette.surface else palette.accent,
        label = "area_selector_container",
    )
    val borderColor by animateColorAsState(
        targetValue = palette.accent,
        label = "area_selector_border",
    )
    val contentColor by animateColorAsState(
        targetValue = if (isHovered) palette.accent else palette.onAccent,
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
                    text = strings.areaLabel(currentAreaId),
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
            containerColor = palette.surface,
            shadowElevation = 8.dp,
            border = androidx.compose.foundation.BorderStroke(1.dp, palette.line),
        ) {
            Column(
                modifier = Modifier
                    .heightIn(max = AreaSelectorMenuMaxHeight)
                    .background(color = palette.surface)
                    .verticalScroll(rememberScrollState()),
            ) {
                if (includeAllAreas) {
                    DropdownMenuItem(
                        modifier = Modifier.background(
                            color = if (currentAreaId == ALL_AREAS_ID) palette.accent else Color.Transparent,
                            shape = RoundedCornerShape(16.dp),
                        ),
                        text = {
                            Text(
                                text = strings.allAreas,
                                color = if (currentAreaId == ALL_AREAS_ID) palette.onAccent else palette.ink,
                                fontWeight = FontWeight.Medium,
                            )
                        },
                        onClick = {
                            expanded = false
                            onAreaSelected(ALL_AREAS_ID)
                        },
                        colors = MenuDefaults.itemColors(
                            textColor = palette.ink,
                        ),
                    )
                    HorizontalDivider(color = palette.line)
                }
                PrefectureData.regions.forEachIndexed { index, region ->
                    if (index > 0) {
                        HorizontalDivider(color = palette.line)
                    }
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = strings.regionName(region.id, region.name),
                                style = MaterialTheme.typography.labelMedium,
                                color = palette.accent,
                                fontWeight = FontWeight.Bold,
                            )
                        },
                        onClick = {},
                        enabled = false,
                        colors = MenuDefaults.itemColors(
                            disabledTextColor = palette.accent,
                        ),
                    )
                    PrefectureData.prefecturesForRegion(region.id).forEach { prefecture ->
                        DropdownMenuItem(
                            modifier = Modifier.background(
                                color = if (prefecture.id == currentAreaId) palette.accent else Color.Transparent,
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
                                        color = if (prefecture.id == currentAreaId) palette.onAccent else palette.ink,
                                    )
                                }
                            },
                            onClick = {
                                expanded = false
                                onAreaSelected(prefecture.id)
                            },
                            colors = MenuDefaults.itemColors(
                                textColor = palette.ink,
                            ),
                        )
                    }
                }
            }
        }
    }
}
