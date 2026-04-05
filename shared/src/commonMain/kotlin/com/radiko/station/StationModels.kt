package com.radiko.station

import kotlinx.serialization.Serializable

@Serializable
data class Station(
    val id: String,
    val name: String,
    val areaIds: List<String>,
)

val Station.logoUrl: String
    get() = "https://radiko.jp/v2/static/station/logo/$id/224x100.png"

@Serializable
data class Prefecture(
    val id: String,
    val name: String,
)

@Serializable
data class Region(
    val id: String,
    val name: String,
)

@Serializable
data class ProgramEntry(
    val stationId: String,
    val title: String,
    val description: String,
    val performer: String?,
    val startAt: String,
    val endAt: String,
    val info: String?,
    val imageUrl: String?,
    val url: String?,
)

@Serializable
data class OnAirSong(
    val title: String,
    val artist: String,
    val imageUrl: String?,
    val stampDate: String,
)
