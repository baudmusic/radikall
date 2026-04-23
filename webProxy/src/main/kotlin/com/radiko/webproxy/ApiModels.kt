package com.radiko.webproxy

import com.radiko.i18n.AppLocalizer
import com.radiko.settings.AppLanguage
import com.radiko.station.OnAirSong
import com.radiko.station.Prefecture
import com.radiko.station.ProgramEntry
import com.radiko.station.Region
import com.radiko.station.Station
import com.radiko.station.logoUrl
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BootstrapResponse(
    val defaultAreaId: String,
    val supportedLanguages: List<String>,
    val featureFlags: FeatureFlagsResponse,
    val regions: List<RegionSummary>,
    val prefectures: List<PrefectureSummary>,
    val stations: List<StationSummary>,
)

@Serializable
data class FeatureFlagsResponse(
    val autoPlayOnLaunch: Boolean,
    val backgroundPlaybackBestEffort: Boolean,
    val alarms: Boolean,
    val pushReminder: Boolean,
    val sleepTimer: Boolean,
    val wifiOnlyPlayback: Boolean,
    val confirmMobileDataPlayback: Boolean,
)

@Serializable
data class LocalizedTextResponse(
    @SerialName("zh-CN") val zhCn: String,
    @SerialName("zh-TW") val zhTw: String,
    val en: String,
    val ja: String,
    val ko: String,
)

@Serializable
data class RegionSummary(
    val id: String,
    val names: LocalizedTextResponse,
)

@Serializable
data class PrefectureSummary(
    val id: String,
    val regionId: String,
    val areaCode: String,
    val names: LocalizedTextResponse,
)

@Serializable
data class StationSummary(
    val id: String,
    val name: String,
    val areaIds: List<String>,
    val logoUrl: String,
)

@Serializable
data class NowPlayingResponse(
    val stationId: String,
    val currentProgram: ProgramEntry?,
    val currentSong: OnAirSong?,
    val currentProgramSongs: List<OnAirSong>,
    val recentSongs: List<OnAirSong>,
)

@Serializable
data class ScheduleResponse(
    val stationId: String,
    val todayPrograms: List<ProgramEntry>,
    val weeklyPrograms: List<ProgramEntry>,
)

@Serializable
data class LiveSessionRequest(
    val stationId: String,
    val preferredAreaId: String,
)

@Serializable
data class LiveSessionResponse(
    val sessionId: String,
    val stationId: String,
    val resolvedAreaId: String,
    val streamUrl: String,
    val fallbackStreamUrl: String?,
    val currentProgram: ProgramEntry?,
    val expiresAtEpochMillis: Long,
)

@Serializable
data class ErrorResponse(
    val message: String,
)

fun List<Station>.toStationSummaries(): List<StationSummary> = map { station ->
    StationSummary(
        id = station.id,
        name = station.name,
        areaIds = station.areaIds,
        logoUrl = station.logoUrl,
    )
}

fun Region.toSummary(): RegionSummary = RegionSummary(
    id = id,
    names = localizedText { language ->
        AppLocalizer.strings(language).regionName(id, name)
    },
)

fun Prefecture.toSummary(regionId: String, areaCode: String): PrefectureSummary = PrefectureSummary(
    id = id,
    regionId = regionId,
    areaCode = areaCode,
    names = localizedText { language ->
        AppLocalizer.strings(language).prefectureName(id, name)
    },
)

private fun localizedText(valueProvider: (AppLanguage) -> String): LocalizedTextResponse = LocalizedTextResponse(
    zhCn = valueProvider(AppLanguage.SIMPLIFIED_CHINESE),
    zhTw = valueProvider(AppLanguage.TRADITIONAL_CHINESE),
    en = valueProvider(AppLanguage.ENGLISH),
    ja = valueProvider(AppLanguage.JAPANESE),
    ko = valueProvider(AppLanguage.KOREAN),
)
