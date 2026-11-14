package com.radiko.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.radiko.settings.AppThemeMode
import org.jetbrains.compose.resources.Font
import radiko_app.shared.generated.resources.Res
import radiko_app.shared.generated.resources.noto_sans_jp_bold
import radiko_app.shared.generated.resources.noto_sans_jp_medium
import radiko_app.shared.generated.resources.noto_sans_jp_regular

/**
 * 日本の伝統色（https://nipponcolors.com/）。界面上的颜色都取自这里，或由它们按比例混合、加透明度得到，不另造色值。
 * Web 端 webApp/src/styles.css 开头的变量是同一套颜色和角色。
 */
object NipponColors {
    /** 韓紅花：主题色，只表示「当前 / 选中 / 正在播」。 */
    val Karakurenai = Color(0xFFD0104C)
    val Gofun = Color(0xFFFFFFFB) // 胡粉
    val Shironeri = Color(0xFFFCFAF2) // 白練
    val Shironezumi = Color(0xFFBDC0BA) // 白鼠
    val Ginnezumi = Color(0xFF91989F) // 銀鼠
    val Nibi = Color(0xFF656765) // 鈍
    val Sumi = Color(0xFF1C1C1C) // 墨
    val Ro = Color(0xFF0C0C0C) // 呂
    val Ruri = Color(0xFF005CAF) // 瑠璃
    val Konjyo = Color(0xFF113285) // 紺青
    val Rurikon = Color(0xFF0B346E) // 瑠璃紺
    val Wasurenagusa = Color(0xFF7DB9DE) // 勿忘草
}

/** 两种模式通用的品牌色，主要给桌面端托盘这类不跟随主题的界面用。界面里请用 [radikoPalette]。 */
object RadikoColors {
    val PrimaryBlue = NipponColors.Ruri
    val AccentRed = NipponColors.Karakurenai
    val White = NipponColors.Gofun
    val DarkText = NipponColors.Sumi
    val TextSecondary = NipponColors.Nibi
}

/**
 * 界面配色角色。名字和 Web 端的 CSS 变量一一对应：
 * page=--page、surface=--surface、surfaceSunken=--surface-sunken、line=--line、ink=--ink……
 */
@Immutable
data class RadikoPalette(
    val isDark: Boolean,
    /** 页面底色。 */
    val page: Color,
    /** 卡片、面板。 */
    val surface: Color,
    /** 输入框、选择片、节目表的行等下沉一层的块。 */
    val surfaceSunken: Color,
    /** radikoPanelColor()：页面里等于 surfaceSunken，正在播放页里是舞台上的面板。 */
    val panel: Color,
    val line: Color,
    val lineStrong: Color,
    val lineHover: Color,
    /** 卡片底部的节目进度线（非当前台）。 */
    val progress: Color,
    val ink: Color,
    val ink2: Color,
    val ink3: Color,
    val accent: Color,
    val onAccent: Color,
    /** 链接、地区标签等信息色。 */
    val info: Color,
    val infoSoft: Color,
    /** 台标、封面的衬底：台标按白底设计，两种模式都用胡粉。 */
    val plate: Color,
    /** 播放条和正在播放页的纯色舞台。 */
    val stage: Color,
    val onStage: Color,
    val onStage2: Color,
    val stageLine: Color,
    val stagePanel: Color,
    val scrim: Color,
    val shadow: Color,
)

/** 按 sRGB 通道线性混合，和 CSS 的 color-mix(in srgb, top fraction, base) 一致。 */
private fun mix(top: Color, fraction: Float, base: Color): Color = Color(
    red = base.red + (top.red - base.red) * fraction,
    green = base.green + (top.green - base.green) * fraction,
    blue = base.blue + (top.blue - base.blue) * fraction,
)

