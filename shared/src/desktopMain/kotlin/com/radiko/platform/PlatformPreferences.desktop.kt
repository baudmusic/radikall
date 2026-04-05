package com.radiko.platform

import java.util.prefs.Preferences

actual object PlatformPreferences {
    private val preferences = Preferences.userRoot().node("com.radiko.radikall.desktop")

    actual fun getString(key: String, defaultValue: String): String {
        return preferences.get(key, defaultValue)
    }

    actual fun putString(key: String, value: String) {
        preferences.put(key, value)
    }

    actual fun getBoolean(key: String, defaultValue: Boolean): Boolean {
        return preferences.getBoolean(key, defaultValue)
    }

    actual fun putBoolean(key: String, value: Boolean) {
        preferences.putBoolean(key, value)
    }

    actual fun getInt(key: String, defaultValue: Int): Int {
        return preferences.getInt(key, defaultValue)
    }

    actual fun putInt(key: String, value: Int) {
        preferences.putInt(key, value)
    }
}
