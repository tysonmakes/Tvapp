package com.tysonmakes.tvremoteapp.adb

import android.content.Context
import com.tysonmakes.tvremoteapp.model.KeycodeMapper
import dadb.AdbKeyPair
import dadb.Dadb
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.io.IOException

sealed class AdbConnectionResult {
    data class Success(val message: String, val latencyMs: Long) : AdbConnectionResult()
    data class Failure(val error: String) : AdbConnectionResult()
}

sealed class AdbCommandResult {
    data class Success(val output: String, val latencyMs: Long) : AdbCommandResult()
    data class Failure(val error: String) : AdbCommandResult()
}

class AdbManager(private val context: Context) {
    @Volatile
    private var dadb: Dadb? = null
    private val adbMutex = Mutex()

    var connectedIp: String? = null
        private set
    var connectedPort: Int = 5555
        private set
    var lastTargetIp: String? = null
        private set
    var lastTargetPort: Int = 5555
        private set

    val isConnected: Boolean
        get() = dadb != null

    private fun getOrCreateKeyPair(): AdbKeyPair {
        val dir = context.filesDir
        val privFile = File(dir, "adbkey")
        val pubFile = File(dir, "adbkey.pub")
        if (!privFile.exists() || !pubFile.exists()) {
            AdbKeyPair.generate(privFile, pubFile)
        }
        return AdbKeyPair.read(privFile, pubFile)
    }

    suspend fun connect(
        ip: String,
        port: Int = 5555,
        onStatusUpdate: ((String) -> Unit)? = null
    ): AdbConnectionResult = withContext(Dispatchers.IO) {
        adbMutex.withLock {
            val startTime = System.currentTimeMillis()
            lastTargetIp = ip
            lastTargetPort = port
            try {
                cleanupSessionInternal()

                val keyPair = getOrCreateKeyPair()
                val instance = Dadb.create(ip, port, keyPair)

                // Dadb.create is lazy and does not open socket until first shell call.
                // Actively verify connection and provide feedback if user needs to accept TV prompt.
                var verified = false
                var lastErrorMessage: String? = null
                val deadline = System.currentTimeMillis() + 25000L
                var authPromptShown = false

                while (System.currentTimeMillis() < deadline && !verified) {
                    try {
                        val probe = withTimeout(3000L) {
                            instance.shell("echo ping")
                        }
                        if (probe.exitCode == 0 || probe.output.contains("ping")) {
                            verified = true
                            break
                        } else {
                            val allOut = probe.allOutput.trim()
                            if (allOut.contains("unauthorized", ignoreCase = true)) {
                                onStatusUpdate?.invoke("Accept 'Allow USB Debugging' on TV screen (tick 'Always allow')...")
                            }
                            lastErrorMessage = allOut
                        }
                    } catch (e: Exception) {
                        val msg = e.localizedMessage?.lowercase() ?: ""
                        if (msg.contains("unauthorized") || msg.contains("auth") || msg.contains("failed to read response") || msg.contains("broken pipe") || msg.contains("connection reset")) {
                            if (!authPromptShown) {
                                onStatusUpdate?.invoke("Accept 'Allow USB Debugging' prompt on your TV screen...")
                                authPromptShown = true
                            }
                            lastErrorMessage = "Device unauthorized or key rejected. Please check TV screen and select 'Always allow'."
                        } else if (msg.contains("refused") || msg.contains("unreachable") || msg.contains("timed out")) {
                            lastErrorMessage = "Cannot connect to $ip:$port (${e.localizedMessage}). Make sure TV is ON and Wireless/USB Debugging is enabled."
                            break
                        } else {
                            lastErrorMessage = e.localizedMessage
                        }
                    }
                    delay(1200L)
                }

                if (!verified) {
                    cleanupSessionInternal()
                    return@withLock AdbConnectionResult.Failure(
                        lastErrorMessage ?: "Failed to verify connection to $ip:$port. Make sure TV is awake."
                    )
                }

                dadb = instance
                connectedIp = ip
                connectedPort = port

                val latency = (System.currentTimeMillis() - startTime).coerceAtLeast(1)
                AdbConnectionResult.Success("Connected to $ip:$port", latency)
            } catch (e: TimeoutCancellationException) {
                cleanupSessionInternal()
                AdbConnectionResult.Failure("Connection timed out. Check TV screen for 'Allow USB Debugging' prompt and accept.")
            } catch (e: Exception) {
                cleanupSessionInternal()
                AdbConnectionResult.Failure(e.localizedMessage ?: "Failed to connect to $ip:$port")
            }
        }
    }

    private suspend fun tryReconnectInternal(): Boolean {
        val ip = lastTargetIp ?: connectedIp ?: return false
        val port = lastTargetPort
        return try {
            cleanupSessionInternal()
            val keyPair = getOrCreateKeyPair()
            val instance = Dadb.create(ip, port, keyPair)
            val probe = withTimeout(2500L) { instance.shell("echo 1") }
            if (probe.exitCode == 0 || probe.output.contains("1")) {
                dadb = instance
                connectedIp = ip
                connectedPort = port
                true
            } else {
                false
            }
        } catch (_: Exception) {
            cleanupSessionInternal()
            false
        }
    }

