package com.radiko.platform

import android.content.Intent
import android.net.Uri

actual fun openUrl(url: String) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    AndroidPlatformContext.requireContext().startActivity(intent)
}
