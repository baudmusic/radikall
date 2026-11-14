package com.radiko.station

import kotlin.math.sqrt

/** 卡片用的台标图：去掉左右留白的 688×160 版本，裁掉上下留白放大后在高密度屏上也清晰。 */
val Station.wideLogoUrl: String
    get() = "https://radiko.jp/v2/static/station/logo/$id/lrtrim/688x160.png"

/** 台标在卡片上的摆放（单位 dp）：外框只框住墨迹，里面的整张图按 offset 往左上挪，把透明边藏到框外。 */
data class LogoBox(
    val inkWidth: Float,
    val inkHeight: Float,
    val imageWidth: Float,
    val imageHeight: Float,
    val offsetX: Float,
    val offsetY: Float,
)

/**
 * 按「视觉面积」统一台标大小：墨迹面积尽量等于 [area]，同时不超过 [maxWidth] × [maxHeight]。
 * 细长的字标会更宽更矮、方形徽标会顶到高度上限，看起来分量相近，而不是按图片尺寸一律缩放。
 * 没有测量数据（新加的台，见 tools/generate_logo_ink.mjs）时返回 null，由调用方退回普通缩放。
 * Web 端的 webApp/src/stationLogo.ts 是同一套算法。
 */
fun fitLogo(stationId: String, maxWidth: Float, maxHeight: Float, area: Float): LogoBox? {
    val ink = stationLogoInk[stationId] ?: return null
    val inkAspect = ink.aspect * (ink.right - ink.left) / (ink.bottom - ink.top)
    val inkHeight = minOf(maxHeight, sqrt(area / inkAspect), maxWidth / inkAspect)
    val inkWidth = inkHeight * inkAspect
    val imageHeight = inkHeight / (ink.bottom - ink.top)
    val imageWidth = imageHeight * ink.aspect
    return LogoBox(
        inkWidth = inkWidth,
        inkHeight = inkHeight,
        imageWidth = imageWidth,
        imageHeight = imageHeight,
        offsetX = ink.left * imageWidth,
        offsetY = ink.top * imageHeight,
    )
}