    /**
     * Ultra-fast, low-latency key event dispatcher.
     * Uses 'cmd input keyevent' (Android 9+ native binder IPC, ~10-25ms)
     * with automatic fallback to standard 'input keyevent'.
     */
    suspend fun sendKeyFast(keycode: String): AdbCommandResult = withContext(Dispatchers.IO) {
        val numCode = KeycodeMapper.toNumeric(keycode)
        // 'cmd input' bypasses JVM zygote startup overhead for ultra-low latency (<20ms)
        val command = "cmd input keyevent $numCode || input keyevent $numCode"
        val startTime = System.currentTimeMillis()

        try {
            withTimeout(3500L) {
                adbMutex.withLock {
                    if (dadb == null) {
                        if (!tryReconnectInternal()) {
                            return@withLock AdbCommandResult.Failure("Not connected to TV")
                        }
                    }

                    try {
                        val response = dadb!!.shell(command)
                        val latency = (System.currentTimeMillis() - startTime).coerceAtLeast(1)
                        if (response.exitCode == 0 || response.errorOutput.isBlank()) {
                            AdbCommandResult.Success(response.output.ifBlank { "OK" }, latency)
                        } else {
                            val err = response.errorOutput.trim()
                            if (err.contains("unauthorized", ignoreCase = true)) {
                                AdbCommandResult.Failure("TV unauthorized. Accept USB debugging on TV screen.")
                            } else {
                                AdbCommandResult.Failure(err.ifBlank { "Key dispatch failed" })
                            }
                        }
                    } catch (e: Exception) {
                        // Socket broke or broken pipe -> attempt fresh reconnect and retry
                        if (tryReconnectInternal()) {
                            try {
                                val retryResponse = dadb!!.shell(command)
                                val latency = (System.currentTimeMillis() - startTime).coerceAtLeast(1)
                                AdbCommandResult.Success("OK", latency)
                            } catch (retryEx: Exception) {
                                AdbCommandResult.Failure(retryEx.localizedMessage ?: "Key dispatch failed")
                            }
                        } else {
                            AdbCommandResult.Failure(e.localizedMessage ?: "Key dispatch failed")
                        }
                    }
                }
            }
        } catch (e: TimeoutCancellationException) {
            AdbCommandResult.Failure("TV response timed out")
        } catch (e: Exception) {
            AdbCommandResult.Failure(e.localizedMessage ?: "Key error")
        }
    }

    suspend fun sendTextFast(text: String): AdbCommandResult = withContext(Dispatchers.IO) {
        // Robust escaping of shell metacharacters and spaces for Android TV text input
        val escaped = buildString {
            for (ch in text) {
                when (ch) {
                    ' ' -> append("%s")
                    '\\', '"', '\'', '$', '`', '&', '|', ';', '<', '>', '(', ')', '*', '?', '~', '#', '!', '{', '}' -> {
                        append('\\').append(ch)
                    }
                    else -> append(ch)
                }
            }
        }
        val command = "cmd input text \"$escaped\" || input text \"$escaped\""
        val startTime = System.currentTimeMillis()

        try {
            withTimeout(4000L) {
                adbMutex.withLock {
                    if (dadb == null) {
                        if (!tryReconnectInternal()) {
                            return@withLock AdbCommandResult.Failure("Not connected to TV")
                        }
                    }

                    try {
                        val response = dadb!!.shell(command)
                        val latency = (System.currentTimeMillis() - startTime).coerceAtLeast(1)
                        AdbCommandResult.Success(response.output.ifBlank { "OK" }, latency)
                    } catch (e: Exception) {
                        if (tryReconnectInternal()) {
                            try {
                                val retryResponse = dadb!!.shell(command)
                                val latency = (System.currentTimeMillis() - startTime).coerceAtLeast(1)
                                AdbCommandResult.Success(retryResponse.output.ifBlank { "OK" }, latency)
                            } catch (retryEx: Exception) {
                                AdbCommandResult.Failure(retryEx.localizedMessage ?: "Text dispatch failed")
                            }
                        } else {
                            AdbCommandResult.Failure(e.localizedMessage ?: "Text dispatch failed")
                        }
                    }
                }
            }
        } catch (e: TimeoutCancellationException) {
            AdbCommandResult.Failure("TV response timed out")
        } catch (e: Exception) {
            AdbCommandResult.Failure(e.localizedMessage ?: "Text error")
        }
    }

