package com.radiko.device

import kotlin.math.pow
import kotlin.math.round
import kotlin.random.Random

object GpsSpoofing {
    private val areaList = listOf(
        "北海道", "青森", "岩手", "宮城", "秋田", "山形", "福島", "茨城",
        "栃木", "群馬", "埼玉", "千葉", "東京", "神奈川", "新潟", "富山",
        "石川", "福井", "山梨", "長野", "岐阜", "静岡", "愛知", "三重",
        "滋賀", "京都", "大阪", "兵庫", "奈良", "和歌山", "鳥取", "島根",
        "岡山", "広島", "山口", "徳島", "香川", "愛媛", "高知", "福岡",
        "佐賀", "長崎", "熊本", "大分", "宮崎", "鹿児島", "沖縄"
    )

    private val coordinates: Map<String, Pair<Double, Double>> = mapOf(
        "北海道" to Pair(43.064615, 141.346807),
        "青森" to Pair(40.824308, 140.739998),
        "岩手" to Pair(39.703619, 141.152684),
        "宮城" to Pair(38.268837, 140.8721),
        "秋田" to Pair(39.718614, 140.102364),
        "山形" to Pair(38.240436, 140.363633),
        "福島" to Pair(37.750299, 140.467551),
        "茨城" to Pair(36.341811, 140.446793),
        "栃木" to Pair(36.565725, 139.883565),
        "群馬" to Pair(36.390668, 139.060406),
        "埼玉" to Pair(35.856999, 139.648849),
        "千葉" to Pair(35.605057, 140.123306),
        "東京" to Pair(35.689488, 139.691706),
        "神奈川" to Pair(35.447507, 139.642345),
        "新潟" to Pair(37.902552, 139.023095),
        "富山" to Pair(36.695291, 137.211338),
        "石川" to Pair(36.594682, 136.625573),
        "福井" to Pair(36.065178, 136.221527),
        "山梨" to Pair(35.664158, 138.568449),
        "長野" to Pair(36.651299, 138.180956),
        "岐阜" to Pair(35.391227, 136.722291),
        "静岡" to Pair(34.97712, 138.383084),
        "愛知" to Pair(35.180188, 136.906565),
        "三重" to Pair(34.730283, 136.508588),
        "滋賀" to Pair(35.004531, 135.86859),
        "京都" to Pair(35.021247, 135.755597),
        "大阪" to Pair(34.686297, 135.519661),
        "兵庫" to Pair(34.691269, 135.183071),
        "奈良" to Pair(34.685334, 135.832742),
        "和歌山" to Pair(34.225987, 135.167509),
        "鳥取" to Pair(35.503891, 134.237736),
        "島根" to Pair(35.472295, 133.0505),
        "岡山" to Pair(34.661751, 133.934406),
        "広島" to Pair(34.39656, 132.459622),
        "山口" to Pair(34.185956, 131.470649),
        "徳島" to Pair(34.065718, 134.55936),
        "香川" to Pair(34.340149, 134.043444),
        "愛媛" to Pair(33.841624, 132.765681),
        "高知" to Pair(33.559706, 133.531079),
        "福岡" to Pair(33.606576, 130.418297),
        "佐賀" to Pair(33.249442, 130.299794),
        "長崎" to Pair(32.744839, 129.873756),
        "熊本" to Pair(32.789827, 130.741667),
        "大分" to Pair(33.238172, 131.612619),
        "宮崎" to Pair(31.911096, 131.423893),
        "鹿児島" to Pair(31.560146, 130.557978),
        "沖縄" to Pair(26.2124, 127.680932)
    )

    fun prefectureName(areaId: String): String {
        val index = areaId.removePrefix("JP").toInt() - 1
        return areaList[index]
    }

    fun generate(areaId: String, random: Random = Random.Default): String {
        val prefecture = prefectureName(areaId)
        val (baseLat, baseLng) = coordinates.getValue(prefecture)
        val lat = baseLat + random.nextDouble() / 40.0 * if (random.nextBoolean()) 1 else -1
        val lng = baseLng + random.nextDouble() / 40.0 * if (random.nextBoolean()) 1 else -1
        return "${lat.toFixed(6)},${lng.toFixed(6)},gps"
    }
}

private fun Double.toFixed(decimals: Int): String {
    val factor = 10.0.pow(decimals)
    val rounded = round(this * factor) / factor
    val text = rounded.toString()
    val parts = text.split('.')
    if (parts.size == 1) {
        return text + "." + "0".repeat(decimals)
    }
    val fraction = parts[1].padEnd(decimals, '0')
    return parts[0] + "." + fraction.take(decimals)
}
