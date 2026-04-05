package com.radiko.settings

enum class StartupAreaMode {
    REMEMBER_LAST,
    FIXED,
}

enum class AppThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
}

enum class AppLanguage {
    SIMPLIFIED_CHINESE,
    TRADITIONAL_CHINESE,
    ENGLISH,
    JAPANESE,
    KOREAN,
}

enum class AudioFocusBehavior {
    DUCK,
    PAUSE,
}

enum class CloseButtonMode {
    ASK,
    MINIMIZE_TO_TRAY,
    EXIT,
}

data class AppSettings(
    val language: AppLanguage = AppLanguage.ENGLISH,
    val startupAreaMode: StartupAreaMode = StartupAreaMode.REMEMBER_LAST,
    val fixedAreaId: String = "JP13",
    val lastAreaId: String = "JP13",
    val lastStationId: String? = null,
    val autoPlayOnLaunch: Boolean = false,
    val backgroundPlaybackEnabled: Boolean = true,
    val wifiOnlyPlayback: Boolean = false,
    val confirmMobileDataPlayback: Boolean = true,
    val themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    val closeButtonMode: CloseButtonMode = CloseButtonMode.ASK,
    val audioFocusBehavior: AudioFocusBehavior = AudioFocusBehavior.DUCK,
    val alarmEnabled: Boolean = false,
    val alarmHour: Int = 7,
    val alarmMinute: Int = 0,
    val alarmStationId: String = "FMT",
    val drivingModeEnabled: Boolean = false,
) {
    fun resolvedStartupAreaId(): String = when (startupAreaMode) {
        StartupAreaMode.REMEMBER_LAST -> lastAreaId.ifBlank { "JP13" }
        StartupAreaMode.FIXED -> fixedAreaId.ifBlank { "JP13" }
    }
}

object SettingsKeys {
    const val LANGUAGE = "language"
    const val STARTUP_AREA_MODE = "startup_area_mode"
    const val FIXED_AREA_ID = "fixed_area_id"
    const val LAST_AREA_ID = "last_area_id"
    const val LAST_STATION_ID = "last_station_id"
    const val AUTOPLAY_ON_LAUNCH = "autoplay_on_launch"
    const val BACKGROUND_PLAYBACK = "background_playback"
    const val WIFI_ONLY_PLAYBACK = "wifi_only_playback"
    const val MOBILE_DATA_CONFIRM = "mobile_data_confirm"
    const val THEME_MODE = "theme_mode"
    const val CLOSE_BEHAVIOR = "close_behavior"
    const val AUDIO_FOCUS_BEHAVIOR = "audio_focus_behavior"
    const val ALARM_ENABLED = "alarm_enabled"
    const val ALARM_HOUR = "alarm_hour"
    const val ALARM_MINUTE = "alarm_minute"
    const val ALARM_STATION_ID = "alarm_station_id"
    const val DRIVING_MODE_ENABLED = "driving_mode_enabled"
}
