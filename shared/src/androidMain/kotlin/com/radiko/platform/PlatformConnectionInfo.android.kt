package com.radiko.platform

import android.net.ConnectivityManager
import android.net.NetworkCapabilities

actual object PlatformConnectionInfo {
    actual fun currentConnectionType(): ConnectionType {
        val context = AndroidPlatformContext.requireContext()
        val connectivityManager = context.getSystemService(ConnectivityManager::class.java)
            ?: return ConnectionType.UNKNOWN
        val activeNetwork = connectivityManager.activeNetwork ?: return ConnectionType.UNKNOWN
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
            ?: return ConnectionType.UNKNOWN

        return when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> ConnectionType.WIFI
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> ConnectionType.CELLULAR
            else -> ConnectionType.OTHER
        }
    }
}
