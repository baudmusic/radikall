package com.radiko.station

object PrefectureData {
    val regions = listOf(
        Region(id = "hokkaido-tohoku", name = "北海道・東北"),
        Region(id = "kanto", name = "関東"),
        Region(id = "hokuriku-koushinetsu", name = "北陸・甲信越"),
        Region(id = "chubu", name = "中部"),
        Region(id = "kinki", name = "近畿"),
        Region(id = "chugoku-shikoku", name = "中国・四国"),
        Region(id = "kyushu", name = "九州・沖縄")
    )

    private val prefecturesByRegion: Map<String, List<Prefecture>> = mapOf(
        "hokkaido-tohoku" to listOf(
            Prefecture(id = "JP1", name = "北海道"),
            Prefecture(id = "JP2", name = "青森"),
            Prefecture(id = "JP3", name = "岩手"),
            Prefecture(id = "JP4", name = "宮城"),
            Prefecture(id = "JP5", name = "秋田"),
            Prefecture(id = "JP6", name = "山形"),
            Prefecture(id = "JP7", name = "福島")
        ),
        "kanto" to listOf(
            Prefecture(id = "JP8", name = "茨城"),
            Prefecture(id = "JP9", name = "栃木"),
            Prefecture(id = "JP10", name = "群馬"),
            Prefecture(id = "JP11", name = "埼玉"),
            Prefecture(id = "JP12", name = "千葉"),
            Prefecture(id = "JP13", name = "東京"),
            Prefecture(id = "JP14", name = "神奈川")
        ),
        "hokuriku-koushinetsu" to listOf(
            Prefecture(id = "JP15", name = "新潟"),
            Prefecture(id = "JP19", name = "山梨"),
            Prefecture(id = "JP20", name = "長野"),
            Prefecture(id = "JP17", name = "石川"),
            Prefecture(id = "JP16", name = "富山"),
            Prefecture(id = "JP18", name = "福井")
        ),
        "chubu" to listOf(
            Prefecture(id = "JP23", name = "愛知"),
            Prefecture(id = "JP21", name = "岐阜"),
            Prefecture(id = "JP22", name = "静岡"),
            Prefecture(id = "JP24", name = "三重")
        ),
        "kinki" to listOf(
            Prefecture(id = "JP27", name = "大阪"),
            Prefecture(id = "JP28", name = "兵庫"),
            Prefecture(id = "JP26", name = "京都"),
            Prefecture(id = "JP25", name = "滋賀"),
            Prefecture(id = "JP29", name = "奈良"),
            Prefecture(id = "JP30", name = "和歌山")
        ),
        "chugoku-shikoku" to listOf(
            Prefecture(id = "JP33", name = "岡山"),
            Prefecture(id = "JP34", name = "広島"),
            Prefecture(id = "JP31", name = "鳥取"),
            Prefecture(id = "JP32", name = "島根"),
            Prefecture(id = "JP35", name = "山口"),
            Prefecture(id = "JP37", name = "香川"),
            Prefecture(id = "JP36", name = "徳島"),
            Prefecture(id = "JP38", name = "愛媛"),
            Prefecture(id = "JP39", name = "高知")
        ),
        "kyushu" to listOf(
            Prefecture(id = "JP40", name = "福岡"),
            Prefecture(id = "JP41", name = "佐賀"),
            Prefecture(id = "JP42", name = "長崎"),
            Prefecture(id = "JP43", name = "熊本"),
            Prefecture(id = "JP44", name = "大分"),
            Prefecture(id = "JP45", name = "宮崎"),
            Prefecture(id = "JP46", name = "鹿児島"),
            Prefecture(id = "JP47", name = "沖縄")
        )
    )

    val areaCodeMap: Map<String, String> = mapOf(
        "JP1" to "HOKKAIDO",
        "JP2" to "AOMORI",
        "JP3" to "IWATE",
        "JP4" to "MIYAGI",
        "JP5" to "AKITA",
        "JP6" to "YAMAGATA",
        "JP7" to "FUKUSHIMA",
        "JP8" to "IBARAKI",
        "JP9" to "TOCHIGI",
        "JP10" to "GUNMA",
        "JP11" to "SAITAMA",
        "JP12" to "CHIBA",
        "JP13" to "TOKYO",
        "JP14" to "KANAGAWA",
        "JP15" to "NIIGATA",
        "JP16" to "TOYAMA",
        "JP17" to "ISHIKAWA",
        "JP18" to "FUKUI",
        "JP19" to "YAMANASHI",
        "JP20" to "NAGANO",
        "JP21" to "GIFU",
        "JP22" to "SHIZUOKA",
        "JP23" to "AICHI",
        "JP24" to "MIE",
        "JP25" to "SHIGA",
        "JP26" to "KYOTO",
        "JP27" to "OSAKA",
        "JP28" to "HYOGO",
        "JP29" to "NARA",
        "JP30" to "WAKAYAMA",
        "JP31" to "TOTTORI",
        "JP32" to "SHIMANE",
        "JP33" to "OKAYAMA",
        "JP34" to "HIROSHIMA",
        "JP35" to "YAMAGUCHI",
        "JP36" to "TOKUSHIMA",
        "JP37" to "KAGAWA",
        "JP38" to "EHIME",
        "JP39" to "KOCHI",
        "JP40" to "FUKUOKA",
        "JP41" to "SAGA",
        "JP42" to "NAGASAKI",
        "JP43" to "KUMAMOTO",
        "JP44" to "OITA",
        "JP45" to "MIYAZAKI",
        "JP46" to "KAGOSHIMA",
        "JP47" to "OKINAWA"
    )

    val allPrefectures: List<Prefecture> = regions.flatMap { prefecturesForRegion(it.id) }

    fun prefecturesForRegion(regionId: String): List<Prefecture> = prefecturesByRegion[regionId].orEmpty()

    fun prefecture(areaId: String): Prefecture? = allPrefectures.find { it.id == areaId }

    fun regionForArea(areaId: String): Region? = regions.find { region ->
        prefecturesForRegion(region.id).any { it.id == areaId }
    }
}
