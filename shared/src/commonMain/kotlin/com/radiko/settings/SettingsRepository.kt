package com.radiko.settings

import com.radiko.i18n.AppLocalizer
import com.radiko.platform.PlatformAlarmScheduler
import com.radiko.platform.PlatformLocaleInfo
import com.radiko.platform.PlatformPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsRepository {
    private val _state = MutableStateFlow(load())
    val state: StateFlow<AppSettings> = _state.asStateFlow()

    init {
        PlatformAlarmScheduler.sync(_state.value)
    }

    fun updateLanguage(language: AppLanguage) = persist {
        it.copy(language = language)
    }

    fun updateStartupAreaMode(mode: StartupAreaMode) = persist {
        it.copy(startupAreaMode = mode)
    }

    fun updateFixedAreaId(areaId: String) = persist {
        it.copy(fixedAreaId = areaId)
    }

    fun rememberLastArea(areaId: String) = persist {
        it.copy(lastAreaId = areaId)
    }

    fun rememberLastStation(stationId: String?) = persist {
        it.copy(lastStationId = stationId)
    }

    fun updateAutoPlayOnLaunch(enabled: Boolean) = persist {
        it.copy(autoPlayOnLaunch = enabled)
    }

    fun updateBackgroundPlayback(enabled: Boolean) = persist {
        it.copy(backgroundPlaybackEnabled = enabled)
    }

    fun updateWifiOnlyPlayback(enabled: Boolean) = persist {
        it.copy(wifiOnlyPlayback = enabled)
    }

    fun updateConfirmMobileDataPlayback(enabled: Boolean) = persist {
        it.copy(confirmMobileDataPlayback = enabled)
    }

    fun updateThemeMode(mode: AppThemeMode) = persist {
        it.copy(themeMode = mode)
    }

    fun updateCloseButtonMode(mode: CloseButtonMode) = persist {
        it.copy(closeButtonMode = mode)
    }

    fun updateAudioFocusBehavior(behavior: AudioFocusBehavior) = persist {
        it.copy(audioFocusBehavior = behavior)
    }

    fun updateAlarmEnabled(enabled: Boolean) = persist {
        it.copy(alarmEnabled = enabled)
    }

    fun updateAlarmTime(hour: Int, minute: Int) = persist {
        it.copy(alarmHour = hour, alarmMinute = minute)
    }

    fun updateAlarmStation(stationId: String) = persist {
        it.copy(alarmStationId = stationId)
    }

    fun updateDrivingModeEnabled(enabled: Boolean) = persist {
        it.copy(drivingModeEnabled = enabled)
    }

    private fun persist(transform: (AppSettings) -> AppSettings) {
        val updated = transform(_state.value)
        _state.value = updated
        save(updated)
        PlatformAlarmScheduler.sync(updated)
    }

    private fun load(): AppSettings {
        val defaultLanguage = AppLocalizer.resolveLanguageTag(
            PlatformLocaleInfo.currentLanguageTag(),
        )
        return AppSettings(
            language = PlatformPreferences.getEnum(
                key = SettingsKeys.LANGUAGE,
                defaultValue = defaultLanguage,
            ),
            startupAreaMode = PlatformPreferences.getEnum(
                key = SettingsKeys.STARTUP_AREA_MODE,
                defaultValue = StartupAreaMode.REMEMBER_LAST,
            ),
            fixedAreaId = PlatformPreferences.getString(SettingsKeys.FIXED_AREA_ID, "JP13"),
            lastAreaId = PlatformPreferences.getString(SettingsKeys.LAST_AREA_ID, "JP13"),
            lastStationId = PlatformPreferences.getString(SettingsKeys.LAST_STATION_ID, "").ifBlank { null },
            autoPlayOnLaunch = PlatformPreferences.getBoolean(SettingsKeys.AUTOPLAY_ON_LAUNCH, false),
            backgroundPlaybackEnabled = PlatformPreferences.getBoolean(SettingsKeys.BACKGROUND_PLAYBACK, true),
            wifiOnlyPlayback = PlatformPreferences.getBoolean(SettingsKeys.WIFI_ONLY_PLAYBACK, false),
            confirmMobileDataPlayback = PlatformPreferences.getBoolean(SettingsKeys.MOBILE_DATA_CONFIRM, true),
            themeMode = PlatformPreferences.getEnum(SettingsKeys.THEME_MODE, AppThemeMode.SYSTEM),
            closeButtonMode = PlatformPreferences.getEnum(SettingsKeys.CLOSE_BEHAVIOR, CloseButtonMode.ASK),
            audioFocusBehavior = PlatformPreferences.getEnum(
                SettingsKeys.AUDIO_FOCUS_BEHAVIOR,
                AudioFocusBehavior.DUCK,
            ),
            alarmEnabled = PlatformPreferences.getBoolean(SettingsKeys.ALARM_ENABLED, false),
            alarmHour = PlatformPreferences.getInt(SettingsKeys.ALARM_HOUR, 7),
            alarmMinute = PlatformPreferences.getInt(SettingsKeys.ALARM_MINUTE, 0),
            alarmStationId = PlatformPreferences.getString(SettingsKeys.ALARM_STATION_ID, "FMT"),
            drivingModeEnabled = PlatformPreferences.getBoolean(SettingsKeys.DRIVING_MODE_ENABLED, false),
        )
    }

    private fun save(settings: AppSettings) {
        PlatformPreferences.putString(SettingsKeys.LANGUAGE, settings.language.name)
        PlatformPreferences.putString(SettingsKeys.STARTUP_AREA_MODE, settings.startupAreaMode.name)
        PlatformPreferences.putString(SettingsKeys.FIXED_AREA_ID, settings.fixedAreaId)
        PlatformPreferences.putString(SettingsKeys.LAST_AREA_ID, settings.lastAreaId)
        PlatformPreferences.putString(SettingsKeys.LAST_STATION_ID, settings.lastStationId.orEmpty())
        PlatformPreferences.putBoolean(SettingsKeys.AUTOPLAY_ON_LAUNCH, settings.autoPlayOnLaunch)
        PlatformPreferences.putBoolean(SettingsKeys.BACKGROUND_PLAYBACK, settings.backgroundPlaybackEnabled)
        PlatformPreferences.putBoolean(SettingsKeys.WIFI_ONLY_PLAYBACK, settings.wifiOnlyPlayback)
        PlatformPreferences.putBoolean(SettingsKeys.MOBILE_DATA_CONFIRM, settings.confirmMobileDataPlayback)
        PlatformPreferences.putString(SettingsKeys.THEME_MODE, settings.themeMode.name)
        PlatformPreferences.putString(SettingsKeys.CLOSE_BEHAVIOR, settings.closeButtonMode.name)
        PlatformPreferences.putString(SettingsKeys.AUDIO_FOCUS_BEHAVIOR, settings.audioFocusBehavior.name)
        PlatformPreferences.putBoolean(SettingsKeys.ALARM_ENABLED, settings.alarmEnabled)
        PlatformPreferences.putInt(SettingsKeys.ALARM_HOUR, settings.alarmHour)
        PlatformPreferences.putInt(SettingsKeys.ALARM_MINUTE, settings.alarmMinute)
        PlatformPreferences.putString(SettingsKeys.ALARM_STATION_ID, settings.alarmStationId)
        PlatformPreferences.putBoolean(SettingsKeys.DRIVING_MODE_ENABLED, settings.drivingModeEnabled)
    }
}

private inline fun <reified T : Enum<T>> com.radiko.platform.PlatformPreferences.getEnum(
    key: String,
    defaultValue: T,
): T {
    val raw = getString(key, defaultValue.name)
    return enumValues<T>().firstOrNull { it.name == raw } ?: defaultValue
}
