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
import com.radiko.ui.screens.HomeScreen
import com.radiko.ui.screens.SettingsScreen
import com.radiko.ui.theme.RadikoTheme
import com.radiko.ui.viewmodel.RadikoAppGraph
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.radiko.i18n.ProvideAppLocalization

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AndroidPlatformContext.initialize(applicationContext)

        setContent {
            val settingsRepository = remember { RadikoAppGraph.settingsRepository }
            val playerViewModel = remember { RadikoAppGraph.playerViewModel }
            val nowPlayingViewModel = remember { RadikoAppGraph.nowPlayingViewModel }
            val settingsState by settingsRepository.state.collectAsState()
            val nowPlayingState by nowPlayingViewModel.state.collectAsState()
            val lifecycleOwner = LocalLifecycleOwner.current

            DisposableEffect(lifecycleOwner, settingsState.backgroundPlaybackEnabled) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_STOP && !settingsState.backgroundPlaybackEnabled) {
                        playerViewModel.stop()
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(observer)
                }
            }

            RadikoTheme(themeMode = settingsState.themeMode) {
                ProvideAppLocalization(language = settingsState.language) {
                    var isSettingsVisible by remember { mutableStateOf(false) }

                    BackHandler(enabled = isSettingsVisible || nowPlayingState.isVisible) {
                        when {
                            isSettingsVisible -> isSettingsVisible = false
                            nowPlayingState.isVisible -> nowPlayingViewModel.hide()
                        }
                    }

                    androidx.compose.runtime.LaunchedEffect(Unit) {
                        playerViewModel.restorePlaybackIfNeeded()
                    }

                    if (isSettingsVisible) {
                        SettingsScreen(
                            onBack = { isSettingsVisible = false },
                        )
                    } else {
                        HomeScreen(
                            onOpenSettings = { isSettingsVisible = true },
                        )
                    }
                }
            }
        }
    }
}
