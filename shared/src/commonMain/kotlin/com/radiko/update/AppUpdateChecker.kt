package com.radiko.update

import com.radiko.platform.PlatformPreferences
import com.radiko.platform.openUrl
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

/** 平台对应的安装包类型；iOS（走 Web App）为 null。 */
expect val platformUpdateTarget: UpdateTarget?

/**
 * 启动后到 GitHub Releases 看一眼有没有新版本，有就把它放进 [pendingUpdate] 让首页弹窗。
 *
 * 「暂不更新」会把这个版本记下来，[SNOOZE_MILLIS] 内不再打扰；出了更新的版本则立刻提醒。
 * 「下载」只是打开下载直链，不记录：下了没装的话，下次启动还会再提醒。
 */
class AppUpdateChecker internal constructor(
    private val currentVersion: () -> String,
    private val target: UpdateTarget?,
    private val fetchReleases: suspend () -> List<GitHubRelease>,
    private val now: () -> Long = { Clock.System.now().toEpochMilliseconds() },
    private val preferences: UpdatePreferences = PlatformUpdatePreferences,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val _pendingUpdate = MutableStateFlow<AppUpdate?>(null)
    val pendingUpdate: StateFlow<AppUpdate?> = _pendingUpdate.asStateFlow()
    private var hasChecked = false

    /** 每次启动只查一次，并且稍等一会儿，别和首屏加载抢网络。 */
    fun checkOnLaunch(delayMillis: Long = 3_000L) {
        if (hasChecked || target == null) return
        hasChecked = true
        scope.launch {
            delay(delayMillis)
            runCatching { check() }
        }
    }

    internal suspend fun check() {
        val target = target ?: return
        val update = selectUpdate(fetchReleases(), currentVersion(), target) ?: return
        if (isSnoozed(update.version)) return
        _pendingUpdate.value = update
    }

    fun download() {
        val update = _pendingUpdate.value ?: return
        _pendingUpdate.value = null
        runCatching { openUrl(update.downloadUrl) }
    }

    /** 只是这次先关掉（返回键），不记录；下次启动还会提醒。 */
    fun dismiss() {
        _pendingUpdate.value = null
    }

    fun openReleasePage() {
        val update = _pendingUpdate.value ?: return
        runCatching { openUrl(update.releasePageUrl) }
    }

    fun snooze() {
        val update = _pendingUpdate.value ?: return
        _pendingUpdate.value = null
        preferences.snoozedVersion = update.version
        preferences.snoozedAtMillis = now()
    }

    private fun isSnoozed(version: String): Boolean =
        preferences.snoozedVersion == version &&
            now() - preferences.snoozedAtMillis < SNOOZE_MILLIS

    companion object {
        const val SNOOZE_MILLIS = 3L * 24 * 60 * 60 * 1000

        private const val RELEASES_URL =
            "https://api.github.com/repos/baudmusic/radikall/releases?per_page=15"

        internal fun githubFetcher(
            httpClient: HttpClient,
            currentVersion: () -> String,
        ): suspend () -> List<GitHubRelease> = {
            httpClient.get(RELEASES_URL) {
                header(HttpHeaders.Accept, "application/vnd.github+json")
                header(HttpHeaders.UserAgent, "Radikall/${currentVersion()}")
            }.body()
        }
    }
}

interface UpdatePreferences {
    var snoozedVersion: String
    var snoozedAtMillis: Long
}

private object PlatformUpdatePreferences : UpdatePreferences {
    private const val KEY_VERSION = "update_snoozed_version"
    private const val KEY_AT = "update_snoozed_at"

    override var snoozedVersion: String
        get() = PlatformPreferences.getString(KEY_VERSION, "")
        set(value) = PlatformPreferences.putString(KEY_VERSION, value)

    // PlatformPreferences 没有 Long，存成字符串。
    override var snoozedAtMillis: Long
        get() = PlatformPreferences.getString(KEY_AT, "0").toLongOrNull() ?: 0L
        set(value) = PlatformPreferences.putString(KEY_AT, value.toString())
}
