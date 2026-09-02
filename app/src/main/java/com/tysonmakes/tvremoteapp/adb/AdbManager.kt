package com.tysonmakes.tvremoteapp.adb

import android.content.Context
import com.tysonmakes.tvremoteapp.model.KeycodeMapper
import dadb.AdbKeyPair
import dadb.AdbStream
import dadb.Dadb
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okio.BufferedSink
import okio.buffer
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
    private var dadb: Dadb? = null
    private var interactiveShellStream: AdbStream? = null
    private var shellSink: BufferedSink? = null
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

    private fun ensureInteractiveShell(): BufferedSink? {
        val currentSink = shellSink
        if (currentSink != null && interactiveShellStream != null) {
            return currentSink
        }
        val currentDadb = dadb ?: return null
        return try {
            val stream = currentDadb.open("shell:")
            val sink = stream.sink.buffer()
            interactiveShellStream = stream
            shellSink = sink
            sink
        } catch (_: Exception) {
            null
        }
    }

    private fun closeInteractiveShellInternal() {
        try {
            shellSink?.close()
        } catch (_: Exception) {}
        try {
            interactiveShellStream?.close()
        } catch (_: Exception) {}
        shellSink = null
        interactiveShellStream = null
    }

    suspend fun connect(ip: String, port: Int = 5555): AdbConnectionResult = withContext(Dispatchers.IO) {
        adbMutex.withLock {
            val startTime = System.currentTimeMillis()
            lastTargetIp = ip
            lastTargetPort = port
            try {
                cleanupSessionInternal()

                val keyPair = getOrCreateKeyPair()
                val instance = Dadb.create(ip, port, keyPair)

                dadb = instance
                connectedIp = ip
                connectedPort = port

                // Warm up interactive shell in background
                ensureInteractiveShell()

                val latency = (System.currentTimeMillis() - startTime).coerceAtLeast(1)
                AdbConnectionResult.Success("Connected to $ip:$port", latency)
            } catch (e: Exception) {
                cleanupSessionInternal()
                AdbConnectionResult.Failure(e.localizedMessage ?: "Failed to connect to $ip:$port")
            }
        }
    }

    /**
     * Internal reconnect helper used to auto-heal dropped sockets.
     */
    private fun tryReconnectInternal(): Boolean {
        val ip = lastTargetIp ?: connectedIp ?: return false
        val port = lastTargetPort
        return try {
            cleanupSessionInternal()
            val keyPair = getOrCreateKeyPair()
            val instance = Dadb.create(ip, port, keyPair)
            dadb = instance
            connectedIp = ip
            connectedPort = port
            ensureInteractiveShell()
            true
        } catch (_: Exception) {
            cleanupSessionInternal()
            false
        }
    }

    /**
     * Ultra-low latency (<5ms) persistent-stream key event dispatcher with robust fallback.
     */
    suspend fun sendKeyFast(keycode: String): AdbCommandResult = withContext(Dispatchers.IO) {
        val numCode = KeycodeMapper.toNumeric(keycode)
        val startTime = System.currentTimeMillis()

        adbMutex.withLock {
            // 1. Ensure active Dadb instance (or auto-reconnect)
            if (dadb == null) {
                if (!tryReconnectInternal()) {
                    return@withContext AdbCommandResult.Failure("Not connected to TV")
                }
            }

            // 2. Primary Fast Path: Inject into Persistent Interactive Shell (<2ms execution)
            val sink = ensureInteractiveShell()
            if (sink != null) {
                try {
                    sink.writeUtf8("input keyevent $numCode\n")
                    sink.flush()
                    val latency = (System.currentTimeMillis() - startTime).coerceAtLeast(1)
                    return@withContext AdbCommandResult.Success("OK", latency)
                } catch (_: Exception) {
                    // Broken pipe / closed stream -> reset stream and fall through to fallback
                    closeInteractiveShellInternal()
                }
            }

            // 3. Resilient Secondary Fallback: Standard Dadb shell command
            val command = "input keyevent $numCode"
            try {
                val response = dadb!!.shell(command)
                val latency = (System.currentTimeMillis() - startTime).coerceAtLeast(1)
                val out = response.output.trim()
                AdbCommandResult.Success(if (out.isNotEmpty()) out else "OK", latency)
            } catch (e: Exception) {
                // Socket broken or TV closed connection -> Try 1 auto-reconnect attempt
                if (tryReconnectInternal()) {
                    try {
                        val retryResponse = dadb!!.shell(command)
                        val latency = (System.currentTimeMillis() - startTime).coerceAtLeast(1)
                        val out = retryResponse.output.trim()
                        AdbCommandResult.Success(if (out.isNotEmpty()) out else "OK", latency)
                    } catch (retryEx: Exception) {
                        AdbCommandResult.Failure(retryEx.localizedMessage ?: "Key dispatch failed after reconnect")
                    }
                } else {
                    AdbCommandResult.Failure(e.localizedMessage ?: "Key dispatch failed")
                }
            }
        }
    }

    suspend fun sendTextFast(text: String): AdbCommandResult = withContext(Dispatchers.IO) {
        val escaped = text.replace(" ", "%s").replace("'", "\\'").replace("\"", "\\\"")
        val startTime = System.currentTimeMillis()

        adbMutex.withLock {
            if (dadb == null) {
                if (!tryReconnectInternal()) {
                    return@withContext AdbCommandResult.Failure("Not connected to TV")
                }
            }

            val sink = ensureInteractiveShell()
            if (sink != null) {
                try {
                    sink.writeUtf8("input text \"$escaped\"\n")
                    sink.flush()
                    val latency = (System.currentTimeMillis() - startTime).coerceAtLeast(1)
                    return@withContext AdbCommandResult.Success("OK", latency)
                } catch (_: Exception) {
                    closeInteractiveShellInternal()
                }
            }

            val command = "input text \"$escaped\""
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

    suspend fun pushFile(localFile: File, remotePath: String): AdbCommandResult = withContext(Dispatchers.IO) {
        adbMutex.withLock {
            val startTime = System.currentTimeMillis()
            var activeDadb = dadb
            if (activeDadb == null) {
                if (tryReconnectInternal()) {
                    activeDadb = dadb
                } else {
                    return@withContext AdbCommandResult.Failure("Not connected to TV")
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

    suspend fun installApk(localApkFile: File): AdbCommandResult = withContext(Dispatchers.IO) {
        adbMutex.withLock {
            val startTime = System.currentTimeMillis()
            var activeDadb = dadb
            if (activeDadb == null) {
                if (tryReconnectInternal()) {
                    activeDadb = dadb
                } else {
                    return@withContext AdbCommandResult.Failure("Not connected to TV")
                }
            }

            try {
                try {
                    activeDadb!!.install(localApkFile)
                    val latency = (System.currentTimeMillis() - startTime).coerceAtLeast(1)
                    AdbCommandResult.Success("Success: App installed on TV", latency)
                } catch (_: Exception) {
                    // Fallback: push to /data/local/tmp/ and run pm install -r
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

    suspend fun runShell(command: String): AdbCommandResult = withContext(Dispatchers.IO) {
        adbMutex.withLock {
            val startTime = System.currentTimeMillis()
            var activeDadb = dadb
            if (activeDadb == null) {
                if (tryReconnectInternal()) {
                    activeDadb = dadb
                } else {
                    return@withContext AdbCommandResult.Failure("Not connected to TV")
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

    private fun cleanupSessionInternal() {
        closeInteractiveShellInternal()
        try {
            dadb?.close()
        } catch (_: Exception) {}
        dadb = null
        connectedIp = null
    }

    suspend fun disconnect(): AdbConnectionResult = withContext(Dispatchers.IO) {
        adbMutex.withLock {
            cleanupSessionInternal()
            AdbConnectionResult.Success("Disconnected", 0)
        }
    }
}
