package com.radiko.platform

actual object PlatformRuntimeInfo {
    actual val versionName: String = "1.0.0"

    actual fun clearCaches(): Boolean = false
}
