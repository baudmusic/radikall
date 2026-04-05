package com.radiko.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
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

object RadikoColors {
    val PrimaryBlue = Color(0xFF005CAF)
    val NowPlayingBlue = Color(0xFF005CAF)
    val ScheduleHighlight = Color(0xFF005CAF)
    val DeepBlue = Color(0xFF113285)
    val DarkRed = Color(0xFFD0104C)

    val AccentRed = Color(0xFFD0104C)
    val PastProgramRed = Color(0xFFD0104C)

    val White = Color(0xFFFAFAF8)
    val LightGray = Color(0xFFF2F0EC)
    val MediumGray = Color(0xFFD8D5D0)
    val DarkText = Color(0xFF2C2C2A)

    val TextSecondary = Color(0xFF6B6966)
    val BorderLight = Color(0xFFE6E3DE)
    val SubtleBlue = Color(0xFFEAF3F7)
    val ScheduleLightBlue = Color(0xFFEAF3F7)

    // ── 渐变色 ──
    val NowPlayingGradientTop = Color(0xFF005CAF)
    val NowPlayingGradientMid = Color(0xFF113285)
    val NowPlayingGradientBottom = Color(0xFF113285)
    val PlayerBarGradientStart = Color(0xFF005CAF)
    val PlayerBarGradientEnd = Color(0xFF113285)
}

@Composable
fun radikoPanelColor(): Color = if (MaterialTheme.colorScheme.surface.luminance() < 0.5f) {
    MaterialTheme.colorScheme.surfaceVariant
} else {
    RadikoColors.ScheduleLightBlue
}

@Composable
fun radikoPanelBorderColor(): Color = if (MaterialTheme.colorScheme.surface.luminance() < 0.5f) {
    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.72f)
} else {
    RadikoColors.PrimaryBlue.copy(alpha = 0.16f)
}

@Composable
fun radikoPrimaryTextColor(): Color = MaterialTheme.colorScheme.onSurface

@Composable
fun radikoSecondaryTextColor(alpha: Float = 0.72f): Color =
    MaterialTheme.colorScheme.onSurface.copy(alpha = alpha)

private val RadikoColorScheme = lightColorScheme(
    primary = RadikoColors.PrimaryBlue,
    onPrimary = RadikoColors.White,
    secondary = RadikoColors.AccentRed,
    onSecondary = RadikoColors.White,
    tertiary = RadikoColors.DarkRed,
    background = RadikoColors.White,
    onBackground = RadikoColors.DarkText,
    surface = RadikoColors.White,
    onSurface = RadikoColors.DarkText,
    surfaceVariant = RadikoColors.LightGray,
    outlineVariant = RadikoColors.MediumGray,
)

private val RadikoDarkColorScheme = darkColorScheme(
    primary = RadikoColors.PrimaryBlue,
    onPrimary = Color.White,
    secondary = RadikoColors.AccentRed,
    onSecondary = Color.White,
    tertiary = RadikoColors.DeepBlue,
    background = Color(0xFF0F1724),
    onBackground = Color(0xFFF8FAFC),
    surface = Color(0xFF162132),
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = Color(0xFF1D2A3D),
    outlineVariant = Color(0xFF334155),
)

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

    MaterialTheme(
        colorScheme = if (useDarkTheme) RadikoDarkColorScheme else RadikoColorScheme,
        typography = radikoTypography,
        shapes = RadikoShapes,
        content = content,
    )
}
