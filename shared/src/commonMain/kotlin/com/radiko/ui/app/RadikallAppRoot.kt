package com.radiko.ui.app

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.radiko.i18n.ProvideAppLocalization
import com.radiko.ui.screens.HomeScreen
import com.radiko.ui.screens.SettingsScreen
import com.radiko.ui.theme.RadikoTheme
import com.radiko.ui.viewmodel.RadikoAppGraph

@Composable
fun RadikallAppRoot(
    isSettingsVisible: Boolean,
    onSettingsVisibilityChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    showHeader: Boolean = true,
) {
    val settingsRepository = remember { RadikoAppGraph.settingsRepository }
    val settingsState by settingsRepository.state.collectAsState()

    LaunchedEffect(Unit) {
        RadikoAppGraph.playerViewModel.restorePlaybackIfNeeded()
    }

    RadikoTheme(themeMode = settingsState.themeMode) {
        ProvideAppLocalization(language = settingsState.language) {
            if (isSettingsVisible) {
                SettingsScreen(
                    onBack = { onSettingsVisibilityChange(false) },
                    modifier = modifier.fillMaxSize(),
                )
            } else {
                HomeScreen(
                    showHeader = showHeader,
                    onOpenSettings = { onSettingsVisibilityChange(true) },
                )
            }
        }
    }
}

object RadikallAppLifecycle {
    fun onAppEnteredBackground() {
        val settings = RadikoAppGraph.settingsRepository.state.value
        if (!settings.backgroundPlaybackEnabled) {
            RadikoAppGraph.playerViewModel.stop()
        }
    }
}
