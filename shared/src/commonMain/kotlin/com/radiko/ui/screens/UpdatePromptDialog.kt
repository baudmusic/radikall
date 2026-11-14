package com.radiko.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.radiko.i18n.appStrings
import com.radiko.settings.AppLanguage
import com.radiko.ui.theme.radikoPalette
import com.radiko.update.AppUpdate
import com.radiko.update.notesFor
import kotlin.math.roundToInt

/**
 * 更新提醒（设计稿 A「瑠璃の幕」）：上半是和播放条同色的瑠璃舞台，右上角一粒韓紅花的「电台信号」
 * 一圈圈向外扩散；下半是更新要点和两个按钮。
 *
 * - 「下载更新」→ [onDownload]（打开本平台安装包直链）
 * - 「暂不更新」→ [onLater]（同一版本 3 天内不再提醒）
 * - 返回键 → [onDismiss]（只是这次先关掉，下次启动还会提醒）。点外面不关，免得误触。
 */
@Composable
fun UpdatePromptDialog(
    update: AppUpdate,
    currentVersion: String,
    language: AppLanguage,
    onDownload: () -> Unit,
    onLater: () -> Unit,
    onDismiss: () -> Unit,
    onOpenReleasePage: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
        ),
    ) {
        Box(Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
            UpdatePromptCard(
                update = update,
                currentVersion = currentVersion,
                language = language,
                onDownload = onDownload,
                onLater = onLater,
                onOpenReleasePage = onOpenReleasePage,
            )
        }
    }
}

@Composable
internal fun UpdatePromptCard(
    update: AppUpdate,
    currentVersion: String,
    language: AppLanguage,
    onDownload: () -> Unit,
    onLater: () -> Unit,
    onOpenReleasePage: () -> Unit,
) {
    val palette = radikoPalette()
    val strings = appStrings()
    val notes = update.notesFor(language)
    val shape = RoundedCornerShape(28.dp)

    Column(
        modifier = Modifier
            .widthIn(max = 360.dp)
            .fillMaxWidth()
            .shadow(28.dp, shape, ambientColor = palette.shadow, spotColor = palette.shadow)
            .clip(shape)
            .background(palette.surface)
            .verticalScroll(rememberScrollState()),
    ) {
        Box(Modifier.fillMaxWidth().background(palette.stage)) {
            SignalRings(
                ringColor = palette.onStage,
                sourceColor = palette.accent,
                modifier = Modifier.matchParentSize(),
            )
            Column(Modifier.padding(start = 26.dp, end = 26.dp, top = 28.dp, bottom = 26.dp)) {
                Text(
                    text = strings.updateAvailableTitle,
                    color = palette.onStage2,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = update.version,
                    color = palette.onStage,
                    fontSize = 56.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 60.sp,
                    letterSpacing = (-1).sp,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = updateMetaLine(strings.updateCurrentVersion(currentVersion), update),
                    color = palette.onStage2,
                    fontSize = 12.5.sp,
                )
            }
        }

        Column(Modifier.padding(start = 26.dp, end = 26.dp, top = 22.dp, bottom = 14.dp)) {
            if (notes.isNotEmpty()) {
                Text(
                    text = strings.updateWhatsNew,
                    color = palette.ink3,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp,
                )
                Spacer(Modifier.height(12.dp))
                notes.forEach { note ->
                    Row(Modifier.padding(bottom = 10.dp)) {
                        Box(
                            Modifier
                                .padding(top = 8.dp)
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(palette.accent),
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(text = note, color = palette.ink, fontSize = 14.5.sp, lineHeight = 21.sp)
                    }
                }
            } else if (update.releasePageUrl.isNotBlank()) {
                // 发布说明里没有能列出来的要点时，至少给个入口去看完整说明。
                Text(
                    text = "${strings.updateReleaseNotesLink} ›",
                    color = palette.info,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onOpenReleasePage)
                        .padding(vertical = 6.dp),
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .clip(RoundedCornerShape(25.dp))
                    .background(palette.accent)
                    .clickable(onClick = onDownload),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                DownloadGlyph(palette.onAccent)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = strings.updateDownload,
                    color = palette.onAccent,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Spacer(Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .clickable(onClick = onLater),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = strings.updateLater,
                    color = palette.ink2,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

/** 「当前 1.0.0 · 20.3 MB · 2026.11.20」，大小或日期取不到就省掉那一段。 */
private fun updateMetaLine(currentVersionLabel: String, update: AppUpdate): String =
    listOfNotNull(
        currentVersionLabel,
        update.sizeBytes.takeIf { it > 0 }?.let(::formatMegabytes),
        update.publishedDate?.replace('-', '.'),
    ).joinToString("  ·  ")

private fun formatMegabytes(bytes: Long): String {
    val tenths = (bytes / 1_048_576.0 * 10).roundToInt()
    return "${tenths / 10}.${tenths % 10} MB"
}

/** 右上角的电台信号：一粒韓紅花的点，外面一圈圈越来越淡的同心圆。 */
@Composable
private fun SignalRings(ringColor: Color, sourceColor: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val center = Offset(size.width - 44.dp.toPx(), 48.dp.toPx())
        val stroke = Stroke(1.4.dp.toPx())
        listOf(24, 46, 68, 90, 112, 134, 156).forEachIndexed { index, radius ->
            drawCircle(
                color = ringColor.copy(alpha = (0.2f - index * 0.026f).coerceAtLeast(0.03f)),
                radius = radius.dp.toPx(),
                center = center,
                style = stroke,
            )
        }
        drawCircle(sourceColor, radius = 7.dp.toPx(), center = center)
    }
}

/** 下载箭头。项目没引图标库，用几笔线画出来。 */
@Composable
private fun DownloadGlyph(color: Color) {
    Canvas(Modifier.size(16.dp)) {
        val stroke = 2.dp.toPx()
        val w = size.width
        val h = size.height
        drawLine(color, Offset(w / 2, h * 0.08f), Offset(w / 2, h * 0.66f), stroke, StrokeCap.Round)
        drawLine(color, Offset(w * 0.24f, h * 0.42f), Offset(w / 2, h * 0.68f), stroke, StrokeCap.Round)
        drawLine(color, Offset(w * 0.76f, h * 0.42f), Offset(w / 2, h * 0.68f), stroke, StrokeCap.Round)
        drawLine(color, Offset(w * 0.12f, h * 0.92f), Offset(w * 0.88f, h * 0.92f), stroke, StrokeCap.Round)
    }
}
