package com.radiko.ui.viewmodel

import com.radiko.station.Station
import com.radiko.station.StationRegistry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class StationListState(
    val currentAreaId: String = "JP13",
    val searchQuery: String = "",
    val stations: List<Station> = StationRegistry.getStationsForArea("JP13"),
)

class StationListViewModel {
    private val _state = MutableStateFlow(StationListState())
    val state: StateFlow<StationListState> = _state.asStateFlow()

    fun switchArea(areaId: String) {
        _state.update { current ->
            current.copy(
                currentAreaId = areaId,
                stations = stationsFor(areaId = areaId, query = current.searchQuery),
            )
        }
    }

    fun updateSearchQuery(query: String) {
        _state.update { current ->
            current.copy(
                searchQuery = query,
                stations = stationsFor(areaId = current.currentAreaId, query = query),
            )
        }
    }

    fun getStationsForCurrentArea(): List<Station> = _state.value.stations

    fun searchStations(query: String): List<Station> = StationRegistry.search(
        query = query,
        areaId = _state.value.currentAreaId,
    )

    private fun stationsFor(areaId: String, query: String): List<Station> = if (query.isBlank()) {
        StationRegistry.getStationsForArea(areaId)
    } else {
        StationRegistry.search(query = query, areaId = areaId)
    }
}

