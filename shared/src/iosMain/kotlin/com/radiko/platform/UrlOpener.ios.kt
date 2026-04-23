@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.radiko.platform

import platform.Foundation.NSURL
import platform.UIKit.UIApplication

actual fun openUrl(url: String) {
    val nsUrl = NSURL(string = url) ?: return
    UIApplication.sharedApplication.openURL(nsUrl)
}
