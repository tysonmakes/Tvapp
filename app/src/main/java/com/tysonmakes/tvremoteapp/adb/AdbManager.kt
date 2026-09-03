package com.tysonmakes.tvremoteapp.adb

import android.content.Context
import com.tysonmakes.tvremoteapp.model.KeycodeMapper
import dadb.AdbKeyPair
import dadb.AdbShellStream
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

    // Persistent interactive shell stream for instant, real-time key/text execution
    // like ATVTools without the overhead of opening/closing sockets or streams per keypress.
    @Volatile
    private var persistentShell: AdbShellStream? = null

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

    private fun createDadbInstance(ip: String, port: Int): Dadb {
        val keyPair = getOrCreateKeyPair()
        // connectTimeout=10000ms, socketTimeout=0 (infinite / no premature read timeouts on idle stream), keepAlive=true
        return Dadb.create(ip, port, keyPair, 10000, 0, true)
    }

    private fun getOrCreatePersistentShell(instance: Dadb): AdbShellStream {
        val current = persistentShell
        if (current != null) {
            return current
        }
        val newStream = instance.openShell("")
        persistentShell = newStream
        return newStream
    }

    private fun closePersistentShellInternal() {
        try {
            persistentShell?.close()
        } catch (_: Exception) {}
        persistentShell = null
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

                val instance = createDadbInstance(ip, port)

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

                // Warm up the persistent interactive shell stream immediately upon connection
                try {
                    val stream = getOrCreatePersistentShell(instance)
                    stream.write("echo ready\n")
                } catch (e: Exception) {
                    android.util.Log.w("AdbManager", "Pre-warming persistent shell note: ${e.message}")
                }

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
            closePersistentShellInternal()
            try { dadb?.close() } catch (_: Exception) {}
            val instance = createDadbInstance(ip, port)
            val probe = withTimeout(5000L) { instance.shell("echo 1") }
            if (probe.exitCode == 0 || probe.output.contains("1")) {
                dadb = instance
                connectedIp = ip
                connectedPort = port
                try {
                    val stream = getOrCreatePersistentShell(instance)
                    stream.write("echo ready\n")
                } catch (_: Exception) {}
                true
            } else {
                false
            }
        } catch (e: Exception) {
            android.util.Log.e("AdbManager", "tryReconnectInternal failed to $ip:$port: ${e.message}")
            false
        }
    }

    /**
     * Ultra-fast, real-time key event dispatcher.
     * Uses persistent interactive shell stream (write command directly to stream)
     * achieving true near-zero latency (<5-15ms) without per-key socket teardown.
     * Smoothly recovers stream/socket if the TV drops or suspends connection.
     */
    suspend fun sendKeyFast(keycode: String): AdbCommandResult = withContext(Dispatchers.IO) {
        val targetKey = if (keycode.all { it.isDigit() }) {
            keycode
        } else if (keycode.startsWith("KEYCODE_")) {
            keycode
        } else {
            "KEYCODE_$keycode"
        }
        val command = "input keyevent $targetKey\n"
        val startTime = System.currentTimeMillis()

        try {
            withTimeout(8000L) {
                adbMutex.withLock {
                    var currentDadb = dadb
                    if (currentDadb == null) {
                        val ip = lastTargetIp ?: connectedIp
                        val port = lastTargetPort
                        if (ip != null) {
                            try {
                                currentDadb = createDadbInstance(ip, port)
                                dadb = currentDadb
                                connectedIp = ip
                                connectedPort = port
                            } catch (e: Exception) {
                                return@withLock AdbCommandResult.Failure("Cannot connect to TV at $ip:$port")
                            }
                        } else {
                            return@withLock AdbCommandResult.Failure("Not connected to TV")
                        }
                    }

                    // Attempt 1: Write directly to persistent shell stream (Instant execution)
                    try {
                        val stream = getOrCreatePersistentShell(currentDadb)
                        stream.write(command)
                        val latency = (System.currentTimeMillis() - startTime).coerceAtLeast(1)
                        return@withLock AdbCommandResult.Success("OK", latency)
                    } catch (streamEx: Exception) {
                        android.util.Log.w("AdbManager", "Persistent stream error, reopening stream: ${streamEx.message}")
                        closePersistentShellInternal()
                    }

                    // Attempt 2: Reopen stream on existing dadb instance
                    try {
                        val freshStream = getOrCreatePersistentShell(currentDadb)
                        freshStream.write(command)
                        val latency = (System.currentTimeMillis() - startTime).coerceAtLeast(1)
                        return@withLock AdbCommandResult.Success("OK", latency)
                    } catch (retryStreamEx: Exception) {
                        android.util.Log.w("AdbManager", "Stream retry failed, reconnecting session: ${retryStreamEx.message}")
                        closePersistentShellInternal()
                        try { currentDadb.close() } catch (_: Exception) {}
                    }

                    // Attempt 3: Recreate Dadb instance and stream if socket dropped completely
                    val ip = lastTargetIp ?: connectedIp
                    val port = lastTargetPort
                    if (ip != null) {
                        try {
                            val freshDadb = createDadbInstance(ip, port)
                            dadb = freshDadb
                            connectedIp = ip
                            connectedPort = port
                            val freshStream = getOrCreatePersistentShell(freshDadb)
                            freshStream.write(command)
                            val latency = (System.currentTimeMillis() - startTime).coerceAtLeast(1)
                            AdbCommandResult.Success("OK", latency)
                        } catch (reconnectEx: Exception) {
                            // Fallback to one-shot shell command if interactive stream cannot be opened
                            try {
                                val fallbackResponse = dadb?.shell("input keyevent $targetKey")
                                val latency = (System.currentTimeMillis() - startTime).coerceAtLeast(1)
                                AdbCommandResult.Success(fallbackResponse?.output?.ifBlank { "OK" } ?: "OK", latency)
                            } catch (fallbackEx: Exception) {
                                AdbCommandResult.Failure(reconnectEx.localizedMessage ?: "Key dispatch failed")
                            }
                        }
                    } else {
                        AdbCommandResult.Failure("TV connection dropped")
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
        val command = "input text \"$escaped\"\n"
        val startTime = System.currentTimeMillis()

        try {
            withTimeout(8000L) {
                adbMutex.withLock {
                    var currentDadb = dadb
                    if (currentDadb == null) {
                        val ip = lastTargetIp ?: connectedIp
                        val port = lastTargetPort
                        if (ip != null) {
                            try {
                                currentDadb = createDadbInstance(ip, port)
                                dadb = currentDadb
                                connectedIp = ip
                                connectedPort = port
                            } catch (e: Exception) {
                                return@withLock AdbCommandResult.Failure("Cannot connect to TV at $ip:$port")
                            }
                        } else {
                            return@withLock AdbCommandResult.Failure("Not connected to TV")
                        }
                    }

                    // Fast write directly to persistent shell stream
                    try {
                        val stream = getOrCreatePersistentShell(currentDadb)
                        stream.write(command)
                        val latency = (System.currentTimeMillis() - startTime).coerceAtLeast(1)
                        AdbCommandResult.Success("OK", latency)
                    } catch (e: Exception) {
                        closePersistentShellInternal()
                        val ip = lastTargetIp ?: connectedIp
                        val port = lastTargetPort
                        if (ip != null) {
                            try {
                                val freshDadb = createDadbInstance(ip, port)
                                dadb = freshDadb
                                connectedIp = ip
                                connectedPort = port
                                val freshStream = getOrCreatePersistentShell(freshDadb)
                                freshStream.write(command)
                                val latency = (System.currentTimeMillis() - startTime).coerceAtLeast(1)
                                AdbCommandResult.Success("OK", latency)
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
        closePersistentShellInternal()
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
