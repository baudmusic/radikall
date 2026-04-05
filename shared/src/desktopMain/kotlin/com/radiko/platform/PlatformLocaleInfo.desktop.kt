package com.radiko.platform

import java.util.Locale

actual object PlatformLocaleInfo {
    actual fun currentLanguageTag(): String = Locale.getDefault().toLanguageTag()
}
