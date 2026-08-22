package com.tysonmakes.tvremoteapp.adb

import android.content.Context
import dadb.AdbKeyPair
import dadb.Dadb
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
            
            // Validate connection
            instance.shell("echo 1")
            
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
     * Fast-path key event execution on dedicated IO dispatcher
     */
    suspend fun sendKeyFast(keycode: String): AdbCommandResult = withContext(Dispatchers.IO) {
        runShell("input keyevent $keycode")
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

    suspend fun sendTextFast(text: String): AdbCommandResult = withContext(Dispatchers.IO) {
        val escaped = text.replace(" ", "%s").replace("'", "\\'").replace("\"", "\\\"")
        runShell("input text \"$escaped\"")
    }

    suspend fun launchApp(packageName: String): AdbCommandResult {
        return runShell("monkey -p $packageName -c android.intent.category.LAUNCHER 1 || am start $(pm dump $packageName | grep -A 1 'MAIN' | grep -oE '[^ ]+/[^ ]+' | head -n 1)")
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
