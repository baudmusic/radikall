package com.radiko.update

import com.radiko.settings.AppLanguage
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AppUpdateTest {
    private val iosBody = """
        # iOS WebApp 1.0.0

        ## 中文

        这个版本把 Radikall 的 iOS Web App 正式公开上线。

        更新亮点：

        - iPhone / iPad 可直接保存到主屏幕使用
        - macOS 可直接用 **Safari** 打开
        - 使用[稳定的公网地址](https://example.com)

        使用方式：

        - 用 Safari 打开这个网址

        ## English

        Highlights:

        - iPhone / iPad support
        - macOS support
    """.trimIndent()

    // 仓库现状：最新的是不带安装包的 iOS Web App 发布，原生安装包在更早的 1.0.0 里。
    private val iosRelease = GitHubRelease(
        tagName = "v1.0.0",
        htmlUrl = "https://github.com/baudmusic/radikall/releases/tag/v1.0.0",
        publishedAt = "2026-04-23T18:27:47Z",
        body = iosBody,
    )
    private val native100 = nativeRelease("1.0.0", "2026-04-06T08:18:22Z")

    private fun nativeRelease(version: String, publishedAt: String, body: String = "") = GitHubRelease(
        tagName = version,
        htmlUrl = "https://github.com/baudmusic/radikall/releases/tag/$version",
        publishedAt = publishedAt,
        body = body,
        assets = listOf(
            GitHubAsset("Radikall-release-$version.apk", "https://dl/apk/$version", 21_292_644),
            GitHubAsset("Radikall-Setup-$version.exe", "https://dl/exe/$version", 71_352_481),
        ),
    )

    @Test
    fun noUpdateWhenOnlyNewerReleaseHasNoInstaller() {
        assertNull(selectUpdate(listOf(iosRelease, native100), "1.0.0", UpdateTarget.ANDROID_APK))
        assertNull(selectUpdate(listOf(iosRelease, native100), "1.0.1", UpdateTarget.WINDOWS_INSTALLER))
    }

    @Test
    fun picksNewestReleaseWithInstallerForPlatform() {
        val releases = listOf(nativeRelease("1.0.1", "2026-11-20T00:00:00Z", iosBody), iosRelease, native100)

        val android = assertNotNull(selectUpdate(releases, "1.0.0", UpdateTarget.ANDROID_APK))
        assertEquals("1.0.1", android.version)
        assertEquals("https://dl/apk/1.0.1", android.downloadUrl)
        assertEquals("2026-11-20", android.publishedDate)

        val windows = assertNotNull(selectUpdate(releases, "1.0.0", UpdateTarget.WINDOWS_INSTALLER))
        assertEquals("https://dl/exe/1.0.1", windows.downloadUrl)
        assertEquals(71_352_481, windows.sizeBytes)
    }

    @Test
    fun skipsDraftsAndPrereleases() {
        val releases = listOf(
            nativeRelease("2.0.0", "2026-12-01T00:00:00Z").copy(prerelease = true),
            nativeRelease("1.9.0", "2026-11-30T00:00:00Z").copy(draft = true),
            nativeRelease("1.1.0", "2026-11-20T00:00:00Z"),
        )
        assertEquals("1.1.0", selectUpdate(releases, "1.0.1", UpdateTarget.ANDROID_APK)?.version)
    }

    @Test
    fun versionParsingAndComparison() {
        assertEquals(listOf(1, 2, 0), parseVersion("v1.2.0"))
        assertEquals(listOf(1, 0, 1), parseVersion("Radikall-release-1.0.1.apk"))
        assertEquals(0, compareVersions(listOf(1, 0), listOf(1, 0, 0)))
        assertTrue(compareVersions(listOf(1, 0, 10), listOf(1, 0, 9)) > 0)
        assertTrue(compareVersions(listOf(1, 1), listOf(1, 0, 9)) > 0)
    }

    @Test
    fun notesAreTakenFromFirstBulletListOfEachLanguage() {
        val notes = parseReleaseNotes(iosBody)
        assertEquals(
            listOf("iPhone / iPad 可直接保存到主屏幕使用", "macOS 可直接用 Safari 打开", "使用稳定的公网地址"),
            notes.byLanguage[AppLanguage.SIMPLIFIED_CHINESE],
        )
        assertEquals(listOf("iPhone / iPad support", "macOS support"), notes.byLanguage[AppLanguage.ENGLISH])
    }

    @Test
    fun notesFallBackAcrossLanguages() {
        val update = assertNotNull(
            selectUpdate(listOf(nativeRelease("1.0.1", "2026-11-20T00:00:00Z", iosBody)), "1.0.0", UpdateTarget.ANDROID_APK),
        )
        assertEquals("iPhone / iPad 可直接保存到主屏幕使用", update.notesFor(AppLanguage.TRADITIONAL_CHINESE).first())
        assertEquals("iPhone / iPad support", update.notesFor(AppLanguage.KOREAN).first())

        val plain = parseReleaseNotes("Intro line\n\n- first\n- second\n\nOutro")
        assertEquals(listOf("first", "second"), plain.fallback)
    }

    @Test
    fun snoozeHidesSameVersionForThreeDaysOnly() = runBlocking<Unit> {
        var clock = 1_000_000L
        var releases = listOf(nativeRelease("1.0.1", "2026-11-20T00:00:00Z"))
        val preferences = object : UpdatePreferences {
            override var snoozedVersion = ""
            override var snoozedAtMillis = 0L
        }
        fun checker() = AppUpdateChecker(
            currentVersion = { "1.0.0" },
            target = UpdateTarget.ANDROID_APK,
            fetchReleases = { releases },
            now = { clock },
            preferences = preferences,
        )

        val first = checker()
        first.check()
        assertEquals("1.0.1", first.pendingUpdate.value?.version)
        first.snooze()
        assertNull(first.pendingUpdate.value)

        clock += AppUpdateChecker.SNOOZE_MILLIS - 1
        val nextLaunch = checker().also { it.check() }
        assertNull(nextLaunch.pendingUpdate.value)

        releases = listOf(nativeRelease("1.0.2", "2026-11-25T00:00:00Z"))
        val newerRelease = checker().also { it.check() }
        assertEquals("1.0.2", newerRelease.pendingUpdate.value?.version)

        releases = listOf(nativeRelease("1.0.1", "2026-11-20T00:00:00Z"))
        clock += 2
        val afterThreeDays = checker().also { it.check() }
        assertEquals("1.0.1", afterThreeDays.pendingUpdate.value?.version)
    }
}
