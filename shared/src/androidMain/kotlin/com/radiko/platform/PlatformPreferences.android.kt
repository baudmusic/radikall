package com.radiko.platform

import android.content.Context

actual object PlatformPreferences {
    private val sharedPreferences by lazy {
        AndroidPlatformContext.requireContext().getSharedPreferences(
            "com.radiko.radikall.settings",
            Context.MODE_PRIVATE,
        )
    }

    actual fun getString(key: String, defaultValue: String): String {
        return sharedPreferences.getString(key, defaultValue) ?: defaultValue
    }

    actual fun putString(key: String, value: String) {
        sharedPreferences.edit().putString(key, value).apply()
    }

    actual fun getBoolean(key: String, defaultValue: Boolean): Boolean {
        return sharedPreferences.getBoolean(key, defaultValue)
    }

    actual fun putBoolean(key: String, value: Boolean) {
        sharedPreferences.edit().putBoolean(key, value).apply()
    }

    actual fun getInt(key: String, defaultValue: Int): Int {
        return sharedPreferences.getInt(key, defaultValue)
    }

    actual fun putInt(key: String, value: Int) {
        sharedPreferences.edit().putInt(key, value).apply()
    }
}
