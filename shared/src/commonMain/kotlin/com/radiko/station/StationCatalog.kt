package com.radiko.station

import com.radiko.i18n.prefectureNamesInAllLanguages
import com.radiko.i18n.regionNamesInAllLanguages

/** 地区选择器里「全国」一项的 ID：浏览全部电台并按大区分组。它不是真实地区，不能拿去认证。 */
const val ALL_AREAS_ID = "ALL"

/** 电台的覆盖范围，用于卡片上的地区标签和「全国」总表的分组。 */
sealed interface StationCoverage {
    data object Nationwide : StationCoverage
    data class Prefecture(val areaId: String) : StationCoverage
    data class Region(val regionId: String) : StationCoverage
}

enum class StationSectionKind {
    /** 普通的单地区列表，没有分组标题。 */
    AREA,
    /** 搜索时，当前浏览地区内的结果。 */
    CURRENT_AREA,
    /** 搜索时，当前浏览地区以外的结果。 */
    OTHER_AREAS,
    /** 「全国」总表里覆盖全部 47 个都道府县的电台。 */
    NATIONWIDE,
    /** 「全国」总表里某个大区的电台。 */
    REGION,
}

data class StationSection(
    val kind: StationSectionKind,
    /** AREA / CURRENT_AREA 时是地区 ID，REGION 时是大区 ID，其余为 null。 */
    val groupId: String?,
    val stations: List<Station>,
) {
    val key: String get() = "${kind.name}:${groupId.orEmpty()}"

    /** 这一组里的电台不属于当前浏览地区，卡片上要标出它们来自哪里。 */
    val showsCoverage: Boolean get() = kind == StationSectionKind.OTHER_AREAS
}

/**
 * 把 [StationRegistry] 里的全部电台整理成首页要显示的分组：单地区列表、全局搜索结果、「全国」总表。
 * Web 端的 `stationCatalog.ts` 是同一套规则，两边改动时要保持一致。
 */
object StationCatalog {
    private val allAreaIds = PrefectureData.allPrefectures.map { it.id }.toSet()
    private val prefectureIndex = PrefectureData.allPrefectures.withIndex().associate { (index, prefecture) -> prefecture.id to index }
    private val regionIndex = PrefectureData.regions.withIndex().associate { (index, region) -> region.id to index }
    private val regionByArea = PrefectureData.regions
        .flatMap { region -> PrefectureData.prefecturesForRegion(region.id).map { it.id to region.id } }
        .toMap()

    /**
     * 由两部分组成的大区名（「北海道・東北」「Hokkaido / Tohoku」）里，每一部分各自对应的都道府县。
     * 各语言的名字都按同样顺序写，所以搜「東北」「Tohoku」只命中东北六县，搜「北海道」不会带出东北。
     */
    private val compositeRegionParts: Map<String, List<Set<String>>> = mapOf(
        "hokkaido-tohoku" to listOf(setOf("JP1"), setOf("JP2", "JP3", "JP4", "JP5", "JP6", "JP7")),
        "hokuriku-koushinetsu" to listOf(setOf("JP16", "JP17", "JP18"), setOf("JP15", "JP19", "JP20")),
        "chugoku-shikoku" to listOf(setOf("JP31", "JP32", "JP33", "JP34", "JP35"), setOf("JP36", "JP37", "JP38", "JP39")),
        "kyushu" to listOf(setOf("JP40", "JP41", "JP42", "JP43", "JP44", "JP45", "JP46"), setOf("JP47")),
    )

    /** 可被搜索的地名（都道府县、大区或大区的一半）及其对应的都道府县。名字已做 [normalizeForSearch]。 */
    private class AreaSearchGroup(val names: List<String>, val areaIds: Set<String>)

