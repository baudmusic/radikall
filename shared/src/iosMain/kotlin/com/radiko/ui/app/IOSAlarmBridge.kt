@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.radiko.ui.app

import com.radiko.ui.viewmodel.RadikoAppGraph
import kotlinx.datetime.Clock

object IOSAlarmBridge {
    private const val dedupeWindowMillis = 5_000L

    @Volatile
    private var appInForeground: Boolean = true

    @Volatile
    private var lastHandledStationId: String? = null

    @Volatile
    private var lastHandledAtMillis: Long = 0L

    fun updateAppForeground(isForeground: Boolean) {
        appInForeground = isForeground
    }

    fun isAppInForeground(): Boolean = appInForeground

    fun handleForegroundAlarmNotification(stationId: String?) {
        stationId?.let { playStationIfNeeded(it) }
    }

    fun handleNotificationTap(stationId: String?) {
        stationId?.let { playStationIfNeeded(it) }
    }

    fun handleForegroundAlarmTimer(stationId: String) {
        if (appInForeground) {
            playStationIfNeeded(stationId)
        }
    }

    private fun playStationIfNeeded(stationId: String) {
        val now = Clock.System.now().toEpochMilliseconds()
        val recentlyHandled = lastHandledStationId == stationId &&
            now - lastHandledAtMillis in 0 until dedupeWindowMillis

        if (recentlyHandled) {
            return
        }

        lastHandledStationId = stationId
        lastHandledAtMillis = now
        RadikoAppGraph.playerViewModel.playStation(
            stationId = stationId,
            bypassCellularConfirmation = true,
        )
    }
}
