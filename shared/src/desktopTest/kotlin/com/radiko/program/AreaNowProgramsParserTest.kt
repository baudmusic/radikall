package com.radiko.program

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AreaNowProgramsParserTest {
    // 按 api.radiko.jp/program/v3/now/{area}.xml 的真实结构裁剪；FMJ 的两档故意倒序，验证会按开始时间排序。
    private val xml = """
        <?xml version="1.0" encoding="UTF-8"?>
        <radiko>
          <ttl>300</ttl>
          <srvtime>1794668682</srvtime>
          <stations>
            <station id="TBS">
              <name>TBSラジオ</name>
              <progs>
                <date>20261114</date>
                <prog id="1" master_id="" ft="20261115000000" to="20261115010000" ftl="2400" tol="2500" dur="3600">
                  <title>俺達には土曜日しかない</title>
                  <url>https://www.tbsradio.jp/</url>
                  <desc></desc>
                  <info>&lt;br&gt;メール</info>
                  <pfm>綾小路 翔</pfm>
                  <img>https://program-static.cf.radiko.jp/45pq4zag79.jpg</img>
                  <tag><item><name>tag</name></item></tag>
                  <genre><program id="P006"><name>バラエティ</name></program></genre>
                </prog>
                <prog id="2" master_id="" ft="20261115010000" to="20261115020000" ftl="2500" tol="2600" dur="3600">
                  <title>Rock &amp; Roll&apos;s Night</title>
                  <pfm>A &amp; B</pfm>
                  <img>https://program-static.cf.radiko.jp/x2t1bey92r.jpg</img>
                </prog>
              </progs>
            </station>
            <station id="FMJ">
              <name>J-WAVE</name>
              <progs>
                <prog id="4" ft="20261115010000" to="20261115030000"><title>後</title></prog>
                <prog id="3" ft="20261115000000" to="20261115010000"><title>前</title></prog>
              </progs>
            </station>
          </stations>
        </radiko>
    """.trimIndent()

    @Test
    fun parsesEveryStationWithCurrentAndNextProgram() {
        val feed = parseAreaNowPrograms("JP13", xml)

        assertEquals("JP13", feed.areaId)
        assertEquals(300, feed.ttlSeconds)
        assertEquals(setOf("TBS", "FMJ"), feed.programsByStation.keys)

        val tbs = feed.programsByStation.getValue("TBS")
        assertEquals(2, tbs.size)
        assertEquals("俺達には土曜日しかない", tbs[0].title)
        assertEquals("綾小路 翔", tbs[0].performer)
        assertEquals("https://program-static.cf.radiko.jp/45pq4zag79.jpg", tbs[0].imageUrl)
        assertEquals("20261115000000", tbs[0].startAt)
        assertEquals("20261115010000", tbs[0].endAt)
        // 简介是转义过的 HTML，原样保留给描述显示逻辑。
        assertEquals("&lt;br&gt;メール", tbs[0].info)
    }

    @Test
    fun decodesEntitiesInTitleAndPerformer() {
        val program = parseAreaNowPrograms("JP13", xml).programsByStation.getValue("TBS")[1]
        assertEquals("Rock & Roll's Night", program.title)
        assertEquals("A & B", program.performer)
    }

    @Test
    fun sortsProgramsByStartTime() {
        val fmj = parseAreaNowPrograms("JP13", xml).programsByStation.getValue("FMJ")
        assertEquals(listOf("前", "後"), fmj.map { it.title })
        assertNull(fmj[0].performer)
    }

    @Test
    fun fallsBackToDefaultTtl() {
        assertEquals(300, parseAreaNowPrograms("JP13", "<radiko><stations></stations></radiko>").ttlSeconds)
    }

    @Test
    fun decodeXmlTextHandlesNumericAndUnknownEntities() {
        assertEquals("a&b<c>\"d'é😀&unknown;", decodeXmlText("a&amp;b&lt;c&gt;&quot;d&apos;&#233;&#x1F600;&unknown;"))
    }
}
