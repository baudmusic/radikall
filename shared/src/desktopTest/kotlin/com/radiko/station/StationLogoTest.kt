package com.radiko.station

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class StationLogoTest {
    private val tolerance = 0.01f

    @Test
    fun everyRegisteredStationHasInkMetrics() {
        val missing = StationRegistry.allStations.map { it.id }.filterNot { it in stationLogoInk }
        assertTrue(missing.isEmpty(), "缺少台标测量数据，请运行 node tools/generate_logo_ink.mjs：$missing")
    }

    @Test
    fun everyLogoFitsInsideTheStripAndSolo() {
        StationRegistry.allStations.forEach { station ->
            for ((maxWidth, maxHeight, area) in listOf(Triple(80f, 18f, 720f), Triple(76f, 46f, 2000f))) {
                val box = assertNotNull(fitLogo(station.id, maxWidth, maxHeight, area))
                assertTrue(box.inkWidth <= maxWidth + tolerance && box.inkHeight <= maxHeight + tolerance, "${station.id}: $box")
                assertTrue(box.inkWidth * box.inkHeight <= area + 1f, "${station.id} 超出目标面积：$box")
                // 墨迹框内的整张图偏移后正好覆盖墨迹区域。
                assertTrue(box.imageWidth >= box.inkWidth - tolerance && box.imageHeight >= box.inkHeight - tolerance)
            }
        }
    }

    @Test
    fun wideWordmarkUsesFullWidthAndSquareEmblemHitsHeightCap() {
        // CBC 的墨迹约 8.9:1，上下留白很多：裁掉留白后能撑满 80dp 宽。
        val cbc = fitLogo("CBC", 80f, 18f, 720f)!!
        assertTrue(abs(cbc.inkWidth - 80f) < 0.5f, "CBC: $cbc")
        assertTrue(cbc.offsetY > 0f, "CBC 应把上方透明边挪出框外")
        // μFM 接近方形：顶到 18dp 高度上限。
        val myu = fitLogo("MYUFM", 80f, 18f, 720f)!!
        assertEquals(18f, myu.inkHeight, tolerance)
    }

    @Test
    fun unknownStationFallsBack() {
        assertNull(fitLogo("NOT-A-STATION", 80f, 18f, 720f))
    }
}
