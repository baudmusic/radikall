@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.radiko.platform

import platform.Foundation.NSBundle
import platform.Foundation.NSCachesDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSUserDomainMask

actual object PlatformRuntimeInfo {
    actual val versionName: String
        get() {
            val shortVersion = NSBundle.mainBundle.objectForInfoDictionaryKey("CFBundleShortVersionString") as? String
            val buildVersion = NSBundle.mainBundle.objectForInfoDictionaryKey("CFBundleVersion") as? String
            return shortVersion ?: buildVersion ?: "1.0.1"
        }

    actual fun clearCaches(): Boolean {
        val fileManager = NSFileManager.defaultManager
        val roots = buildList {
            NSTemporaryDirectory().takeIf { it.isNotBlank() }?.let(::add)
            (NSSearchPathForDirectoriesInDomains(NSCachesDirectory, NSUserDomainMask, true).firstOrNull() as? String)
                ?.takeIf { it.isNotBlank() }
                ?.let(::add)
        }

        var cleared = false
        roots.forEach { root ->
            val contents = fileManager.contentsOfDirectoryAtPath(root, error = null) ?: return@forEach
            contents.forEach { child ->
                val childName = child as? String ?: return@forEach
                val separator = if (root.endsWith("/")) "" else "/"
                val childPath = root + separator + childName
                cleared = fileManager.removeItemAtPath(childPath, error = null) || cleared
            }
        }
        return cleared
    }
}
