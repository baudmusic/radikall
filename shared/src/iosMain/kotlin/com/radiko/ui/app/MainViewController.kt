@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.radiko.ui.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIViewController

fun MainViewController(): UIViewController = ComposeUIViewController {
    var isSettingsVisible by remember { mutableStateOf(false) }

    RadikallAppRoot(
        isSettingsVisible = isSettingsVisible,
        onSettingsVisibilityChange = { isSettingsVisible = it },
    )
}