object RadikoPalettes {
    /** 浅色：胡粉的白底，瑠璃做轻的细线和淡底，强色留给韓紅花和蓝色舞台。 */
    val Light = RadikoPalette(
        isDark = false,
        page = NipponColors.Gofun,
        surface = NipponColors.Gofun,
        surfaceSunken = mix(NipponColors.Ruri, 0.05f, NipponColors.Gofun),
        panel = mix(NipponColors.Ruri, 0.05f, NipponColors.Gofun),
        line = NipponColors.Ruri.copy(alpha = 0.15f),
        lineStrong = NipponColors.Ruri.copy(alpha = 0.30f),
        lineHover = NipponColors.Ruri.copy(alpha = 0.62f),
        progress = NipponColors.Ruri.copy(alpha = 0.50f),
        ink = NipponColors.Sumi,
        ink2 = NipponColors.Nibi,
        ink3 = NipponColors.Ginnezumi,
        accent = NipponColors.Karakurenai,
        onAccent = NipponColors.Gofun,
        info = NipponColors.Ruri,
        infoSoft = NipponColors.Ruri.copy(alpha = 0.09f),
        plate = NipponColors.Gofun,
        stage = NipponColors.Ruri,
        onStage = NipponColors.Gofun,
        onStage2 = NipponColors.Gofun.copy(alpha = 0.80f),
        stageLine = NipponColors.Gofun.copy(alpha = 0.16f),
        stagePanel = NipponColors.Gofun,
        scrim = NipponColors.Sumi.copy(alpha = 0.36f),
        shadow = NipponColors.Konjyo.copy(alpha = 0.10f),
    )

    /** 深色：墨底，层次用少量胡粉叠出来；整页只有韓紅花一种强色，舞台是纯色瑠璃紺。 */
    val Dark = RadikoPalette(
        isDark = true,
        page = NipponColors.Sumi,
        surface = mix(NipponColors.Gofun, 0.05f, NipponColors.Sumi),
        surfaceSunken = mix(NipponColors.Gofun, 0.09f, NipponColors.Sumi),
        panel = mix(NipponColors.Gofun, 0.09f, NipponColors.Sumi),
        line = NipponColors.Shironezumi.copy(alpha = 0.16f),
        lineStrong = NipponColors.Shironezumi.copy(alpha = 0.32f),
        lineHover = NipponColors.Shironezumi.copy(alpha = 0.55f),
        progress = NipponColors.Shironezumi.copy(alpha = 0.45f),
        ink = NipponColors.Shironeri,
        ink2 = NipponColors.Shironezumi,
        ink3 = NipponColors.Ginnezumi,
        accent = NipponColors.Karakurenai,
        onAccent = NipponColors.Gofun,
        info = NipponColors.Wasurenagusa,
        infoSoft = NipponColors.Wasurenagusa.copy(alpha = 0.14f),
        plate = NipponColors.Gofun,
        stage = NipponColors.Rurikon,
        onStage = NipponColors.Gofun,
        onStage2 = NipponColors.Gofun.copy(alpha = 0.80f),
        stageLine = NipponColors.Gofun.copy(alpha = 0.16f),
        stagePanel = mix(NipponColors.Gofun, 0.07f, NipponColors.Rurikon),
        scrim = NipponColors.Ro.copy(alpha = 0.58f),
        shadow = NipponColors.Ro.copy(alpha = 0.50f),
    )
}

/**
 * 正在播放页（蓝色舞台）里用的配色：面板换成舞台面板；
 * 深色模式下，面板、下沉层和细线都从瑠璃紺叠出来，不用外面墨色页面的中性灰。
 */
fun RadikoPalette.onStage(): RadikoPalette = if (isDark) {
    copy(
        surface = stagePanel,
        surfaceSunken = mix(NipponColors.Gofun, 0.12f, NipponColors.Rurikon),
        panel = stagePanel,
        line = NipponColors.Gofun.copy(alpha = 0.14f),
    )
} else {
    copy(panel = stagePanel)
}

val LocalRadikoPalette = staticCompositionLocalOf { RadikoPalettes.Light }

/** 当前配色。正在播放页里是 [onStage] 之后的版本。 */
@Composable
@ReadOnlyComposable
fun radikoPalette(): RadikoPalette = LocalRadikoPalette.current

@Composable
@ReadOnlyComposable
fun radikoPanelColor(): Color = radikoPalette().panel

@Composable
@ReadOnlyComposable
fun radikoPanelBorderColor(): Color = radikoPalette().line

@Composable
@ReadOnlyComposable
fun radikoPrimaryTextColor(): Color = radikoPalette().ink

/** 次要文字。[alpha] 低于 0.7 时视为「提示」级别，用更淡的 ink3。 */
@Composable
@ReadOnlyComposable
fun radikoSecondaryTextColor(alpha: Float = 0.72f): Color =
    if (alpha < 0.7f) radikoPalette().ink3 else radikoPalette().ink2

