package com.radiko.platform

expect object PlatformRuntimeInfo {
    val versionName: String
    fun clearCaches(): Boolean
}