    private val areaSearchGroups: List<AreaSearchGroup> by lazy {
        buildList {
            PrefectureData.allPrefectures.forEach { prefecture ->
                add(AreaSearchGroup(searchNames(prefectureNamesInAllLanguages(prefecture.id) + prefecture.name), setOf(prefecture.id)))
            }
            PrefectureData.regions.forEach { region ->
                val names = regionNamesInAllLanguages(region.id) + region.name
                val parts = compositeRegionParts[region.id]
                if (parts == null) {
                    val areaIds = PrefectureData.prefecturesForRegion(region.id).map { it.id }.toSet()
                    add(AreaSearchGroup(searchNames(names), areaIds))
                } else {
                    parts.forEachIndexed { index, areaIds ->
                        add(AreaSearchGroup(searchNames(names.mapNotNull { it.split('・', '/', '／').getOrNull(index) }), areaIds))
                    }
                }
            }
        }
    }

    /** 全国台、各大区依次排列，大区内按覆盖的第一个都道府县、再按台号排。 */
    private val catalogOrder: Comparator<Station> = compareBy<Station>(
        { groupIndex(it) },
        { firstPrefectureIndex(it) },
        { it.id },
    )

    fun isNationwide(station: Station): Boolean = station.areaIds.containsAll(allAreaIds)

    fun coverage(station: Station): StationCoverage {
        if (isNationwide(station)) {
            return StationCoverage.Nationwide
        }
        val singleArea = station.areaIds.singleOrNull()
        if (singleArea != null) {
            return StationCoverage.Prefecture(singleArea)
        }
        return primaryRegionId(station)?.let(StationCoverage::Region)
            ?: StationCoverage.Prefecture(station.areaIds.first())
    }

    /** 电台的收听地区，按地区选择器里的顺序排好。 */
    fun sortedAreaIds(station: Station): List<String> =
        station.areaIds.sortedBy { prefectureIndex[it] ?: Int.MAX_VALUE }

    /**
     * 要拿到这些电台的当前节目，最少该请求哪几个地区（贪心近似）。
     * 覆盖数相同时优先 [preferredAreaIds] 里靠前的地区，其次按地区选择器顺序。
     */
    fun coveringAreas(stations: Collection<Station>, preferredAreaIds: List<String> = emptyList()): List<String> {
        fun rank(areaId: String): Int {
            val preferredIndex = preferredAreaIds.indexOf(areaId)
            return if (preferredIndex >= 0) {
                preferredIndex
            } else {
                preferredAreaIds.size + (prefectureIndex[areaId] ?: (Int.MAX_VALUE - preferredAreaIds.size))
            }
        }

        var uncovered = stations.distinctBy { it.id }
        val chosen = mutableListOf<String>()
        while (uncovered.isNotEmpty()) {
            val best = uncovered.flatMap { it.areaIds }.distinct()
                .minWithOrNull(
                    compareByDescending<String> { areaId -> uncovered.count { areaId in it.areaIds } }
                        .thenBy { rank(it) },
                )
                ?: break
            chosen += best
            uncovered = uncovered.filterNot { best in it.areaIds }
        }
        return chosen
    }

    /**
     * @param areaId 当前浏览的地区，可以是 [ALL_AREAS_ID]。
     * @param query 搜索词；为空时返回普通列表。搜索永远覆盖全部电台。
     */
    fun sections(areaId: String, query: String): List<StationSection> {
        val normalizedQuery = normalizeForSearch(query)
        if (normalizedQuery.isEmpty()) {
            return if (areaId == ALL_AREAS_ID) {
                groupByRegion(StationRegistry.allStations, scores = emptyMap())
            } else {
                listOf(StationSection(StationSectionKind.AREA, areaId, StationRegistry.getStationsForArea(areaId)))
                    .filter { it.stations.isNotEmpty() }
            }
        }

        val matchedAreaIds = areasNamed(normalizedQuery)
        val scores = StationRegistry.allStations
            .mapNotNull { station -> matchScore(station, normalizedQuery, matchedAreaIds)?.let { station.id to it } }
            .toMap()
        val matches = StationRegistry.allStations.filter { it.id in scores }
        if (areaId == ALL_AREAS_ID) {
            return groupByRegion(matches, scores)
        }

        val order = compareBy<Station> { scores.getValue(it.id) }.then(catalogOrder)
        val (inArea, elsewhere) = matches.partition { areaId in it.areaIds }
        return listOf(
            StationSection(StationSectionKind.CURRENT_AREA, areaId, inArea.sortedWith(order)),
            StationSection(StationSectionKind.OTHER_AREAS, null, elsewhere.sortedWith(order)),
        ).filter { it.stations.isNotEmpty() }
    }

