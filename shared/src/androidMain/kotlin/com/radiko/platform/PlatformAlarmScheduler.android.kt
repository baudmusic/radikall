package com.radiko.platform

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.radiko.settings.AppSettings
import java.util.Calendar

actual object PlatformAlarmScheduler {
    actual fun sync(settings: AppSettings) {
        val context = AndroidPlatformContext.requireContext()
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val alarmIntent = buildAlarmPendingIntent(context, settings.alarmStationId)

        if (!settings.alarmEnabled) {
            alarmManager.cancel(alarmIntent)
            alarmIntent.cancel()
            return
        }

        val triggerAtMillis = nextTriggerAtMillis(settings)
        val showIntent = PendingIntent.getActivity(
            context,
            REQUEST_CODE_SHOW_APP,
            context.packageManager.getLaunchIntentForPackage(context.packageName),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        alarmManager.cancel(alarmIntent)
        alarmManager.setAlarmClock(
            AlarmManager.AlarmClockInfo(triggerAtMillis, showIntent),
            alarmIntent,
        )
    }

    internal fun nextTriggerAtMillis(settings: AppSettings, nowMillis: Long = System.currentTimeMillis()): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = nowMillis
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            set(Calendar.HOUR_OF_DAY, settings.alarmHour)
            set(Calendar.MINUTE, settings.alarmMinute)
            if (timeInMillis <= nowMillis) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        return calendar.timeInMillis
    }

    internal fun buildAlarmPendingIntent(
        context: Context,
        stationId: String,
    ): PendingIntent {
        val intent = Intent(context, PlaybackAlarmReceiver::class.java).apply {
            action = ACTION_PLAYBACK_ALARM
            putExtra(EXTRA_STATION_ID, stationId)
        }
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_PLAYBACK_ALARM,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    internal const val ACTION_PLAYBACK_ALARM = "com.radiko.radikall.PLAYBACK_ALARM"
    internal const val EXTRA_STATION_ID = "extra_station_id"
    private const val REQUEST_CODE_PLAYBACK_ALARM = 1101
    private const val REQUEST_CODE_SHOW_APP = 1102
}
