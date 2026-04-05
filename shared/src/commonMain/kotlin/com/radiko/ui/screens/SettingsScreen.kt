package com.radiko.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import com.radiko.i18n.appStrings
import com.radiko.platform.PlatformEnvironment
import com.radiko.platform.PlatformRuntimeInfo
import com.radiko.settings.AppLanguage
import com.radiko.settings.AppThemeMode
import com.radiko.settings.AudioFocusBehavior
import com.radiko.settings.CloseButtonMode
import com.radiko.settings.StartupAreaMode
import com.radiko.station.StationRegistry
import com.radiko.ui.components.TopBarActionButton
import com.radiko.ui.components.drawBackArrowIcon
import com.radiko.ui.theme.RadikoColors
import com.radiko.ui.theme.radikoPanelBorderColor
import com.radiko.ui.theme.radikoPanelColor
import com.radiko.ui.theme.radikoPrimaryTextColor
import com.radiko.ui.theme.radikoSecondaryTextColor
import com.radiko.ui.viewmodel.RadikoAppGraph
import kotlinx.coroutines.delay
import kotlinx.datetime.Clock

private data class DropdownOption<T>(
    val label: String,
    val value: T,
)

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val settingsRepository = remember { RadikoAppGraph.settingsRepository }
    val playerViewModel = remember { RadikoAppGraph.playerViewModel }
    val settingsState by settingsRepository.state.collectAsState()
    val playerState by playerViewModel.state.collectAsState()
    val strings = appStrings()
    var cacheClearMessage by remember { mutableStateOf<String?>(null) }
    val nowMillis by produceState(
        initialValue = Clock.System.now().toEpochMilliseconds(),
        key1 = playerState.sleepTimerEndsAtMillis,
    ) {
        value = Clock.System.now().toEpochMilliseconds()
        while (playerState.sleepTimerEndsAtMillis != null) {
            delay(1_000L)
            value = Clock.System.now().toEpochMilliseconds()
        }
    }
    val sleepTimerMinutesLeft = playerState.sleepTimerEndsAtMillis?.let { endsAt ->
        (((endsAt - nowMillis).coerceAtLeast(0L)) + 59_999L) / 60_000L
    }
    val alarmStationOptions = remember(settingsState.language) {
        StationRegistry.allStations
            .distinctBy { it.id }
            .sortedWith(compareBy({ it.id }, { it.name }))
            .map { station ->
                DropdownOption(
                    label = "${station.id} · ${station.name}",
                    value = station.id,
                )
            }
    }

    LaunchedEffect(cacheClearMessage) {
        if (cacheClearMessage != null) {
            delay(2_400L)
            cacheClearMessage = null
        }
    }

    Surface(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                TopBarActionButton(
                    onClick = onBack,
                    buttonSize = 42.dp,
                    drawIcon = { iconColor -> drawBackArrowIcon(iconColor) },
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = strings.settingsTitle,
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Text(
                        text = strings.settingsSubtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.72f),
                    )
                }
            }

            cacheClearMessage?.let { message ->
                SettingsBanner(message = message)
            }

            SettingsSectionCard(
                title = strings.languageSectionTitle,
                description = strings.languageSectionDescription,
            ) {
                SettingsChoiceRow(
                    title = strings.languageSectionTitle.removePrefix("0. ").trim(),
                    subtitle = strings.languageSectionDescription,
                ) {
                    AppLanguage.entries.forEach { language ->
                        OptionPill(
                            text = strings.languageName(language),
                            selected = settingsState.language == language,
                            onClick = { settingsRepository.updateLanguage(language) },
                        )
                    }
                }
            }

            SettingsSectionCard(
                title = strings.regionSectionTitle,
                description = strings.regionSectionDescription,
            ) {
                SettingsChoiceRow(
                    title = strings.startupAreaTitle,
                    subtitle = strings.startupAreaSubtitle,
                ) {
                    OptionPill(
                        text = strings.rememberLastArea,
                        selected = settingsState.startupAreaMode == StartupAreaMode.REMEMBER_LAST,
                        onClick = { settingsRepository.updateStartupAreaMode(StartupAreaMode.REMEMBER_LAST) },
                    )
                    OptionPill(
                        text = strings.fixedArea,
                        selected = settingsState.startupAreaMode == StartupAreaMode.FIXED,
                        onClick = { settingsRepository.updateStartupAreaMode(StartupAreaMode.FIXED) },
                    )
                }

                if (settingsState.startupAreaMode == StartupAreaMode.FIXED) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = strings.fixedStartupAreaTitle,
                            style = MaterialTheme.typography.titleSmall,
                            color = radikoPrimaryTextColor(),
                        )
                        AreaSelector(
                            currentAreaId = settingsState.fixedAreaId,
                            onAreaSelected = settingsRepository::updateFixedAreaId,
                        )
                    }
                }
            }

            SettingsSectionCard(
                title = strings.playbackSectionTitle,
                description = strings.playbackSectionDescription,
            ) {
                SettingsToggleRow(
                    title = strings.autoPlayTitle,
                    subtitle = strings.autoPlaySubtitle,
                    checked = settingsState.autoPlayOnLaunch,
                    onCheckedChange = settingsRepository::updateAutoPlayOnLaunch,
                )
                SettingsToggleRow(
                    title = strings.backgroundPlaybackTitle,
                    subtitle = strings.backgroundPlaybackSubtitle,
                    checked = settingsState.backgroundPlaybackEnabled,
                    onCheckedChange = settingsRepository::updateBackgroundPlayback,
                )
                SettingsChoiceRow(
                    title = strings.audioFocusTitle,
                    subtitle = strings.audioFocusSubtitle,
                ) {
                    OptionPill(
                        text = strings.duckAudio,
                        selected = settingsState.audioFocusBehavior == AudioFocusBehavior.DUCK,
                        onClick = { settingsRepository.updateAudioFocusBehavior(AudioFocusBehavior.DUCK) },
                    )
                    OptionPill(
                        text = strings.pausePlayback,
                        selected = settingsState.audioFocusBehavior == AudioFocusBehavior.PAUSE,
                        onClick = { settingsRepository.updateAudioFocusBehavior(AudioFocusBehavior.PAUSE) },
                    )
                }
            }

            SettingsSectionCard(
                title = strings.timerSectionTitle,
                description = strings.timerSectionDescription,
            ) {
                SettingsChoiceRow(
                    title = strings.sleepTimerTitle,
                    subtitle = strings.sleepTimerSubtitle(sleepTimerMinutesLeft),
                ) {
                    listOf(15, 30, 60, 90).forEach { minutes ->
                        OptionPill(
                            text = strings.sleepTimerOption(minutes),
                            selected = sleepTimerMinutesLeft?.toInt() == minutes,
                            onClick = { playerViewModel.setSleepTimer(minutes) },
                        )
                    }
                    OptionPill(
                        text = strings.sleepTimerOff,
                        selected = playerState.sleepTimerEndsAtMillis == null,
                        onClick = { playerViewModel.setSleepTimer(null) },
                    )
                }

                SettingsToggleRow(
                    title = strings.alarmEnabledTitle,
                    subtitle = strings.alarmEnabledSubtitle,
                    checked = settingsState.alarmEnabled,
                    onCheckedChange = settingsRepository::updateAlarmEnabled,
                )

                if (settingsState.alarmEnabled) {
                    val hourOptions = remember {
                        (0..23).map { hour ->
                            DropdownOption(hour.toString().padStart(2, '0'), hour)
                        }
                    }
                    val minuteOptions = remember {
                        (0..59).map { minute ->
                            DropdownOption(minute.toString().padStart(2, '0'), minute)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        SettingsDropdownField(
                            title = strings.alarmTimeTitle,
                            valueLabel = settingsState.alarmHour.toString().padStart(2, '0'),
                            options = hourOptions,
                            modifier = Modifier.weight(1f),
                            onSelect = { hour ->
                                settingsRepository.updateAlarmTime(hour, settingsState.alarmMinute)
                            },
                        )
                        SettingsDropdownField(
                            title = strings.alarmTimeTitle,
                            valueLabel = settingsState.alarmMinute.toString().padStart(2, '0'),
                            options = minuteOptions,
                            modifier = Modifier.weight(1f),
                            onSelect = { minute ->
                                settingsRepository.updateAlarmTime(settingsState.alarmHour, minute)
                            },
                        )
                    }

                    SettingsDropdownField(
                        title = strings.alarmStationTitle,
                        valueLabel = alarmStationOptions.firstOrNull { it.value == settingsState.alarmStationId }?.label
                            ?: settingsState.alarmStationId,
                        options = alarmStationOptions,
                        onSelect = settingsRepository::updateAlarmStation,
                    )

                    if (PlatformEnvironment.platformType == com.radiko.platform.PlatformType.DESKTOP) {
                        SettingsNote(
                            title = strings.alarmClockTitle,
                            body = strings.alarmDesktopHint,
                        )
                    }
                }
            }

            SettingsSectionCard(
                title = strings.networkSectionTitle,
                description = strings.networkSectionDescription,
            ) {
                SettingsToggleRow(
                    title = strings.wifiOnlyTitle,
                    subtitle = strings.wifiOnlySubtitle,
                    checked = settingsState.wifiOnlyPlayback,
                    onCheckedChange = settingsRepository::updateWifiOnlyPlayback,
                )
                SettingsToggleRow(
                    title = strings.mobileDataConfirmTitle,
                    subtitle = strings.mobileDataConfirmSubtitle,
                    checked = settingsState.confirmMobileDataPlayback,
                    onCheckedChange = settingsRepository::updateConfirmMobileDataPlayback,
                )
            }

            SettingsSectionCard(
                title = strings.appearanceSectionTitle,
                description = strings.appearanceSectionDescription,
            ) {
                SettingsChoiceRow(
                    title = strings.themeModeTitle,
                    subtitle = strings.themeModeSubtitle,
                ) {
                    OptionPill(
                        text = strings.followSystem,
                        selected = settingsState.themeMode == AppThemeMode.SYSTEM,
                        onClick = { settingsRepository.updateThemeMode(AppThemeMode.SYSTEM) },
                    )
                    OptionPill(
                        text = strings.lightTheme,
                        selected = settingsState.themeMode == AppThemeMode.LIGHT,
                        onClick = { settingsRepository.updateThemeMode(AppThemeMode.LIGHT) },
                    )
                    OptionPill(
                        text = strings.darkTheme,
                        selected = settingsState.themeMode == AppThemeMode.DARK,
                        onClick = { settingsRepository.updateThemeMode(AppThemeMode.DARK) },
                    )
                }
                SettingsToggleRow(
                    title = strings.drivingModeTitle,
                    subtitle = strings.drivingModeSubtitle,
                    checked = settingsState.drivingModeEnabled,
                    onCheckedChange = settingsRepository::updateDrivingModeEnabled,
                )
            }

            if (PlatformEnvironment.platformType == com.radiko.platform.PlatformType.DESKTOP) {
                SettingsSectionCard(
                    title = strings.desktopSectionTitle,
                    description = strings.desktopSectionDescription,
                ) {
                    SettingsChoiceRow(
                        title = strings.closeButtonBehaviorTitle,
                        subtitle = strings.closeButtonBehaviorSubtitle,
                    ) {
                        OptionPill(
                            text = strings.minimizeToTray,
                            selected = settingsState.closeButtonMode != CloseButtonMode.EXIT,
                            onClick = {
                                settingsRepository.updateCloseButtonMode(CloseButtonMode.MINIMIZE_TO_TRAY)
                            },
                        )
                        OptionPill(
                            text = strings.exitApp,
                            selected = settingsState.closeButtonMode == CloseButtonMode.EXIT,
                            onClick = {
                                settingsRepository.updateCloseButtonMode(CloseButtonMode.EXIT)
                            },
                        )
                    }
                }
            }

            SettingsSectionCard(
                title = strings.generalSectionTitle,
                description = strings.generalSectionDescription,
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {
                            cacheClearMessage = strings.cacheClearMessage(PlatformRuntimeInfo.clearCaches())
                        },
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f),
                    border = BorderStroke(1.dp, radikoPanelBorderColor()),
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = strings.clearCacheTitle,
                            style = MaterialTheme.typography.titleMedium,
                            color = radikoPrimaryTextColor(),
                        )
                        Text(
                            text = strings.clearCacheSubtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = radikoSecondaryTextColor(),
                        )
                    }
                }
                SettingsNote(
                    title = strings.aboutTitle,
                    body = strings.aboutBody(PlatformRuntimeInfo.versionName),
                )
            }
        }
    }
}

