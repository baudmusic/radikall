package com.radiko.platform

actual object PlatformConnectionInfo {
    actual fun currentConnectionType(): ConnectionType = ConnectionType.UNKNOWN
}
