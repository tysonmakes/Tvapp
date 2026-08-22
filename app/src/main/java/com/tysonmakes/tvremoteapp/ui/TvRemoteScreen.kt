package com.tysonmakes.tvremoteapp.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Input
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tysonmakes.tvremoteapp.model.RemoteKeycodes
import com.tysonmakes.tvremoteapp.model.RemoteTab
import com.tysonmakes.tvremoteapp.ui.components.*
import com.tysonmakes.tvremoteapp.ui.theme.*

@Composable
fun TvRemoteScreen(
    viewModel: TvRemoteViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val haptic = LocalHapticFeedback.current
    val clipboardManager = LocalClipboardManager.current
    var inputText by remember { mutableStateOf("") }

    val isConnected = uiState.connectionStatus is ConnectionStatus.Connected

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = DarkBackground,
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 14.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 1. Top Header Bar: Device Status, Settings, TV Power
            TopDeviceHeader(
                uiState = uiState,
                onOpenDiscovery = { viewModel.setDiscoveryOpen(true) },
                onOpenSettings = { viewModel.setSettingsOpen(true) },
                onPowerClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    viewModel.setPowerMenuOpen(true)
                },
                onDisconnect = { viewModel.disconnect() }
            )

            Spacer(modifier = Modifier.height(6.dp))

            // 2. Primary Navigation Shortcut Pill Bar (Back, Home, Menu, Voice, Input)
            PrimaryNavigationPillBar(
                onKeySend = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    viewModel.sendKey(it)
                }
            )

            Spacer(modifier = Modifier.height(6.dp))

            // 3. Horizontal Scrollable Tab Selector Bar
            RemoteTabSelectorBar(
                selectedTab = uiState.currentTab,
                onTabSelect = { viewModel.setTab(it) }
            )

            Spacer(modifier = Modifier.height(6.dp))

            // 4. Center Dynamic Arena (Remote D-pad / Tools / Apps / Gamepad / Info / Trackpad / Numpad / Shell)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(
                    targetState = uiState.currentTab,
                    transitionSpec = {
                        fadeIn() togetherWith fadeOut()
                    },
                    label = "remote_tab_content"
                ) { targetTab ->
                    when (targetTab) {
                        RemoteTab.CONTROLS -> {
                            DpadControl(
                                onKeySend = { viewModel.sendKey(it) },
                                onStartRepeat = { viewModel.startKeyRepeat(it) },
                                onStopRepeat = { viewModel.stopKeyRepeat() },
                                hapticLevel = uiState.settings.hapticIntensity
                            )
                        }
                        RemoteTab.TOOLS -> {
                            TvToolsView(
                                onToolClick = { viewModel.handleGridToolClick(it) },
                                isExecuting = uiState.isExecutingTool
                            )
                        }
                        RemoteTab.APPS -> {
                            AppManagerView(
                                apps = uiState.installedApps,
                                isLoading = uiState.isAppsLoading,
                                onRefresh = { viewModel.fetchInstalledApps() },
                                onForceStop = { viewModel.forceStopApp(it) },
                                onClearData = { viewModel.clearAppData(it) },
                                onUninstall = { viewModel.uninstallApp(it) }
                            )
                        }
                        RemoteTab.GAMEPAD -> {
                            GamepadControl(
                                onKeySend = { viewModel.sendKey(it) }
                            )
                        }
                        RemoteTab.INFO -> {
                            DeviceInfoView(
                                telemetry = uiState.telemetry,
                                onRefresh = { viewModel.fetchDeviceTelemetry() }
                            )
                        }
                        RemoteTab.TRACKPAD -> {
                            TrackpadControl(
                                onKeySend = { viewModel.sendKey(it) }
                            )
                        }
                        RemoteTab.NUMPAD -> {
                            NumpadView(
                                onKeySend = { viewModel.sendKey(it) }
                            )
                        }
                        RemoteTab.TERMINAL -> {
                            AdbTerminalView(
                                outputLogs = uiState.consoleLogs,
                                onExecuteCommand = { viewModel.executeShell(it) },
                                onClearLogs = { viewModel.clearLogs() }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // 5. Bottom Smart Keyboard / Text Transmitter Bar
            BottomKeyboardTransmitter(
                inputText = inputText,
                onTextChanged = { inputText = it },
                onSendText = {
                    if (inputText.isNotBlank()) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        viewModel.sendText(inputText)
                        inputText = ""
                    }
                },
                onPasteClipboard = {
                    clipboardManager.getText()?.text?.let { clip ->
                        inputText = clip
                    }
                },
                statusMessage = uiState.statusMessage,
                isConnected = isConnected
            )
        }
    }

    // Dialog: Network Scan & Manual Discovery
    DeviceDiscoveryDialog(
        isOpen = uiState.isDiscoveryOpen,
        isScanning = uiState.isScanning,
        scanProgress = uiState.scanProgress,
        savedDevices = uiState.savedDevices,
        onDismiss = { viewModel.setDiscoveryOpen(false) },
        onStartScan = { viewModel.startScan(it) },
        onStopScan = { viewModel.stopScan() },
        onConnectDevice = { ip, port, name -> viewModel.connectDevice(ip, port, name) },
        onDeleteDevice = { viewModel.deleteDevice(it) }
    )

    // Dialog: Latency & Remote Preferences
    RemoteSettingsDialog(
        isOpen = uiState.isSettingsOpen,
        settings = uiState.settings,
        onDismiss = { viewModel.setSettingsOpen(false) },
        onUpdateSettings = { viewModel.updateSettings(it) }
    )

    // Dialog: Power Menu
    if (uiState.isPowerMenuOpen) {
        PowerMenuDialog(
            onDismiss = { viewModel.setPowerMenuOpen(false) },
            onSleep = { viewModel.sendKey(RemoteKeycodes.SLEEP) },
            onWake = { viewModel.sendKey(RemoteKeycodes.WAKEUP) },
            onSoftReboot = {
                viewModel.executeTvTool(
                    com.tysonmakes.tvremoteapp.model.TvToolAction("soft_reboot", "Soft Reboot", "", "", "setprop ctl.restart zygote || am restart")
                )
            },
            onFullReboot = {
                viewModel.executeTvTool(
                    com.tysonmakes.tvremoteapp.model.TvToolAction("reboot", "Full Reboot", "", "", "reboot")
                )
            },
            onPowerOff = { viewModel.sendKey(RemoteKeycodes.POWER) }
        )
    }

    // Dialog: Screenshot Preview
    if (uiState.isScreenshotDialogOpen) {
        ScreenshotDialog(
            screenshotPath = uiState.lastScreenshotPath,
            onDismiss = { viewModel.setScreenshotDialogOpen(false) }
        )
    }

    // Dialog: File Manager
    if (uiState.isFileManagerOpen) {
        FileManagerDialog(
            currentPath = uiState.currentTvPath,
            files = uiState.tvFiles,
            isLoading = uiState.isFilesLoading,
            onNavigate = { viewModel.fetchTvFiles(it) },
            onDelete = { viewModel.deleteTvFile(it) },
            onRefresh = { viewModel.fetchTvFiles(uiState.currentTvPath) },
            onDismiss = { viewModel.closeFileManager() }
        )
    }
}

