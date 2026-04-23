@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.radiko.platform

import platform.Foundation.NSLocale

actual object PlatformLocaleInfo {
    actual fun currentLanguageTag(): String {
        val preferredLanguage = NSLocale.preferredLanguages.firstOrNull() as? String
        return preferredLanguage
            ?: NSLocale.currentLocale.localeIdentifier
            ?: "en"
    }
}
