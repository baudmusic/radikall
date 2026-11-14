package com.radiko.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.radiko.platform.PlatformEnvironment
import com.radiko.station.ProgramEntry
import com.radiko.station.Station
import com.radiko.ui.theme.radikoPalette
import com.radiko.ui.theme.radikoPrimaryTextColor
import com.radiko.ui.theme.radikoSecondaryTextColor

/** READY：有当前节目；LOADING：还没拿到数据；UNAVAILABLE：请求过但没有（失败或该台不在数据里）。 */
enum class StationProgramState { READY, LOADING, UNAVAILABLE }

/**
 * 横向卡片：左边是「封面 + 白色台标签条」组成的媒体块；右边从上到下是
 * 台名眉标（小字，地区标签跟在后面）、节目名（最多两行）、「结束时间 · 主持人」，底部 2dp 细线是节目进度。
 * 台标很小时（NHK 东京/大阪、联播同一节目的不同台）靠眉标区分电台。
 * Web 端的 webApp/src/StationCard.tsx 是同一套设计。
 */
@Composable
fun StationCard(
    station: Station,
    isActive: Boolean,
    onClick: () -> Unit,
    program: ProgramEntry?,
    /** 当前选中且正在出声：台名前的音柱会跳动。 */
    isPlaying: Boolean = false,
    programState: StationProgramState,
    /** 节目已播出的比例（0–1）。 */
    progress: Float,
    /** 如「〜12:45」，已按语言格式化。 */
    endLabel: String?,
    /** 驾驶模式：放大，隐藏主持人。 */
    emphasized: Boolean = false,
    /** 电台不属于当前浏览地区时，标出它的覆盖范围（如「近畿」「全国」）。 */
    areaLabel: String? = null,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(16.dp)
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    val supportsPointerHover = PlatformEnvironment.supportsPointerHover
    val palette = radikoPalette()
    val scale = if (emphasized) 1.2f else 1f

    val borderColor by animateColorAsState(
        targetValue = when {
            isActive -> palette.accent
            hovered -> palette.lineHover
            else -> palette.lineStrong
        },
        label = "station_card_border",
    )
    val performer = if (emphasized) null else program?.performer
    // 没有节目数据时台名本身就是标题，不再重复放眉标；台号和地区标签放到下面一行。
    val unavailable = programState == StationProgramState.UNAVAILABLE
    val meta = when {
        program != null -> listOfNotNull(endLabel, performer?.takeIf { it.isNotBlank() }).joinToString(" · ")
        unavailable -> station.id
        else -> null
    }
    val metaStyle = if (emphasized) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodySmall

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(color = MaterialTheme.colorScheme.surface, shape = shape)
            .border(width = 1.dp, color = borderColor, shape = shape)
            .then(if (supportsPointerHover) Modifier.hoverable(interactionSource) else Modifier)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .semantics(mergeDescendants = true) {
                contentDescription = if (program != null) "${station.name}：${program.title}" else station.name
            },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(if (emphasized) 12.dp else 10.dp),
            horizontalArrangement = Arrangement.spacedBy(if (emphasized) 14.dp else 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StationMedia(station = station, coverUrl = program?.imageUrl, scale = scale)

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                if (!unavailable) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        val eyebrowColor = if (isActive) palette.accent else radikoSecondaryTextColor()
                        if (isActive) {
                            EqualizerIcon(playing = isPlaying, color = eyebrowColor)
                        }
                        Text(
                            text = station.name,
                            style = metaStyle,
                            fontWeight = FontWeight.Medium,
                            color = eyebrowColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        if (areaLabel != null) {
                            AreaTag(text = areaLabel)
                        }
                    }
                }
                if (programState == StationProgramState.LOADING) {
                    LoadingLines()
                } else {
                    Text(
                        text = program?.title ?: station.name,
                        style = if (emphasized) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = radikoPrimaryTextColor(),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (meta != null || (unavailable && areaLabel != null)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (unavailable && areaLabel != null) {
                            AreaTag(text = areaLabel)
                        }
                        if (meta != null) {
                            Text(
                                text = meta,
                                style = metaStyle,
                                color = radikoSecondaryTextColor(),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }

        if (program != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth(progress.coerceIn(0f, 1f))
                    .height(2.dp)
                    .background(
                        if (isActive) palette.accent else palette.progress,
                    ),
            )
        }
    }
}

/** 96×86 的媒体块：16:10 封面 + 26dp 台标签条；没有封面时整块只放台标。台标按白底设计，深色模式下也保持白色。 */
@Composable
private fun StationMedia(
    station: Station,
    coverUrl: String?,
    scale: Float,
) {
    val palette = radikoPalette()
    var failedCoverUrl by remember { mutableStateOf<String?>(null) }
    val shownCover = coverUrl?.takeIf { it.isNotBlank() && it != failedCoverUrl }
    val width = 96.dp * scale
    val mediaShape = RoundedCornerShape(10.dp)

    Column(
        modifier = Modifier
            .size(width = width, height = width * 86f / 96f)
            .clip(mediaShape)
            .background(palette.plate, mediaShape)
            .border(
                width = 1.dp,
                color = palette.line,
                shape = mediaShape,
            ),
    ) {
        if (shownCover != null) {
            AsyncImage(
                model = shownCover,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                onError = { failedCoverUrl = shownCover },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(width * 0.625f),
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                CroppedStationLogo(station = station, size = LogoSize.Strip.scaled(scale))
            }
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CroppedStationLogo(station = station, size = LogoSize.Solo.scaled(scale))
            }
        }
    }
}

/** 加载中：两条骨架占住标题和信息行的位置，数据回来时卡片不跳动。 */
@Composable
private fun LoadingLines() {
    val pulse by rememberInfiniteTransition(label = "station_card_skeleton").animateFloat(
        initialValue = 1f,
        targetValue = 0.5f,
        animationSpec = infiniteRepeatable(animation = tween(durationMillis = 700), repeatMode = RepeatMode.Reverse),
        label = "station_card_skeleton_alpha",
    )
    val color = radikoPalette().surfaceSunken
    Column(
        modifier = Modifier
            .padding(vertical = 3.dp)
            .alpha(pulse),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.82f)
                .height(12.dp)
                .background(color, RoundedCornerShape(6.dp)),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth(0.52f)
                .height(12.dp)
                .background(color, RoundedCornerShape(6.dp)),
        )
    }
}

