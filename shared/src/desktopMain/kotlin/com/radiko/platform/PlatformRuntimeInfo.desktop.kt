package com.radiko.platform

actual object PlatformRuntimeInfo {
    actual val versionName: String = "0.1.0"

    actual fun clearCaches(): Boolean = false
}
