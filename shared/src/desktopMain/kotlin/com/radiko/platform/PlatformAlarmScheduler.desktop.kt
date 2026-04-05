package com.radiko.platform

import com.radiko.settings.AppSettings
import com.radiko.ui.viewmodel.RadikoAppGraph
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

actual object PlatformAlarmScheduler {
    private val executor = Executors.newSingleThreadScheduledExecutor { runnable ->
        Thread(runnable, "radikall-alarm-scheduler").apply {
            isDaemon = true
        }
    }

    @Volatile
    private var future: ScheduledFuture<*>? = null

    @Synchronized
    actual fun sync(settings: AppSettings) {
        future?.cancel(false)
        future = null

        if (!settings.alarmEnabled) {
            return
        }

        val delayMillis = nextDelayMillis(settings)
        future = executor.schedule({
            runCatching {
                RadikoAppGraph.playerViewModel.playStation(
                    stationId = settings.alarmStationId,
                    bypassCellularConfirmation = true,
                )
            }
            sync(RadikoAppGraph.settingsRepository.state.value)
        }, delayMillis, TimeUnit.MILLISECONDS)
    }

    private fun nextDelayMillis(settings: AppSettings): Long {
        val now = LocalDateTime.now()
        var next = now
            .withHour(settings.alarmHour)
            .withMinute(settings.alarmMinute)
            .withSecond(0)
            .withNano(0)
        if (!next.isAfter(now)) {
            next = next.plusDays(1)
        }
        return Duration.between(now, next).toMillis().coerceAtLeast(0L)
    }
}
