package com.radiko.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.radiko.platform.AndroidPlatformContext
import com.radiko.ui.app.RadikallAppLifecycle
import com.radiko.ui.app.RadikallAppRoot
import com.radiko.ui.viewmodel.RadikoAppGraph
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AndroidPlatformContext.initialize(applicationContext)

        setContent {
            val nowPlayingViewModel = remember { RadikoAppGraph.nowPlayingViewModel }
            val nowPlayingState by nowPlayingViewModel.state.collectAsState()
            val lifecycleOwner = LocalLifecycleOwner.current
            var isSettingsVisible by remember { mutableStateOf(false) }

            DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_STOP) {
                        RadikallAppLifecycle.onAppEnteredBackground()
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(observer)
                }
            }

            BackHandler(enabled = isSettingsVisible || nowPlayingState.isVisible) {
                when {
                    isSettingsVisible -> isSettingsVisible = false
                    nowPlayingState.isVisible -> nowPlayingViewModel.hide()
                }
            }

            RadikallAppRoot(
                isSettingsVisible = isSettingsVisible,
                onSettingsVisibilityChange = { isSettingsVisible = it },
            )
        }
    }
}
