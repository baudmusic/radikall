package com.radiko.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.radiko.station.Station
import com.radiko.station.fitLogo
import com.radiko.station.logoUrl
import com.radiko.station.wideLogoUrl

/** 台标尺寸档位：最宽、最高（都是墨迹的尺寸）和目标墨迹面积（dp²）。 */
internal data class LogoSize(val maxWidth: Dp, val maxHeight: Dp, val area: Float) {
    fun scaled(factor: Float) = LogoSize(maxWidth * factor, maxHeight * factor, area * factor * factor)

    companion object {
        /** 有封面时，放在封面下方 26dp 白色签条里。 */
        val Strip = LogoSize(maxWidth = 80.dp, maxHeight = 18.dp, area = 720f)

        /** 没有封面时，整块媒体区只放台标。 */
        val Solo = LogoSize(maxWidth = 76.dp, maxHeight = 46.dp, area = 2000f)
    }
}

/**
 * 按测量好的墨迹范围（StationLogoInk.kt）只显示台标有内容的部分：外框只有墨迹大小，
 * 整张图按偏移量画在框里、超出部分被裁掉。没有测量数据或高清图加载失败时，退回普通缩放的小图。
 */
@Composable
internal fun CroppedStationLogo(
    station: Station,
    size: LogoSize,
    modifier: Modifier = Modifier,
) {
    var failed by remember(station.id) { mutableStateOf(false) }
    val box = if (failed) null else fitLogo(station.id, size.maxWidth.value, size.maxHeight.value, size.area)

    if (box == null) {
        Box(modifier = modifier.size(size.maxWidth, size.maxHeight)) {
            AsyncImage(
                model = station.logoUrl,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
            )
        }
        return
    }

    Box(modifier = modifier.size(box.inkWidth.dp, box.inkHeight.dp).clipToBounds()) {
        AsyncImage(
            model = station.wideLogoUrl,
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            onError = { failed = true },
            modifier = Modifier.layout { measurable, _ ->
                val placeable = measurable.measure(
                    Constraints.fixed(box.imageWidth.dp.roundToPx(), box.imageHeight.dp.roundToPx()),
                )
                layout(box.inkWidth.dp.roundToPx(), box.inkHeight.dp.roundToPx()) {
                    placeable.place(-box.offsetX.dp.roundToPx(), -box.offsetY.dp.roundToPx())
                }
            },
        )
    }
}
