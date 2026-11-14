package com.radiko.ui.viewmodel

import com.radiko.station.StationCatalog
import com.radiko.station.StationSection
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class StationListState(
    /** 正在浏览的地区，可以是 [com.radiko.station.ALL_AREAS_ID]。 */
    val currentAreaId: String = "JP13",
    val searchQuery: String = "",
    val sections: List<StationSection> = StationCatalog.sections(areaId = "JP13", query = ""),
) {
    val isEmpty: Boolean get() = sections.all { it.stations.isEmpty() }
}

class StationListViewModel {
    private val _state = MutableStateFlow(StationListState())
    val state: StateFlow<StationListState> = _state.asStateFlow()

    fun switchArea(areaId: String) {
        _state.update { current ->
            current.copy(
                currentAreaId = areaId,
                sections = StationCatalog.sections(areaId = areaId, query = current.searchQuery),
            )
        }
    }

    fun updateSearchQuery(query: String) {
        _state.update { current ->
            current.copy(
                searchQuery = query,
                sections = StationCatalog.sections(areaId = current.currentAreaId, query = query),
            )
        }
    }
}
