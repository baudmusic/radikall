package com.radiko.platform

actual object PlatformRuntimeInfo {
    actual val versionName: String
        get() {
            val context = AndroidPlatformContext.requireContext()
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            return packageInfo.versionName ?: "0.1.0"
        }

    actual fun clearCaches(): Boolean {
        val context = AndroidPlatformContext.requireContext()
        var cleared = false
        context.cacheDir?.listFiles()?.forEach { file ->
            cleared = file.deleteRecursively() || cleared
        }
        context.externalCacheDir?.listFiles()?.forEach { file ->
            cleared = file.deleteRecursively() || cleared
        }
        return cleared
    }
}
