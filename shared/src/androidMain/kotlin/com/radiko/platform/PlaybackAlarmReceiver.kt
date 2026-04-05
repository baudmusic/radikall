package com.radiko.platform

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.radiko.settings.SettingsRepository
import com.radiko.ui.viewmodel.RadikoAppGraph

class PlaybackAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        AndroidPlatformContext.initialize(context.applicationContext)
        val settings = SettingsRepository().state.value
        PlatformAlarmScheduler.sync(settings)

        val stationId = intent.getStringExtra(PlatformAlarmScheduler.EXTRA_STATION_ID)
            ?: settings.alarmStationId
        RadikoAppGraph.playerViewModel.playStation(
            stationId = stationId,
            bypassCellularConfirmation = true,
        )

        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?.apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
        if (launchIntent != null) {
            runCatching { context.startActivity(launchIntent) }
        }
    }
}

class AlarmBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            return
        }

        AndroidPlatformContext.initialize(context.applicationContext)
        PlatformAlarmScheduler.sync(SettingsRepository().state.value)
    }
}