@Composable
private fun TopDeviceHeader(
    uiState: TvRemoteUiState,
    onOpenDiscovery: () -> Unit,
    onOpenSettings: () -> Unit,
    onPowerClick: () -> Unit,
    onDisconnect: () -> Unit
) {
    val isConnected = uiState.connectionStatus is ConnectionStatus.Connected

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DarkSurface)
            .border(1.dp, DpadBorderColor, RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Device Info & Status Pill
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clickable(onClick = onOpenDiscovery)
                .padding(4.dp)
                .testTag("device_header_pill")
        ) {
            // Live Status Indicator LED
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(
                        when (uiState.connectionStatus) {
                            is ConnectionStatus.Connected -> StatusConnected
                            is ConnectionStatus.Connecting -> StatusConnecting
                            is ConnectionStatus.Error -> StatusError
                            ConnectionStatus.Disconnected -> StatusDisconnected
                        }
                    )
            )

            Spacer(modifier = Modifier.width(10.dp))

            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = uiState.connectedDevice?.name ?: "Android TV",
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "Switch Device",
                        tint = TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Text(
                    text = when (uiState.connectionStatus) {
                        is ConnectionStatus.Connected -> "${uiState.connectedDevice?.ip} • ${uiState.latencyMs}ms"
                        is ConnectionStatus.Connecting -> "Connecting to ${uiState.connectionStatus.ip}..."
                        is ConnectionStatus.Error -> "Connection error (Tap to retry)"
                        ConnectionStatus.Disconnected -> "Disconnected (Tap to scan)"
                    },
                    color = when (uiState.connectionStatus) {
                        is ConnectionStatus.Connected -> AccentCyan
                        is ConnectionStatus.Error -> StatusError
                        else -> TextMuted
                    },
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Action Icons (Tune & Power)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(
                onClick = onOpenSettings,
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(DarkSurfaceRaised)
                    .testTag("settings_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = "Settings",
                    tint = TextPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }

            IconButton(
                onClick = onPowerClick,
                modifier = Modifier
                    .size(38.dp)
                    .shadow(4.dp, CircleShape, spotColor = Color(0xFFEF4444))
                    .clip(CircleShape)
                    .background(Color(0xFFEF4444).copy(alpha = 0.15f))
                    .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.5f), CircleShape)
                    .testTag("power_button")
            ) {
                Icon(
                    imageVector = Icons.Default.PowerSettingsNew,
                    contentDescription = "Power Menu",
                    tint = Color(0xFFEF4444),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun PrimaryNavigationPillBar(
    onKeySend: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(DarkSurface)
            .border(1.dp, DpadBorderColor, RoundedCornerShape(14.dp))
            .padding(vertical = 4.dp, horizontal = 6.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        QuickNavPill(
            icon = Icons.AutoMirrored.Filled.ArrowBack,
            label = "Back",
            tag = "back",
            onClick = { onKeySend(RemoteKeycodes.BACK) }
        )
        QuickNavPill(
            icon = Icons.Default.Home,
            label = "Home",
            tag = "home",
            onClick = { onKeySend(RemoteKeycodes.HOME) }
        )
        QuickNavPill(
            icon = Icons.Default.Menu,
            label = "Menu",
            tag = "menu",
            onClick = { onKeySend(RemoteKeycodes.MENU) }
        )
        QuickNavPill(
            icon = Icons.Default.Mic,
            label = "Voice",
            tag = "voice",
            onClick = { onKeySend(RemoteKeycodes.VOICE_ASSIST) }
        )
        QuickNavPill(
            icon = Icons.AutoMirrored.Filled.Input,
            label = "Input",
            tag = "input",
            onClick = { onKeySend(RemoteKeycodes.TV_INPUT) }
        )
    }
}

@Composable
private fun QuickNavPill(
    icon: ImageVector,
    label: String,
    tag: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .testTag("quick_nav_$tag")
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = AccentCyan,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            color = TextMuted,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun RemoteTabSelectorBar(
    selectedTab: RemoteTab,
    onTabSelect: (RemoteTab) -> Unit
) {
    val scrollState = rememberScrollState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DarkSurface)
            .border(1.dp, DpadBorderColor, RoundedCornerShape(12.dp))
            .horizontalScroll(scrollState)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RemoteTab.entries.forEach { tab ->
            val isSelected = selectedTab == tab
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSelected) AccentCyan else Color.Transparent)
                    .clickable { onTabSelect(tab) }
                    .padding(horizontal = 14.dp, vertical = 7.dp)
                    .testTag("tab_${tab.name.lowercase()}"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = tab.title,
                    color = if (isSelected) Color.Black else TextMuted,
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun BottomKeyboardTransmitter(
    inputText: String,
    onTextChanged: (String) -> Unit,
    onSendText: () -> Unit,
    onPasteClipboard: () -> Unit,
    statusMessage: String,
    isConnected: Boolean
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = DarkSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, DpadBorderColor)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Keyboard,
                    contentDescription = null,
                    tint = AccentCyan,
                    modifier = Modifier.size(20.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                TextField(
                    value = inputText,
                    onValueChange = onTextChanged,
                    placeholder = {
                        Text(
                            text = "Type text to TV...",
                            color = TextMuted,
                            fontSize = 13.sp
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("tv_input_textfield"),
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )

                IconButton(
                    onClick = onPasteClipboard,
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentPaste,
                        contentDescription = "Paste Clipboard",
                        tint = TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = onSendText,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (inputText.isNotBlank()) AccentCyan else DarkSurfaceRaised)
                        .testTag("send_text_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = if (inputText.isNotBlank()) Color.Black else TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Status Toast Micro-Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = statusMessage,
                color = AccentCyan,
                fontSize = 11.sp,
                maxLines = 1
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Bolt,
                    contentDescription = null,
                    tint = if (isConnected) Color(0xFFFBBF24) else TextMuted,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = if (isConnected) "ADB Turbo Stream" else "Offline",
                    color = if (isConnected) Color(0xFFFBBF24) else TextMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
