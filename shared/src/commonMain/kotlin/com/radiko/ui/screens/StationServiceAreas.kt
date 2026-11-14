package com.radiko.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.radiko.i18n.appStrings
import com.radiko.station.ALL_AREAS_ID
import com.radiko.station.Station
import com.radiko.station.StationCatalog
import com.radiko.ui.theme.radikoPalette

/**
 * 详情页里的「可收听地区」：点某个地区就切到那里浏览。
 * 全国台只显示一个「全国」，点了打开全国总表。
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StationServiceAreas(
    station: Station,
    currentAreaId: String,
    onSelectArea: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = appStrings()
    val areaIds = if (StationCatalog.isNationwide(station)) {
        listOf(ALL_AREAS_ID)
    } else {
        StationCatalog.sortedAreaIds(station)
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = strings.serviceAreasLabel,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = radikoPalette().onStage,
        )
        FlowRow(modifier = Modifier.fillMaxWidth()) {
            areaIds.forEach { areaId ->
                GlowChip(
                    text = strings.areaLabel(areaId),
                    selected = areaId == currentAreaId,
                    onClick = { onSelectArea(areaId) },
                )
            }
        }
    }
}