    suspend fun pushFile(localFile: File, remotePath: String): AdbCommandResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        try {
            withTimeout(60000L) {
                adbMutex.withLock {
                    var activeDadb = dadb
                    if (activeDadb == null) {
                        if (tryReconnectInternal()) {
                            activeDadb = dadb
                        } else {
                            return@withLock AdbCommandResult.Failure("Not connected to TV")
                        }
                    }

                    try {
                        activeDadb!!.push(localFile, remotePath)
                        val latency = (System.currentTimeMillis() - startTime).coerceAtLeast(1)
                        AdbCommandResult.Success("Uploaded to $remotePath", latency)
                    } catch (e: Exception) {
                        AdbCommandResult.Failure(e.localizedMessage ?: "File upload failed")
                    }
                }
            }
        } catch (e: TimeoutCancellationException) {
            AdbCommandResult.Failure("Upload timed out")
        } catch (e: Exception) {
            AdbCommandResult.Failure(e.localizedMessage ?: "Upload error")
        }
    }

    suspend fun installApk(localApkFile: File): AdbCommandResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        try {
            withTimeout(120000L) {
                adbMutex.withLock {
                    var activeDadb = dadb
                    if (activeDadb == null) {
                        if (tryReconnectInternal()) {
                            activeDadb = dadb
                        } else {
                            return@withLock AdbCommandResult.Failure("Not connected to TV")
                        }
                    }

                    try {
                        try {
                            activeDadb!!.install(localApkFile)
                            val latency = (System.currentTimeMillis() - startTime).coerceAtLeast(1)
                            AdbCommandResult.Success("Success: App installed on TV", latency)
                        } catch (_: Exception) {
                            val safeFileName = "install_${System.currentTimeMillis()}.apk"
                            val tmpRemotePath = "/data/local/tmp/$safeFileName"
                            activeDadb!!.push(localApkFile, tmpRemotePath)
                            val installRes = activeDadb!!.shell("pm install -r -d -g \"$tmpRemotePath\" || pm install -r --user 0 \"$tmpRemotePath\"")
                            activeDadb!!.shell("rm -f \"$tmpRemotePath\"")
                            val latency = (System.currentTimeMillis() - startTime).coerceAtLeast(1)
                            if (installRes.output.contains("Success", ignoreCase = true)) {
                                AdbCommandResult.Success("Success: App installed on TV", latency)
                            } else {
                                val out = installRes.output.trim()
                                if (out.isBlank()) {
                                    AdbCommandResult.Success("Success: App installed on TV", latency)
                                } else {
                                    AdbCommandResult.Failure("Install result: $out")
                                }
                            }
                        }
                    } catch (e: Exception) {
                        AdbCommandResult.Failure(e.localizedMessage ?: "APK installation failed")
                    }
                }
            }
        } catch (e: TimeoutCancellationException) {
            AdbCommandResult.Failure("APK install timed out")
        } catch (e: Exception) {
            AdbCommandResult.Failure(e.localizedMessage ?: "APK install error")
        }
    }

    suspend fun runShell(command: String): AdbCommandResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        try {
            withTimeout(10000L) {
                adbMutex.withLock {
                    var activeDadb = dadb
                    if (activeDadb == null) {
                        if (tryReconnectInternal()) {
                            activeDadb = dadb
                        } else {
                            return@withLock AdbCommandResult.Failure("Not connected to TV")
                        }
                    }

                    try {
                        val response = activeDadb!!.shell(command)
                        val latency = (System.currentTimeMillis() - startTime).coerceAtLeast(1)
                        val output = response.output.trim()
                        val resultText = if (output.isNotEmpty()) output else "OK"
                        AdbCommandResult.Success(resultText, latency)
                    } catch (e: Exception) {
                        if (tryReconnectInternal()) {
                            try {
                                val retryResponse = dadb!!.shell(command)
                                val latency = (System.currentTimeMillis() - startTime).coerceAtLeast(1)
                                val output = retryResponse.output.trim()
                                val resultText = if (output.isNotEmpty()) output else "OK"
                                AdbCommandResult.Success(resultText, latency)
                            } catch (retryEx: Exception) {
                                AdbCommandResult.Failure(retryEx.localizedMessage ?: "Command execution failed")
                            }
                        } else {
                            AdbCommandResult.Failure(e.localizedMessage ?: "Command execution failed")
                        }
                    }
                }
            }
        } catch (e: TimeoutCancellationException) {
            AdbCommandResult.Failure("Shell command timed out")
        } catch (e: Exception) {
            AdbCommandResult.Failure(e.localizedMessage ?: "Command error")
        }
    }

    private fun cleanupSessionInternal() {
        try {
            dadb?.close()
        } catch (_: Exception) {}
        dadb = null
        connectedIp = null
    }

    suspend fun disconnect(): AdbConnectionResult = withContext(Dispatchers.IO) {
        try {
            withTimeout(2000L) {
                adbMutex.withLock {
                    cleanupSessionInternal()
                    AdbConnectionResult.Success("Disconnected", 0)
                }
            }
        } catch (_: Exception) {
            cleanupSessionInternal()
            AdbConnectionResult.Success("Disconnected", 0)
        }
    }
}
