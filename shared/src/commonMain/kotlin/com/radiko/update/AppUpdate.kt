package com.radiko.update

import com.radiko.settings.AppLanguage
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** 本平台要下载的安装包类型。没有对应安装包的平台（iOS 走 Web App）为 null，不检查更新。 */
enum class UpdateTarget {
    ANDROID_APK,
    WINDOWS_INSTALLER,
}

/** GitHub 上比当前版本新、而且带着本平台安装包的那一版。 */
data class AppUpdate(
    val version: String,
    val downloadUrl: String,
    val releasePageUrl: String,
    val sizeBytes: Long,
    /** 发布日期，形如 2026-11-15；取不到时为 null。 */
    val publishedDate: String?,
    val notesByLanguage: Map<AppLanguage, List<String>>,
    /** 发布说明没按语言分节时，整篇里第一组要点。 */
    val fallbackNotes: List<String>,
)

/**
 * 按界面语言取更新要点：先找同语言，再找另一种中文，再英文、日文，最后随便哪种都行。
 */
fun AppUpdate.notesFor(language: AppLanguage): List<String> {
    val preference = buildList {
        add(language)
        when (language) {
            AppLanguage.SIMPLIFIED_CHINESE -> add(AppLanguage.TRADITIONAL_CHINESE)
            AppLanguage.TRADITIONAL_CHINESE -> add(AppLanguage.SIMPLIFIED_CHINESE)
            else -> Unit
        }
        add(AppLanguage.ENGLISH)
        add(AppLanguage.JAPANESE)
    }
    return preference.firstNotNullOfOrNull { notesByLanguage[it]?.takeIf(List<String>::isNotEmpty) }
        ?: notesByLanguage.values.firstOrNull(List<String>::isNotEmpty)
        ?: fallbackNotes
}

@Serializable
internal data class GitHubRelease(
    @SerialName("tag_name") val tagName: String,
    val name: String? = null,
    val draft: Boolean = false,
    val prerelease: Boolean = false,
    @SerialName("html_url") val htmlUrl: String = "",
    @SerialName("published_at") val publishedAt: String? = null,
    val body: String? = null,
    val assets: List<GitHubAsset> = emptyList(),
)

@Serializable
internal data class GitHubAsset(
    val name: String,
    @SerialName("browser_download_url") val downloadUrl: String,
    val size: Long = 0,
)

/**
 * 从发布列表里挑出比 [currentVersion] 新、并且带着 [target] 安装包的最新一版。
 *
 * 不能直接用 GitHub 的 releases/latest：仓库里还有不带安装包的发布（比如 iOS Web App 那一版），
 * 它们经常是「最新」的那个。
 */
internal fun selectUpdate(
    releases: List<GitHubRelease>,
    currentVersion: String,
    target: UpdateTarget,
): AppUpdate? {
    val current = parseVersion(currentVersion) ?: return null
    return releases
        .asSequence()
        .filter { !it.draft && !it.prerelease }
        .mapNotNull { release ->
            val asset = pickAsset(release.assets, target) ?: return@mapNotNull null
            val version = parseVersion(release.tagName)
                ?: parseVersion(asset.name)
                ?: return@mapNotNull null
            Triple(release, asset, version)
        }
        .filter { (_, _, version) -> compareVersions(version, current) > 0 }
        .maxWithOrNull { a, b -> compareVersions(a.third, b.third) }
        ?.let { (release, asset, version) ->
            val notes = parseReleaseNotes(release.body.orEmpty())
            AppUpdate(
                version = version.joinToString("."),
                downloadUrl = asset.downloadUrl,
                releasePageUrl = release.htmlUrl,
                sizeBytes = asset.size,
                publishedDate = release.publishedAt?.take(10)?.takeIf { it.length == 10 },
                notesByLanguage = notes.byLanguage,
                fallbackNotes = notes.fallback,
            )
        }
}

