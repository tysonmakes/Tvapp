package com.tysonmakes.tvremoteapp.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tysonmakes.tvremoteapp.adb.*
import com.tysonmakes.tvremoteapp.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

sealed class ConnectionStatus {
    data object Disconnected : ConnectionStatus()
    data class Connecting(val ip: String) : ConnectionStatus()
    data class Connected(val device: TvDevice, val latencyMs: Long) : ConnectionStatus()
    data class Error(val error: String) : ConnectionStatus()
}

data class TvRemoteUiState(
    val connectionStatus: ConnectionStatus = ConnectionStatus.Disconnected,
    val connectedDevice: TvDevice? = null,
    val statusMessage: String = "Ready to connect",
    val latencyMs: Long = 0,
    val currentTab: RemoteTab = RemoteTab.CONTROLS,
    val isDiscoveryOpen: Boolean = false,
    val isSettingsOpen: Boolean = false,
    val isScanning: Boolean = false,
    val scanProgress: ScanProgress? = null,
    val savedDevices: List<TvDevice> = emptyList(),
    val consoleLogs: List<String> = emptyList(),
    val settings: RemoteSettings = RemoteSettings(),
    val lastPressedKey: String? = null,
    val isExecutingTool: Boolean = false
)

class TvRemoteViewModel(application: Application) : AndroidViewModel(application) {

    private val adbManager = AdbManager(application.applicationContext)
    private val prefs = application.getSharedPreferences("tv_remote_prefs", Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(TvRemoteUiState())
    val uiState: StateFlow<TvRemoteUiState> = _uiState.asStateFlow()

    private var scanJob: Job? = null
    private var repeatJob: Job? = null

    init {
        loadSettings()
        loadSavedDevices()
        autoConnectIfEnabled()
    }

    private fun loadSettings() {
        val hapticOrdinal = prefs.getInt("haptic_intensity", HapticIntensity.MEDIUM.ordinal)
        val haptic = HapticIntensity.entries.getOrElse(hapticOrdinal) { HapticIntensity.MEDIUM }

        val responseOrdinal = prefs.getInt("response_mode", ResponseMode.TURBO_STREAM.ordinal)
        val response = ResponseMode.entries.getOrElse(responseOrdinal) { ResponseMode.TURBO_STREAM }

        val repeatSpeed = prefs.getLong("repeat_speed", 85L)
        val autoConnect = prefs.getBoolean("auto_connect", true)

        val themeOrdinal = prefs.getInt("theme_accent", ThemeAccent.CYAN.ordinal)
        val theme = ThemeAccent.entries.getOrElse(themeOrdinal) { ThemeAccent.CYAN }

        val loadedSettings = RemoteSettings(
            hapticIntensity = haptic,
            responseMode = response,
            repeatSpeedMs = repeatSpeed,
            autoConnectLastDevice = autoConnect,
            themeAccent = theme
        )
        _uiState.update { it.copy(settings = loadedSettings) }
    }

    private fun saveSettings(newSettings: RemoteSettings) {
        prefs.edit()
            .putInt("haptic_intensity", newSettings.hapticIntensity.ordinal)
            .putInt("response_mode", newSettings.responseMode.ordinal)
            .putLong("repeat_speed", newSettings.repeatSpeedMs)
            .putBoolean("auto_connect", newSettings.autoConnectLastDevice)
            .putInt("theme_accent", newSettings.themeAccent.ordinal)
            .apply()
        _uiState.update { it.copy(settings = newSettings) }
    }

    fun updateSettings(newSettings: RemoteSettings) {
        saveSettings(newSettings)
    }

    private fun loadSavedDevices() {
        val jsonString = prefs.getString("saved_devices", null)
        val devices = mutableListOf<TvDevice>()
        if (jsonString != null) {
            try {
                val array = JSONArray(jsonString)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    devices.add(
                        TvDevice(
                            ip = obj.getString("ip"),
                            port = obj.optInt("port", 5555),
                            name = obj.optString("name", "Android TV"),
                            lastConnected = obj.optLong("lastConnected", System.currentTimeMillis())
                        )
                    )
                }
            } catch (_: Exception) {}
        }
        _uiState.update { it.copy(savedDevices = devices) }
    }

    private fun persistSavedDevices(devices: List<TvDevice>) {
        val array = JSONArray()
        for (dev in devices) {
            val obj = JSONObject()
            obj.put("ip", dev.ip)
            obj.put("port", dev.port)
            obj.put("name", dev.name)
            obj.put("lastConnected", dev.lastConnected)
            array.put(obj)
        }
        prefs.edit().putString("saved_devices", array.toString()).apply()
    }

