package com.radiko.station

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StationCatalogTest {
    private fun ids(sections: List<StationSection>): List<String> = sections.flatMap { section -> section.stations.map { it.id } }

    private fun stationsIn(areaId: String) = StationRegistry.allStations.filter { areaId in it.areaIds }

    @Test
    fun allJapanListsEveryStationExactlyOnce() {
        val sections = StationCatalog.sections(ALL_AREAS_ID, "")
        val all = ids(sections)
        assertEquals(StationRegistry.allStations.size, all.size)
        assertEquals(all.size, all.toSet().size)
        assertEquals(StationSectionKind.NATIONWIDE, sections.first().kind)
        assertEquals(listOf("JOAK-FM", "RN1", "RN2"), sections.first().stations.map { it.id })
    }

    @Test
    fun plainAreaViewHasNoHeaderSection() {
        val sections = StationCatalog.sections("JP13", "")
        assertEquals(listOf(StationSectionKind.AREA), sections.map { it.kind })
        assertEquals(StationRegistry.getStationsForArea("JP13").map { it.id }, ids(sections))
    }

    @Test
    fun searchingAreaNameFromAnotherAreaListsThatAreaWithoutNationwideStations() {
        val sections = StationCatalog.sections("JP13", "大阪")
        assertEquals(listOf(StationSectionKind.OTHER_AREAS), sections.map { it.kind })
        val found = ids(sections)
        // 名字里带「大阪」的排最前。
        assertEquals(setOf("FMO", "JOBK", "OBC"), found.take(3).toSet())
        assertTrue(found.none { it in setOf("RN1", "RN2", "JOAK-FM") })
        assertEquals(stationsIn("JP27").filterNot { StationCatalog.isNationwide(it) }.map { it.id }.toSet(), found.toSet())
    }

    @Test
    fun compositeRegionNamesOnlyMatchTheirOwnHalf() {
        val hokkaido = ids(StationCatalog.sections("JP13", "北海道")).toSet()
        assertEquals(setOf("HBC", "STV", "AIR-G", "NORTHWAVE", "JOIK"), hokkaido)

        val tohoku = ids(StationCatalog.sections("JP13", "Tohoku")).toSet()
        assertTrue("HBC" !in tohoku, "搜 Tohoku 不应带出北海道")
        assertTrue("JOHK" in tohoku && "TBC" in tohoku)
        assertEquals(tohoku, ids(StationCatalog.sections("JP13", "東北")).toSet())
    }

    @Test
    fun searchIsCaseAndWidthInsensitiveAndSplitsByCurrentArea() {
        val sections = StationCatalog.sections("JP13", "ＮＨＫ")
        assertEquals(listOf(StationSectionKind.CURRENT_AREA, StationSectionKind.OTHER_AREAS), sections.map { it.kind })
        assertEquals(setOf("JOAK", "JOAK-FM"), sections[0].stations.map { it.id }.toSet())
        assertTrue(sections[1].showsCoverage)
    }

    @Test
    fun singleLatinLetterDoesNotMatchAreaNames() {
        val found = ids(StationCatalog.sections("JP13", "f")).toSet()
        // f 只按台号/台名匹配，不会因为 Fukuoka 带出 RKB、KBC。
        assertTrue("RKB" !in found && "KBC" !in found)
    }

    @Test
    fun coveringAreasCoversEveryStation() {
        val areas = StationCatalog.coveringAreas(StationRegistry.allStations)
        StationRegistry.allStations.forEach { station ->
            assertTrue(station.areaIds.any { it in areas }, "${station.id} 没被覆盖")
        }
        assertTrue(areas.size <= 36, "覆盖全部电台用了 ${areas.size} 个地区")
    }

    @Test
    fun coveringAreasPrefersRequestedArea() {
        assertEquals(listOf("JP13"), StationCatalog.coveringAreas(stationsIn("JP13"), listOf("JP13")))
        val tbs = StationRegistry.getStation("TBS")!!
        assertEquals(listOf("JP14"), StationCatalog.coveringAreas(listOf(tbs), listOf("JP14")))
        assertEquals(listOf("JP8"), StationCatalog.coveringAreas(listOf(tbs)))
    }
}
