package com.radiko.platform

enum class ConnectionType {
    WIFI,
    CELLULAR,
    OTHER,
    UNKNOWN,
}

expect object PlatformConnectionInfo {
    fun currentConnectionType(): ConnectionType
}
