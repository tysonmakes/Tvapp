package com.tysonmakes.tvremoteapp.adb

import android.content.Context
import com.tysonmakes.tvremoteapp.model.KeycodeMapper
import dadb.AdbKeyPair
import dadb.Dadb
import kotlinx.coroutines.*
import java.io.File

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

            // Test basic ping to verify active ADB daemon responsiveness
            instance.shell("echo ping")

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
     * Ultra-reliable high speed key dispatch using direct integer keycodes.
     * Uses the active persistent Dadb TCP connection channel.
     */
    suspend fun sendKeyFast(keycode: String): AdbCommandResult = withContext(Dispatchers.IO) {
        val activeDadb = dadb ?: return@withContext AdbCommandResult.Failure("Not connected to TV")
        val startTime = System.currentTimeMillis()
        val numCode = KeycodeMapper.toNumeric(keycode)
        try {
            // Standard universal command: 'input keyevent <numeric_code>'
            val response = activeDadb.shell("input keyevent $numCode")
            val latency = (System.currentTimeMillis() - startTime).coerceAtLeast(1)
            val output = response.output.trim()
            val resultText = if (output.isNotEmpty()) output else "OK"
            AdbCommandResult.Success(resultText, latency)
        } catch (e: Exception) {
            AdbCommandResult.Failure(e.localizedMessage ?: "Key dispatch failed")
        }
    }

    suspend fun sendTextFast(text: String): AdbCommandResult = withContext(Dispatchers.IO) {
        val activeDadb = dadb ?: return@withContext AdbCommandResult.Failure("Not connected to TV")
        val startTime = System.currentTimeMillis()
        try {
            val escaped = text.replace(" ", "%s").replace("'", "\\'").replace("\"", "\\\"")
            val response = activeDadb.shell("input text \"$escaped\"")
            val latency = (System.currentTimeMillis() - startTime).coerceAtLeast(1)
            AdbCommandResult.Success(response.output.ifBlank { "OK" }, latency)
        } catch (e: Exception) {
            AdbCommandResult.Failure(e.localizedMessage ?: "Text dispatch failed")
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
