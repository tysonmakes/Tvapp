package com.tysonmakes.tvremoteapp.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 1. Top Header Bar: Device Status, Settings, TV Power
            TopDeviceHeader(
                uiState = uiState,
                onOpenDiscovery = { viewModel.setDiscoveryOpen(true) },
                onOpenSettings = { viewModel.setSettingsOpen(true) },
                onPowerClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    viewModel.sendKey(RemoteKeycodes.POWER)
                },
                onDisconnect = { viewModel.disconnect() }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 2. Primary Navigation Shortcut Pill Bar (Back, Home, Menu, Voice, Input)
            PrimaryNavigationPillBar(
                onKeySend = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    viewModel.sendKey(it)
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 3. Tab Selector Bar
            RemoteTabSelectorBar(
                selectedTab = uiState.currentTab,
                onTabSelect = { viewModel.setTab(it) }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 4. Center Dynamic Arena (Remote D-pad / Trackpad / Apps / Numpad / TV Tools / Console)
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
                        RemoteTab.TOOLS -> {
                            TvToolsView(
                                onExecuteTool = { viewModel.executeTvTool(it) },
                                isExecuting = uiState.isExecutingTool
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

            Spacer(modifier = Modifier.height(8.dp))

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
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: Connection Status Pill & TV Label
        Row(
            modifier = Modifier
                .clickable(onClick = onOpenDiscovery)
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(if (isConnected) AccentGreen else AccentRed)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (isConnected) (uiState.connectedDevice?.name ?: "Android TV") else "Disconnected",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "Select TV",
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Text(
                    text = if (isConnected) "${uiState.connectedDevice?.ip} • ${uiState.latencyMs}ms" else "Tap to connect TV",
                    color = if (isConnected) AccentCyan else TextMuted,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Right Actions: Settings & Standby Power
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onOpenSettings,
                modifier = Modifier
                    .size(36.dp)
                    .testTag("open_settings_button")
            ) {
                Icon(Icons.Default.Tune, contentDescription = "Settings", tint = TextSecondary, modifier = Modifier.size(20.dp))
            }

            Spacer(modifier = Modifier.width(4.dp))

            // TV Power Button with Red LED Halo
            Button(
                onClick = onPowerClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentRed.copy(alpha = 0.2f),
                    contentColor = AccentRed
                ),
                shape = CircleShape,
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier
                    .size(36.dp)
                    .shadow(6.dp, CircleShape, spotColor = AccentRed.copy(alpha = 0.5f))
                    .testTag("tv_power_button")
            ) {
                Icon(
                    imageVector = Icons.Default.PowerSettingsNew,
                    contentDescription = "Power",
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
            .background(DarkSurfaceRaised)
            .border(1.dp, DpadBorderColor, RoundedCornerShape(14.dp))
            .padding(horizontal = 6.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        QuickNavPill(icon = Icons.AutoMirrored.Filled.ArrowBack, label = "Back", keycode = RemoteKeycodes.BACK, onKeySend = onKeySend, testTag = "nav_back")
        QuickNavPill(icon = Icons.Default.Home, label = "Home", keycode = RemoteKeycodes.HOME, onKeySend = onKeySend, testTag = "nav_home")
        QuickNavPill(icon = Icons.Default.Menu, label = "Menu", keycode = RemoteKeycodes.MENU, onKeySend = onKeySend, testTag = "nav_menu")
        QuickNavPill(icon = Icons.Default.Mic, label = "Voice", keycode = RemoteKeycodes.SEARCH, onKeySend = onKeySend, testTag = "nav_voice")
        QuickNavPill(icon = Icons.AutoMirrored.Filled.Input, label = "Input", keycode = RemoteKeycodes.TV_INPUT, onKeySend = onKeySend, testTag = "nav_input")
    }
}

@Composable
private fun QuickNavPill(
    icon: ImageVector,
    label: String,
    keycode: String,
    onKeySend: (String) -> Unit,
    testTag: String
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable { onKeySend(keycode) }
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(imageVector = icon, contentDescription = label, tint = AccentCyan, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = label, color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun RemoteTabSelectorBar(
    selectedTab: RemoteTab,
    onTabSelect: (RemoteTab) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(DarkSurface)
            .border(1.dp, DpadBorderColor, RoundedCornerShape(14.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        RemoteTab.entries.forEach { tab ->
            val isSelected = selectedTab == tab
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (isSelected) AccentCyan else Color.Transparent
                    )
                    .clickable { onTabSelect(tab) }
                    .padding(vertical = 6.dp)
                    .testTag("tab_${tab.name.lowercase()}"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = tab.title,
                    color = if (isSelected) DarkBackground else TextSecondary,
                    fontSize = 11.sp,
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
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(DarkSurfaceRaised)
                .border(1.dp, DpadBorderColor, RoundedCornerShape(14.dp))
                .padding(horizontal = 8.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Keyboard,
                contentDescription = "Keyboard",
                tint = AccentCyan,
                modifier = Modifier.size(18.dp)
            )

            OutlinedTextField(
                value = inputText,
                onValueChange = onTextChanged,
                placeholder = { Text("Type text to TV...", color = TextMuted, fontSize = 12.sp) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                modifier = Modifier
                    .weight(1f)
                    .testTag("tv_text_input")
            )

            IconButton(
                onClick = onPasteClipboard,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ContentPaste,
                    contentDescription = "Paste",
                    tint = TextSecondary,
                    modifier = Modifier.size(16.dp)
                )
            }

            IconButton(
                onClick = onSendText,
                modifier = Modifier
                    .size(34.dp)
                    .testTag("send_text_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send",
                    tint = AccentCyan,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // Live Status Footer
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = statusMessage,
                color = if (isConnected) AccentGreen else TextMuted,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
            Text(
                text = "⚡ ADB Turbo Mode",
                color = AccentCyan.copy(alpha = 0.8f),
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
