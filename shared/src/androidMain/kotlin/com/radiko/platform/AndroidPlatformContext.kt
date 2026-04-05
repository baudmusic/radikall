package com.radiko.platform

import android.content.Context

object AndroidPlatformContext {
    private var context: Context? = null

    fun initialize(applicationContext: Context) {
        context = applicationContext
    }

    fun requireContext(): Context = checkNotNull(context) {
        "Android platform context was not initialized"
    }
}

