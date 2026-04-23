@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.radiko.platform

import platform.Network.nw_interface_type_cellular
import platform.Network.nw_interface_type_wifi
import platform.Network.nw_path_get_status
import platform.Network.nw_path_monitor_create
import platform.Network.nw_path_monitor_set_queue
import platform.Network.nw_path_monitor_set_update_handler
import platform.Network.nw_path_monitor_start
import platform.Network.nw_path_status_satisfied
import platform.Network.nw_path_uses_interface_type
import platform.darwin.dispatch_queue_create

actual object PlatformConnectionInfo {
    @Volatile
    private var cachedConnectionType: ConnectionType = ConnectionType.UNKNOWN

    private val monitor = nw_path_monitor_create()
    private val queue = dispatch_queue_create("com.radiko.radikall.network", null)

    init {
        nw_path_monitor_set_update_handler(monitor) { path ->
            cachedConnectionType = when {
                path == null -> ConnectionType.UNKNOWN
                nw_path_uses_interface_type(path, nw_interface_type_wifi) -> ConnectionType.WIFI
                nw_path_uses_interface_type(path, nw_interface_type_cellular) -> ConnectionType.CELLULAR
                nw_path_get_status(path) == nw_path_status_satisfied -> ConnectionType.OTHER
                else -> ConnectionType.UNKNOWN
            }
        }
        nw_path_monitor_set_queue(monitor, queue)
        nw_path_monitor_start(monitor)
    }

    actual fun currentConnectionType(): ConnectionType = cachedConnectionType
}
