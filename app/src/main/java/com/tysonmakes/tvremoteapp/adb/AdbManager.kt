package com.tysonmakes.tvremoteapp.adb

import android.content.Context
import com.tysonmakes.tvremoteapp.model.KeycodeMapper
import dadb.AdbKeyPair
import dadb.Dadb
import kotlinx.coroutines.*
import java.io.File
import java.io.InputStream

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
    private var supportsCmdInput: Boolean = true

    var connectedIp: String? = null
        private set
    var connectedPort: Int = 5555
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

    suspend fun connect(ip: String, port: Int = 5555): AdbConnectionResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        try {
            cleanupSession()

            val keyPair = getOrCreateKeyPair()
            val instance = Dadb.create(ip, port, keyPair)

            // Probe capability for native C++ cmd vs standard shell
            try {
                val probe = instance.shell("cmd input keyevent 0")
                supportsCmdInput = !probe.output.contains("not found") && !probe.output.contains("Permission denied")
            } catch (_: Exception) {
                supportsCmdInput = false
            }

            dadb = instance
            connectedIp = ip
            connectedPort = port

            val latency = (System.currentTimeMillis() - startTime).coerceAtLeast(1)
            AdbConnectionResult.Success("Connected to $ip:$port", latency)
        } catch (e: Exception) {
            cleanupSession()
            AdbConnectionResult.Failure(e.localizedMessage ?: "Failed to connect to $ip:$port")
        }
    }

    /**
     * Ultra-low latency key event dispatcher.
     */
    suspend fun sendKeyFast(keycode: String): AdbCommandResult = withContext(Dispatchers.IO) {
        val activeDadb = dadb ?: return@withContext AdbCommandResult.Failure("Not connected to TV")
        val startTime = System.currentTimeMillis()
        val numCode = KeycodeMapper.toNumeric(keycode)

        val cmdToRun = if (supportsCmdInput) {
            "cmd input keyevent $numCode"
        } else {
            "cmd input keyevent $numCode 2>/dev/null || (input keyevent $numCode >/dev/null 2>&1 &)"
        }

        try {
            val response = activeDadb.shell(cmdToRun)
            val latency = (System.currentTimeMillis() - startTime).coerceAtLeast(1)
            val output = response.output.trim()
            val resultText = if (output.isNotEmpty()) output else "OK"
            AdbCommandResult.Success(resultText, latency)
        } catch (e: Exception) {
            try {
                activeDadb.shell("input keyevent $numCode >/dev/null 2>&1 &")
                val latency = (System.currentTimeMillis() - startTime).coerceAtLeast(1)
                AdbCommandResult.Success("OK", latency)
            } catch (fallbackEx: Exception) {
                AdbCommandResult.Failure(fallbackEx.localizedMessage ?: "Key dispatch failed")
            }
        }
    }

    suspend fun sendTextFast(text: String): AdbCommandResult = withContext(Dispatchers.IO) {
        val activeDadb = dadb ?: return@withContext AdbCommandResult.Failure("Not connected to TV")
        val startTime = System.currentTimeMillis()
        try {
            val escaped = text.replace(" ", "%s").replace("'", "\\'").replace("\"", "\\\"")
            val command = if (supportsCmdInput) {
                "cmd input text \"$escaped\""
            } else {
                "input text \"$escaped\""
            }
            val response = activeDadb.shell(command)
            val latency = (System.currentTimeMillis() - startTime).coerceAtLeast(1)
            AdbCommandResult.Success(response.output.ifBlank { "OK" }, latency)
        } catch (e: Exception) {
            AdbCommandResult.Failure(e.localizedMessage ?: "Text dispatch failed")
        }
    }

    suspend fun pushFile(localFile: File, remotePath: String): AdbCommandResult = withContext(Dispatchers.IO) {
        val activeDadb = dadb ?: return@withContext AdbCommandResult.Failure("Not connected to TV")
        val startTime = System.currentTimeMillis()
        try {
            activeDadb.push(localFile, remotePath)
            val latency = (System.currentTimeMillis() - startTime).coerceAtLeast(1)
            AdbCommandResult.Success("Uploaded to $remotePath", latency)
        } catch (e: Exception) {
            AdbCommandResult.Failure(e.localizedMessage ?: "File upload failed")
        }
    }

    suspend fun installApk(localApkFile: File): AdbCommandResult = withContext(Dispatchers.IO) {
        val activeDadb = dadb ?: return@withContext AdbCommandResult.Failure("Not connected to TV")
        val startTime = System.currentTimeMillis()
        try {
            try {
                activeDadb.install(localApkFile)
                val latency = (System.currentTimeMillis() - startTime).coerceAtLeast(1)
                AdbCommandResult.Success("Success: App installed on TV", latency)
            } catch (_: Exception) {
                // Fallback: push to /data/local/tmp/ and run pm install -r
                val safeFileName = "install_${System.currentTimeMillis()}.apk"
                val tmpRemotePath = "/data/local/tmp/$safeFileName"
                activeDadb.push(localApkFile, tmpRemotePath)
                val installRes = activeDadb.shell("pm install -r \"$tmpRemotePath\" || pm install -r --user 0 \"$tmpRemotePath\"")
                activeDadb.shell("rm -f \"$tmpRemotePath\"")
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

    suspend fun runShell(command: String): AdbCommandResult = withContext(Dispatchers.IO) {
        val activeDadb = dadb ?: return@withContext AdbCommandResult.Failure("Not connected to TV")
        val startTime = System.currentTimeMillis()
        try {
            val response = activeDadb.shell(command)
            val latency = (System.currentTimeMillis() - startTime).coerceAtLeast(1)
            val output = response.output.trim()
            val resultText = if (output.isNotEmpty()) output else "OK"
            AdbCommandResult.Success(resultText, latency)
        } catch (e: Exception) {
            AdbCommandResult.Failure(e.localizedMessage ?: "Command execution failed")
        }
    }

    private fun cleanupSession() {
        try {
            dadb?.close()
        } catch (_: Exception) {}
        dadb = null
        connectedIp = null
    }

    suspend fun disconnect(): AdbConnectionResult = withContext(Dispatchers.IO) {
        cleanupSession()
        AdbConnectionResult.Success("Disconnected", 0)
    }
}
