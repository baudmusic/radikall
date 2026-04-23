@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.radiko.platform

import com.radiko.settings.AppSettings
import com.radiko.ui.app.IOSAlarmBridge
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import platform.Foundation.NSDateComponents
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNCalendarNotificationTrigger
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNUserNotificationCenter

actual object PlatformAlarmScheduler {
    private const val notificationIdentifier = "com.radiko.radikall.alarm.daily"
    private const val notificationStationIdKey = "stationId"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val notificationCenter = UNUserNotificationCenter.currentNotificationCenter()

    @Volatile
    private var foregroundPlaybackJob: Job? = null

    actual fun sync(settings: AppSettings) {
        foregroundPlaybackJob?.cancel()
        foregroundPlaybackJob = null

        notificationCenter.removePendingNotificationRequestsWithIdentifiers(listOf(notificationIdentifier))
        notificationCenter.removeDeliveredNotificationsWithIdentifiers(listOf(notificationIdentifier))

        if (!settings.alarmEnabled) {
            return
        }

        scheduleForegroundPlayback(settings)
        scheduleLocalNotification(settings)
    }

    private fun scheduleForegroundPlayback(settings: AppSettings) {
        foregroundPlaybackJob = scope.launch {
            while (isActive) {
                delay(nextDelayMillis(settings))
                IOSAlarmBridge.handleForegroundAlarmTimer(settings.alarmStationId)
            }
        }
    }

    private fun scheduleLocalNotification(settings: AppSettings) {
        notificationCenter.requestAuthorizationWithOptions(
            options = UNAuthorizationOptionAlert or UNAuthorizationOptionSound,
        ) { granted, _ ->
            if (!granted) {
                return@requestAuthorizationWithOptions
            }

            val content = UNMutableNotificationContent().apply {
                title = "Radikall"
                body = "Station alarm is ready. Tap to open and start playback."
                userInfo = mapOf(notificationStationIdKey to settings.alarmStationId)
            }

            val triggerDate = NSDateComponents().apply {
                hour = settings.alarmHour.toLong()
                minute = settings.alarmMinute.toLong()
            }

            val trigger = UNCalendarNotificationTrigger.triggerWithDateMatchingComponents(
                dateComponents = triggerDate,
                repeats = true,
            )
            val request = UNNotificationRequest.requestWithIdentifier(
                identifier = notificationIdentifier,
                content = content,
                trigger = trigger,
            )
            notificationCenter.addNotificationRequest(request, withCompletionHandler = null)
        }
    }

    private fun nextDelayMillis(settings: AppSettings): Long {
        val timeZone = TimeZone.currentSystemDefault()
        val now = Clock.System.now()
        val nowLocal = now.toLocalDateTime(timeZone)
        var nextAlarm = LocalDateTime(
            year = nowLocal.year,
            monthNumber = nowLocal.monthNumber,
            dayOfMonth = nowLocal.dayOfMonth,
            hour = settings.alarmHour,
            minute = settings.alarmMinute,
            second = 0,
            nanosecond = 0,
        ).toInstant(timeZone)

        if (nextAlarm <= now) {
            nextAlarm = nextAlarm.plus(1, DateTimeUnit.DAY, timeZone)
        }

        return (nextAlarm.toEpochMilliseconds() - now.toEpochMilliseconds()).coerceAtLeast(0L)
    }
}
