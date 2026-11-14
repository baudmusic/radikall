package com.radiko.text

import com.radiko.station.StationRegistry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TextWidthTest {
    @Test
    fun narrowsAlphanumericsInStationNames() {
        assertEquals("HBCラジオ", "ＨＢＣラジオ".narrowAlphanumerics())
        assertEquals("μFM", "μＦＭ".narrowAlphanumerics())
        assertEquals("FM GIFU", "ＦＭ ＧＩＦＵ".narrowAlphanumerics())
        assertEquals("レディオキューブ FM三重", "レディオキューブ ＦＭ三重".narrowAlphanumerics())
    }

    @Test
    fun narrowsSymbolsAndSpacesOnlyBetweenAlphanumerics() {
        assertEquals("TOKYO BEATS", "ＴＯＫＹＯ　ＢＥＡＴＳ".narrowAlphanumerics())
        assertEquals("Vol.2", "Ｖｏｌ．２".narrowAlphanumerics())
        assertEquals("B'z", "Ｂ＇ｚ".narrowAlphanumerics())
        assertEquals("ラジオ深夜便▽深夜便NEXT・後半", "ラジオ深夜便▽深夜便ＮＥＸＴ・後半".narrowAlphanumerics())
    }

    @Test
    fun keepsJapanesePunctuationSpacingAndBrackets() {
        val unchanged = listOf(
            "ブルアカらじお！夜に騒いじゃってごめんなさい！",
            "ニチレイプレゼンツ　オードリーのオールナイトニッポン",
            "Rhythm Station　エフエム山形",
            "モーニング娘。’26のモーニング女学院～放課後ミーティング～",
        )
        unchanged.forEach { assertEquals(it, it.narrowAlphanumerics()) }

        assertEquals(
            "ポルトガル語講座　ステップアップ　アイルトンのニッポン（6）",
            "ポルトガル語講座　ステップアップ　アイルトンのニッポン（６）".narrowAlphanumerics(),
        )
        assertEquals("～ANR大人の秘密基地～", "～ＡＮＲ大人の秘密基地～".narrowAlphanumerics())
    }

    @Test
    fun registryStationNamesHaveNoFullwidthAlphanumerics() {
        val fullwidth = Regex("[０-９Ａ-Ｚａ-ｚ]")
        val offenders = StationRegistry.allStations.filter { fullwidth.containsMatchIn(it.name) }
        assertTrue(offenders.isEmpty(), "still fullwidth: ${offenders.map { it.name }}")
        assertEquals("HBCラジオ", StationRegistry.getStation("HBC")?.name)
    }
}
