package com.radiko.platform

import com.radiko.settings.AppSettings

expect object PlatformAlarmScheduler {
    fun sync(settings: AppSettings)
}
