package com.radiko.platform

actual object PlatformRuntimeInfo {
    actual val versionName: String = "1.0.1"

    actual fun clearCaches(): Boolean = false
}