/** 当前台的小音柱：正在出声时跳动，暂停时静止。 */
@Composable
private fun EqualizerIcon(playing: Boolean, color: Color) {
    val heights = if (playing) {
        val transition = rememberInfiniteTransition(label = "station_eq")
        listOf(0, 300, 600).map { delayMillis ->
            transition.animateFloat(
                initialValue = 0.3f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 900),
                    repeatMode = RepeatMode.Reverse,
                    initialStartOffset = StartOffset(delayMillis),
                ),
                label = "station_eq_$delayMillis",
            ).value
        }
    } else {
        listOf(0.6f, 1f, 0.4f)
    }
    Canvas(modifier = Modifier.size(width = 9.dp, height = 10.dp)) {
        val barWidth = 2.dp.toPx()
        val gap = 1.5.dp.toPx()
        heights.forEachIndexed { index, fraction ->
            val barHeight = size.height * fraction
            drawRoundRect(
                color = color,
                topLeft = Offset(index * (barWidth + gap), size.height - barHeight),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(1.dp.toPx()),
            )
        }
    }
}

@Composable
private fun AreaTag(text: String) {
    val palette = radikoPalette()
    val tint = palette.info
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = tint,
        fontWeight = FontWeight.Medium,
        maxLines = 1,
        modifier = Modifier
            .background(color = palette.infoSoft, shape = RoundedCornerShape(999.dp))
            .padding(horizontal = 8.dp, vertical = 2.dp),
    )
}