    private fun groupByRegion(stations: List<Station>, scores: Map<String, Int>): List<StationSection> {
        val order = compareBy<Station> { scores[it.id] ?: 0 }.then(catalogOrder)
        val (nationwide, regional) = stations.partition(::isNationwide)
        val byRegion = regional.groupBy { primaryRegionId(it) }
        return buildList {
            add(StationSection(StationSectionKind.NATIONWIDE, null, nationwide.sortedWith(order)))
            PrefectureData.regions.forEach { region ->
                add(StationSection(StationSectionKind.REGION, region.id, byRegion[region.id].orEmpty().sortedWith(order)))
            }
        }.filter { it.stations.isNotEmpty() }
    }

    /**
     * 搜索词点名的都道府县（5 种语言的都道府县名、大区名都认）。
     * 单个英文字母太泛（f 会命中 Fukuoka、Fukui……），不当地名用。
     */
    private fun areasNamed(query: String): Set<String> {
        if (query.length < 2 && query.all { it.code < 0x80 }) return emptySet()
        return areaSearchGroups
            .filter { group -> group.names.any { it.startsWith(query) } }
            .flatMapTo(mutableSetOf()) { it.areaIds }
    }

    /**
     * 0：台号或台名以搜索词开头；1：台号或台名包含搜索词；2：能在搜索词点名的地区收听。
     * 不匹配时返回 null。
     */
    private fun matchScore(station: Station, query: String, matchedAreaIds: Set<String>): Int? {
        val id = normalizeForSearch(station.id)
        val name = normalizeForSearch(station.name)
        if (id.startsWith(query) || name.startsWith(query)) return 0
        if (id.contains(query) || name.contains(query)) return 1
        // 全国台在每个地区都收得到，按地区名搜时不列出，否则每次都混进同样几个台。
        if (isNationwide(station)) return null
        return if (station.areaIds.any { it in matchedAreaIds }) 2 else null
    }

    /** 覆盖都道府县最多的大区；数量相同时取靠前的大区。 */
    private fun primaryRegionId(station: Station): String? = station.areaIds
        .mapNotNull { regionByArea[it] }
        .groupingBy { it }
        .eachCount()
        .entries
        .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { regionIndex[it.key] ?: Int.MAX_VALUE })
        .firstOrNull()
        ?.key

    private fun groupIndex(station: Station): Int =
        if (isNationwide(station)) 0 else 1 + (primaryRegionId(station)?.let { regionIndex[it] } ?: regionIndex.size)

    private fun firstPrefectureIndex(station: Station): Int {
        val regionId = if (isNationwide(station)) null else primaryRegionId(station)
        return station.areaIds
            .filter { regionId == null || regionByArea[it] == regionId }
            .minOfOrNull { prefectureIndex[it] ?: Int.MAX_VALUE }
            ?: Int.MAX_VALUE
    }

    private fun searchNames(names: List<String>): List<String> = names
        .map(::normalizeForSearch)
        .filter { it.isNotEmpty() }
        .distinct()
}

/** 全角英数转半角、去首尾空白、转小写，让「ＦＭ」和「fm」、「ＨＢＣ」和「hbc」能互相匹配。 */
internal fun normalizeForSearch(text: String): String = buildString(text.length) {
    text.forEach { char ->
        append(
            when (char) {
                in '！'..'～' -> (char.code - 0xFEE0).toChar()
                '　' -> ' '
                else -> char
            },
        )
    }
}.trim().lowercase()