    private fun autoConnectIfEnabled() {
        val autoConnect = _uiState.value.settings.autoConnectLastDevice
        val saved = _uiState.value.savedDevices
        if (autoConnect && saved.isNotEmpty()) {
            val mostRecent = saved.first()
            connectDevice(mostRecent.ip, mostRecent.port, mostRecent.name)
        }
    }

    fun connectDevice(ip: String, port: Int = 5555, name: String = "Android TV") {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    connectionStatus = ConnectionStatus.Connecting(ip),
                    statusMessage = "Connecting to $ip:$port...",
                    isDiscoveryOpen = false
                )
            }
            appendLog("Connecting to $ip:$port...")

            when (val result = adbManager.connect(ip, port)) {
                is AdbConnectionResult.Success -> {
                    val tvDevice = TvDevice(ip = ip, port = port, name = name, lastConnected = System.currentTimeMillis())
                    val updatedSaved = _uiState.value.savedDevices.filter { it.ip != ip }.toMutableList()
                    updatedSaved.add(0, tvDevice)
                    persistSavedDevices(updatedSaved)

                    _uiState.update {
                        it.copy(
                            connectionStatus = ConnectionStatus.Connected(tvDevice, result.latencyMs),
                            connectedDevice = tvDevice,
                            savedDevices = updatedSaved,
                            statusMessage = "⚡ Connected ($ip • ${result.latencyMs}ms)",
                            latencyMs = result.latencyMs
                        )
                    }
                    appendLog("Successfully connected to $ip:$port (${result.latencyMs}ms)")
                }
                is AdbConnectionResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            connectionStatus = ConnectionStatus.Error(result.error),
                            statusMessage = "Connect failed: ${result.error}"
                        )
                    }
                    appendLog("Error connecting to $ip:$port: ${result.error}")
                }
            }
        }
    }

    fun disconnect() {
        viewModelScope.launch {
            adbManager.disconnect()
            _uiState.update {
                it.copy(
                    connectionStatus = ConnectionStatus.Disconnected,
                    connectedDevice = null,
                    statusMessage = "Disconnected",
                    latencyMs = 0
                )
            }
            appendLog("Disconnected from TV")
        }
    }

    /**
     * Ultra-fast non-blocking key event sender
     */
    fun sendKey(keycode: String) {
        val keyShort = keycode.removePrefix("KEYCODE_")
        _uiState.update { it.copy(lastPressedKey = keyShort) }

        viewModelScope.launch(Dispatchers.IO) {
            val result = if (_uiState.value.settings.responseMode == ResponseMode.TURBO_STREAM) {
                adbManager.sendKeyFast(keycode)
            } else {
                adbManager.runShell("input keyevent $keycode")
            }

            when (result) {
                is AdbCommandResult.Success -> {
                    _uiState.update {
                        it.copy(
                            statusMessage = "$keyShort (${result.latencyMs}ms)",
                            latencyMs = result.latencyMs
                        )
                    }
                }
                is AdbCommandResult.Failure -> {
                    _uiState.update {
                        it.copy(statusMessage = "Key error: ${result.error}")
                    }
                    appendLog("Error sending $keyShort: ${result.error}")
                }
            }
        }
    }

    /**
     * Smooth Continuous Key Repeat on Long Press (e.g. holding D-pad / Volume)
     */
    fun startKeyRepeat(keycode: String) {
        repeatJob?.cancel()
        sendKey(keycode)
        val repeatSpeed = _uiState.value.settings.repeatSpeedMs

        repeatJob = viewModelScope.launch(Dispatchers.IO) {
            delay(280) // Initial hold threshold
            while (isActive) {
                if (_uiState.value.settings.responseMode == ResponseMode.TURBO_STREAM) {
                    adbManager.sendKeyFast(keycode)
                } else {
                    adbManager.runShell("input keyevent $keycode")
                }
                delay(repeatSpeed)
            }
        }
    }

    fun stopKeyRepeat() {
        repeatJob?.cancel()
        repeatJob = null
    }

    fun sendText(text: String) {
        if (text.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            val result = adbManager.sendTextFast(text)
            when (result) {
                is AdbCommandResult.Success -> {
                    _uiState.update {
                        it.copy(
                            statusMessage = "Sent text \"$text\" (${result.latencyMs}ms)",
                            latencyMs = result.latencyMs
                        )
                    }
                    appendLog("Sent text \"$text\" (${result.latencyMs}ms)")
                }
                is AdbCommandResult.Failure -> {
                    _uiState.update {
                        it.copy(statusMessage = "Error sending text: ${result.error}")
                    }
                    appendLog("Error sending text: ${result.error}")
                }
            }
        }
    }

    fun launchApp(packageName: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(statusMessage = "Launching $packageName...") }
            appendLog("Launching app $packageName...")
            when (val result = adbManager.launchApp(packageName)) {
                is AdbCommandResult.Success -> {
                    _uiState.update {
                        it.copy(
                            statusMessage = "Launched $packageName (${result.latencyMs}ms)",
                            latencyMs = result.latencyMs
                        )
                    }
                    appendLog("Launched $packageName (${result.latencyMs}ms)")
                }
                is AdbCommandResult.Failure -> {
                    _uiState.update {
                        it.copy(statusMessage = "Error launching: ${result.error}")
                    }
                    appendLog("Error launching app: ${result.error}")
                }
            }
        }
    }

    fun executeTvTool(tool: TvToolAction) {
        viewModelScope.launch {
            _uiState.update { it.copy(isExecutingTool = true, statusMessage = "Running ${tool.title}...") }
            appendLog("Executing Tool: ${tool.title} ($ ${tool.command})")

            val result = adbManager.runShell(tool.command)
            _uiState.update { it.copy(isExecutingTool = false) }

            when (result) {
                is AdbCommandResult.Success -> {
                    _uiState.update {
                        it.copy(
                            statusMessage = "✓ ${tool.title} completed (${result.latencyMs}ms)",
                            latencyMs = result.latencyMs
                        )
                    }
                    appendLog("Result: ${result.output}")
                }
                is AdbCommandResult.Failure -> {
                    _uiState.update {
                        it.copy(statusMessage = "Error: ${result.error}")
                    }
                    appendLog("Tool Error: ${result.error}")
                }
            }
        }
    }

    fun executeShell(command: String) {
        if (command.isBlank()) return
        viewModelScope.launch {
            appendLog("$ $command")
            when (val result = adbManager.runShell(command)) {
                is AdbCommandResult.Success -> {
                    _uiState.update {
                        it.copy(
                            statusMessage = "Executed in ${result.latencyMs}ms",
                            latencyMs = result.latencyMs
                        )
                    }
                    appendLog(result.output)
                }
                is AdbCommandResult.Failure -> {
                    _uiState.update {
                        it.copy(statusMessage = "Error: ${result.error}")
                    }
                    appendLog("Error: ${result.error}")
                }
            }
        }
    }

    fun startScan(subnet: String) {
        scanJob?.cancel()
        _uiState.update { it.copy(isScanning = true, scanProgress = null) }
        appendLog("Starting network scan for subnet $subnet (port 5555)...")

        scanJob = viewModelScope.launch {
            DeviceScanner.scanSubnet(subnetPrefix = subnet).collect { progress ->
                _uiState.update {
                    it.copy(
                        scanProgress = progress,
                        isScanning = !progress.isComplete
                    )
                }
                if (progress.isComplete) {
                    appendLog("Scan complete. Found ${progress.foundDevices.size} TV device(s)")
                }
            }
        }
    }

    fun stopScan() {
        scanJob?.cancel()
        _uiState.update { it.copy(isScanning = false) }
        appendLog("Network scan stopped")
    }

    fun deleteDevice(ip: String) {
        val updated = _uiState.value.savedDevices.filter { it.ip != ip }
        persistSavedDevices(updated)
        _uiState.update { it.copy(savedDevices = updated) }
    }

    fun setTab(tab: RemoteTab) {
        _uiState.update { it.copy(currentTab = tab) }
    }

    fun setDiscoveryOpen(open: Boolean) {
        _uiState.update { it.copy(isDiscoveryOpen = open) }
    }

    fun setSettingsOpen(open: Boolean) {
        _uiState.update { it.copy(isSettingsOpen = open) }
    }

    fun clearLogs() {
        _uiState.update { it.copy(consoleLogs = emptyList()) }
    }

    private fun appendLog(log: String) {
        val currentLogs = _uiState.value.consoleLogs.toMutableList()
        if (currentLogs.size > 100) {
            currentLogs.removeAt(0)
        }
        currentLogs.add(log)
        _uiState.update { it.copy(consoleLogs = currentLogs) }
    }
}