@Composable
private fun SettingsBanner(message: String) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = radikoPanelColor(),
        border = BorderStroke(1.dp, radikoPanelBorderColor()),
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            color = RadikoColors.PrimaryBlue,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun SettingsSectionCard(
    title: String,
    description: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, radikoPanelBorderColor()),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = radikoPrimaryTextColor(),
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = radikoSecondaryTextColor(),
            )
            content()
        }
    }
}

@Composable
private fun SettingsToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.34f),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = radikoPrimaryTextColor(),
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = radikoSecondaryTextColor(),
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
            )
        }
    }
}

@Composable
private fun SettingsChoiceRow(
    title: String,
    subtitle: String,
    content: @Composable RowScope.() -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = radikoPrimaryTextColor(),
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = radikoSecondaryTextColor(),
        )
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = content,
        )
    }
}

@Composable
private fun RowScope.OptionPill(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    val highlighted = selected || hovered
    val scale by animateFloatAsState(
        targetValue = if (hovered && !selected) 1.03f else 1f,
        label = "settings_option_pill_scale",
    )
    val containerColor by animateColorAsState(
        targetValue = if (highlighted) RadikoColors.AccentRed else MaterialTheme.colorScheme.surface,
        label = "settings_option_pill_container",
    )
    val borderColor by animateColorAsState(
        targetValue = if (highlighted) RadikoColors.AccentRed else radikoPanelBorderColor(),
        label = "settings_option_pill_border",
    )
    val textColor by animateColorAsState(
        targetValue = if (highlighted) Color.White else RadikoColors.AccentRed,
        label = "settings_option_pill_text",
    )

    Surface(
        modifier = Modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .hoverable(interactionSource = interactionSource)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        shape = RoundedCornerShape(999.dp),
        color = containerColor,
        border = BorderStroke(1.dp, borderColor),
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = textColor,
                fontWeight = FontWeight.Medium,
            )
        }
    }
    Spacer(modifier = Modifier.width(2.dp))
}

@Composable
private fun <T> SettingsDropdownField(
    title: String,
    valueLabel: String,
    options: List<DropdownOption<T>>,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = radikoPrimaryTextColor(),
        )
        Box {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { expanded = true },
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.34f),
                border = BorderStroke(1.dp, radikoPanelBorderColor()),
            ) {
                Text(
                    text = valueLabel,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = radikoPrimaryTextColor(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.heightIn(max = 360.dp),
                containerColor = MaterialTheme.colorScheme.surface,
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = option.label,
                                color = radikoPrimaryTextColor(),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                        onClick = {
                            expanded = false
                            onSelect(option.value)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsNote(
    title: String,
    body: String,
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = radikoPanelColor(),
        border = BorderStroke(1.dp, radikoPanelBorderColor()),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = RadikoColors.PrimaryBlue,
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodySmall,
                color = radikoPrimaryTextColor(),
            )
        }
    }
}
