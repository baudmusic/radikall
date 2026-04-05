package com.radiko.platform

import android.os.LocaleList
import java.util.Locale

actual object PlatformLocaleInfo {
    actual fun currentLanguageTag(): String {
        val locale = LocaleList.getDefault().takeIf { !it.isEmpty }?.get(0) ?: Locale.getDefault()
        return locale.toLanguageTag()
    }
}