private fun RadikoPalette.toColorScheme() = if (isDark) {
    darkColorScheme(
        primary = info,
        onPrimary = NipponColors.Sumi,
        secondary = accent,
        onSecondary = onAccent,
        tertiary = stage,
        background = page,
        onBackground = ink,
        surface = surface,
        onSurface = ink,
        surfaceVariant = surfaceSunken,
        onSurfaceVariant = ink2,
        outline = lineStrong,
        outlineVariant = line,
        error = accent,
    )
} else {
    lightColorScheme(
        primary = info,
        onPrimary = onAccent,
        secondary = accent,
        onSecondary = onAccent,
        tertiary = stage,
        background = page,
        onBackground = ink,
        surface = surface,
        onSurface = ink,
        surfaceVariant = surfaceSunken,
        onSurfaceVariant = ink2,
        outline = lineStrong,
        outlineVariant = line,
        error = accent,
    )
}

private val RadikoShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun RadikoTheme(
    themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    content: @Composable () -> Unit,
) {
    val useDarkTheme = when (themeMode) {
        AppThemeMode.SYSTEM -> isSystemInDarkTheme()
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
    }
    val notoSansJP = FontFamily(
        Font(Res.font.noto_sans_jp_regular, FontWeight.Normal),
        Font(Res.font.noto_sans_jp_medium, FontWeight.Medium),
        Font(Res.font.noto_sans_jp_bold, FontWeight.Bold),
    )

    val radikoTypography = Typography(
        displayLarge = TextStyle(
            fontFamily = notoSansJP,
            fontWeight = FontWeight.Bold,
            fontSize = 57.sp,
            lineHeight = 64.sp,
            letterSpacing = (-0.25).sp,
        ),
        displayMedium = TextStyle(
            fontFamily = notoSansJP,
            fontWeight = FontWeight.Bold,
            fontSize = 45.sp,
            lineHeight = 52.sp,
            letterSpacing = 0.sp,
        ),
        displaySmall = TextStyle(
            fontFamily = notoSansJP,
            fontWeight = FontWeight.Bold,
            fontSize = 36.sp,
            lineHeight = 44.sp,
            letterSpacing = 0.sp,
        ),
        headlineLarge = TextStyle(
            fontFamily = notoSansJP,
            fontWeight = FontWeight.Bold,
            fontSize = 32.sp,
            lineHeight = 40.sp,
            letterSpacing = 0.15.sp,
        ),
        headlineMedium = TextStyle(
            fontFamily = notoSansJP,
            fontWeight = FontWeight.Bold,
            fontSize = 28.sp,
            lineHeight = 36.sp,
            letterSpacing = 0.15.sp,
        ),
        headlineSmall = TextStyle(
            fontFamily = notoSansJP,
            fontWeight = FontWeight.SemiBold,
            fontSize = 24.sp,
            lineHeight = 32.sp,
            letterSpacing = 0.3.sp,
        ),
        titleLarge = TextStyle(
            fontFamily = notoSansJP,
            fontWeight = FontWeight.SemiBold,
            fontSize = 22.sp,
            lineHeight = 28.sp,
            letterSpacing = 0.15.sp,
        ),
        titleMedium = TextStyle(
            fontFamily = notoSansJP,
            fontWeight = FontWeight.Medium,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            letterSpacing = 0.15.sp,
        ),
        titleSmall = TextStyle(
            fontFamily = notoSansJP,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.1.sp,
        ),
        bodyLarge = TextStyle(
            fontFamily = notoSansJP,
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            lineHeight = 26.sp,
            letterSpacing = 0.15.sp,
        ),
        bodyMedium = TextStyle(
            fontFamily = notoSansJP,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            lineHeight = 22.sp,
            letterSpacing = 0.15.sp,
        ),
        bodySmall = TextStyle(
            fontFamily = notoSansJP,
            fontWeight = FontWeight.Normal,
            fontSize = 12.sp,
            lineHeight = 18.sp,
            letterSpacing = 0.2.sp,
        ),
        labelLarge = TextStyle(
            fontFamily = notoSansJP,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.4.sp,
        ),
        labelMedium = TextStyle(
            fontFamily = notoSansJP,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.5.sp,
        ),
        labelSmall = TextStyle(
            fontFamily = notoSansJP,
            fontWeight = FontWeight.Medium,
            fontSize = 11.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.5.sp,
        ),
    )

    val palette = if (useDarkTheme) RadikoPalettes.Dark else RadikoPalettes.Light
    CompositionLocalProvider(LocalRadikoPalette provides palette) {
        MaterialTheme(
            colorScheme = palette.toColorScheme(),
            typography = radikoTypography,
            shapes = RadikoShapes,
            content = content,
        )
    }
}
