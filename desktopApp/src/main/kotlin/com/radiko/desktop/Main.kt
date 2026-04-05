package com.radiko.desktop

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.radiko.i18n.ProvideAppLocalization
import com.radiko.ui.screens.AreaSelector
import com.radiko.ui.screens.HomeScreen
import com.radiko.ui.screens.SettingsScreen
import com.radiko.ui.theme.RadikoTheme
import com.radiko.ui.viewmodel.RadikoAppGraph
import org.jetbrains.compose.resources.painterResource
import radiko_app.shared.generated.resources.Res
import radiko_app.shared.generated.resources.logo2
import java.awt.Frame
import java.awt.Toolkit

fun main() = application {
    val playerViewModel = remember { RadikoAppGraph.playerViewModel }
    val nowPlayingViewModel = remember { RadikoAppGraph.nowPlayingViewModel }
    val settingsRepository = remember { RadikoAppGraph.settingsRepository }
    val playerState by playerViewModel.state.collectAsState()
    val settingsState by settingsRepository.state.collectAsState()
    val taskbarPreviewController = remember {
        WindowsTaskbarPreviewController(
            onPreviousStation = playerViewModel::playPreviousStation,
            onTogglePlayback = playerViewModel::togglePlayback,
            onNextStation = playerViewModel::playNextStation,
        )
    }
    val desktopPreferences = remember { DesktopWindowPreferences() }
    var showClosePrompt by remember { mutableStateOf(false) }
    var rememberChoice by remember { mutableStateOf(false) }
    var isWindowVisible by remember { mutableStateOf(true) }
    var isSettingsVisible by remember { mutableStateOf(false) }
    var bringToFrontTrigger by remember { mutableStateOf(0) }
    val trayController = remember {
        DesktopTrayController(
            onRestoreWindow = {
                isWindowVisible = true
                bringToFrontTrigger++
            },
            onChooseStation = {
                nowPlayingViewModel.hide()
                isSettingsVisible = false
                isWindowVisible = true
                bringToFrontTrigger++
            },
            onTogglePlayback = playerViewModel::togglePlayback,
            onExit = ::exitApplication,
        )
    }
    val screenSize = Toolkit.getDefaultToolkit().screenSize
    val defaultWindowSize = DpSize(
        width = (screenSize.width * 0.8).toInt().dp,
        height = (screenSize.height * 0.8).toInt().dp,
    )
    val windowState = rememberWindowState(size = defaultWindowSize)
    val windowTitle = playerState.currentProgram?.title?.takeIf { it.isNotBlank() }
        ?: playerState.currentStation?.name
        ?: "Radikall"
    val handleCloseRequest = {
        if (!trayController.isSupported()) {
            exitApplication()
        } else {
            when (desktopPreferences.closeBehavior()) {
                DesktopCloseBehavior.MINIMIZE_TO_TRAY -> {
                    showClosePrompt = false
                    isWindowVisible = false
                    trayController.hidePopup()
                }
                DesktopCloseBehavior.EXIT -> exitApplication()
                DesktopCloseBehavior.ASK -> {
                    rememberChoice = false
                    showClosePrompt = true
                }
            }
        }
    }

    Window(
        onCloseRequest = handleCloseRequest,
        icon = painterResource(Res.drawable.logo2),
        title = windowTitle,
        state = windowState,
        undecorated = true,
    ) {
        DisposableEffect(window) {
            taskbarPreviewController.attach(window)
            trayController.attach(window)
            onDispose {
                taskbarPreviewController.dispose()
                trayController.dispose()
            }
        }

        LaunchedEffect(
            playerState.currentStation?.id,
            playerState.currentProgram?.title,
            playerState.isPlaying,
            settingsState.language,
        ) {
            taskbarPreviewController.update(playerState, settingsState.language)
            trayController.update(playerState, settingsState.language)
        }

        LaunchedEffect(windowTitle) {
            window.title = windowTitle
        }

        LaunchedEffect(Unit) {
            playerViewModel.restorePlaybackIfNeeded()
        }

        LaunchedEffect(isWindowVisible) {
            window.isVisible = isWindowVisible
        }

        LaunchedEffect(bringToFrontTrigger) {
            if (bringToFrontTrigger == 0) return@LaunchedEffect
            window.isVisible = true
            window.extendedState = Frame.NORMAL
            window.isAlwaysOnTop = true
            window.toFront()
            window.requestFocus()
            window.isAlwaysOnTop = false
        }

        RadikoTheme(themeMode = settingsState.themeMode) {
            ProvideAppLocalization(language = settingsState.language) {
                Column(modifier = Modifier.fillMaxSize()) {
                    DesktopTitleBar(
                        window = window,
                        isMaximized = windowState.placement == WindowPlacement.Maximized,
                        onOpenSettings = { isSettingsVisible = !isSettingsVisible },
                        onMinimize = { windowState.isMinimized = true },
                        onMaximizeRestore = {
                            windowState.placement = if (windowState.placement == WindowPlacement.Maximized) {
                                WindowPlacement.Floating
                            } else {
                                WindowPlacement.Maximized
                            }
                        },
                        onClose = handleCloseRequest,
                        areaContent = {
                            AreaSelector(
                                currentAreaId = playerState.currentAreaId,
                                onAreaSelected = playerViewModel::switchArea,
                            )
                        },
                    )

                    Box(modifier = Modifier.weight(1f)) {
                        HomeScreen(showHeader = false)

                        if (isSettingsVisible) {
                            SettingsScreen(
                                onBack = { isSettingsVisible = false },
                            )
                        }

                        if (showClosePrompt) {
                            CloseToTrayPrompt(
                                rememberChoice = rememberChoice,
                                onRememberChoiceChange = { rememberChoice = it },
                                onMinimizeToTray = {
                                    if (rememberChoice) {
                                        desktopPreferences.saveCloseBehavior(DesktopCloseBehavior.MINIMIZE_TO_TRAY)
                                    }
                                    showClosePrompt = false
                                    isWindowVisible = false
                                    trayController.hidePopup()
                                },
                                onExit = {
                                    if (rememberChoice) {
                                        desktopPreferences.saveCloseBehavior(DesktopCloseBehavior.EXIT)
                                    }
                                    showClosePrompt = false
                                    exitApplication()
                                },
                                onCancel = {
                                    rememberChoice = false
                                    showClosePrompt = false
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}