private fun pickAsset(assets: List<GitHubAsset>, target: UpdateTarget): GitHubAsset? = when (target) {
    UpdateTarget.ANDROID_APK -> assets.firstOrNull { it.name.endsWith(".apk", ignoreCase = true) }
    // 安装器优先（Radikall-Setup-x.y.z.exe），其次任何 exe，再次 msi。
    UpdateTarget.WINDOWS_INSTALLER ->
        assets.firstOrNull { it.name.endsWith(".exe", ignoreCase = true) && it.name.contains("setup", ignoreCase = true) }
            ?: assets.firstOrNull { it.name.endsWith(".exe", ignoreCase = true) }
            ?: assets.firstOrNull { it.name.endsWith(".msi", ignoreCase = true) }
}

/** "v1.2.0"、"1.2"、"Radikall-release-1.2.0.apk" 都能取出 [1, 2, 0]。 */
internal fun parseVersion(raw: String): List<Int>? =
    Regex("""\d+(?:\.\d+)+|\d+""").find(raw)?.value
        ?.split('.')
        ?.map { it.toIntOrNull() ?: return null }

/** 逐段比较，短的补 0：1.0 == 1.0.0 < 1.0.1。 */
internal fun compareVersions(a: List<Int>, b: List<Int>): Int {
    for (index in 0 until maxOf(a.size, b.size)) {
        val diff = a.getOrElse(index) { 0 }.compareTo(b.getOrElse(index) { 0 })
        if (diff != 0) return diff
    }
    return 0
}

internal class ParsedReleaseNotes(
    val byLanguage: Map<AppLanguage, List<String>>,
    val fallback: List<String>,
)

/**
 * 发布说明的约定：每种语言一节，标题是 `## 中文`、`## English`、`## 日本語` 之类；
 * 每节里第一组 `- ` 开头的列表就是更新要点（最多取 [MAX_NOTES] 条）。
 */
internal fun parseReleaseNotes(body: String): ParsedReleaseNotes {
    val sections = mutableMapOf<AppLanguage, MutableList<String>>()
    val finished = mutableSetOf<AppLanguage>()
    val fallback = mutableListOf<String>()
    var fallbackFinished = false
    var language: AppLanguage? = null

    body.lineSequence().forEach { rawLine ->
        val line = rawLine.trim()
        if (line.startsWith("## ")) {
            language = languageForHeading(line.removePrefix("## "))
            if (fallback.isNotEmpty()) fallbackFinished = true
            return@forEach
        }
        val bullet = bulletText(line)
        val current = language
        if (bullet == null) {
            // 列表后面一旦出现别的内容（空行、下一段标题文字），这一组要点就结束了。
            if (current != null && sections[current].orEmpty().isNotEmpty()) finished += current
            if (fallback.isNotEmpty()) fallbackFinished = true
            return@forEach
        }
        if (current != null && current !in finished) {
            val notes = sections.getOrPut(current) { mutableListOf() }
            if (notes.size < MAX_NOTES) notes += bullet
        }
        if (!fallbackFinished && fallback.size < MAX_NOTES) fallback += bullet
    }
    return ParsedReleaseNotes(byLanguage = sections, fallback = fallback)
}

private const val MAX_NOTES = 5

private fun languageForHeading(heading: String): AppLanguage? {
    val text = heading.trim().lowercase()
    return when {
        "繁體" in text || "繁体" in text -> AppLanguage.TRADITIONAL_CHINESE
        "简体" in text || "中文" in text -> AppLanguage.SIMPLIFIED_CHINESE
        "english" in text -> AppLanguage.ENGLISH
        "日本語" in text -> AppLanguage.JAPANESE
        "한국어" in text || "korean" in text -> AppLanguage.KOREAN
        else -> null
    }
}

private fun bulletText(line: String): String? {
    val content = when {
        line.startsWith("- ") || line.startsWith("* ") || line.startsWith("• ") -> line.drop(2)
        else -> return null
    }
    return content
        .replace(Regex("""\[([^\]]+)]\([^)]*\)"""), "$1")
        .replace("**", "")
        .replace("__", "")
        .replace("`", "")
        .trim()
        .takeIf { it.isNotEmpty() }
}
