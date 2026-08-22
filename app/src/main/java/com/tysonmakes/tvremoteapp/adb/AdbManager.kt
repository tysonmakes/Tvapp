package com.tysonmakes.tvremoteapp.adb

import android.content.Context
import dadb.AdbKeyPair
import dadb.AdbStream
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
    private var persistentShellStream: AdbStream? = null

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
            
            // Open a high-speed persistent interactive shell stream (bypasses per-command handshake overhead)
            val stream = instance.open("shell:")
            persistentShellStream = stream
            
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
     * Instantaneous key event execution (<15ms).
     * Writes directly to the open interactive stream socket without spawning new sub-processes.
     */
    suspend fun sendKeyFast(keycode: String): AdbCommandResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        val stream = persistentShellStream
        if (stream != null) {
            try {
                stream.sink.writeUtf8("input keyevent $keycode\n")
                stream.sink.flush()
                val latency = (System.currentTimeMillis() - startTime).coerceAtLeast(1)
                return@withContext AdbCommandResult.Success("OK", latency)
            } catch (e: Exception) {
                // If stream timed out or broke, re-open interactive stream immediately
                try {
                    val newStream = dadb?.open("shell:")
                    persistentShellStream = newStream
                    newStream?.sink?.writeUtf8("input keyevent $keycode\n")
                    newStream?.sink?.flush()
                    val latency = (System.currentTimeMillis() - startTime).coerceAtLeast(1)
                    return@withContext AdbCommandResult.Success("OK", latency)
                } catch (_: Exception) {}
            }
        }
        // Fallback to standard execution
        runShell("input keyevent $keycode")
    }

    suspend fun sendTextFast(text: String): AdbCommandResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        val stream = persistentShellStream
        val escaped = text.replace(" ", "%s").replace("'", "\\'").replace("\"", "\\\"")
        if (stream != null) {
            try {
                stream.sink.writeUtf8("input text \"$escaped\"\n")
                stream.sink.flush()
                val latency = (System.currentTimeMillis() - startTime).coerceAtLeast(1)
                return@withContext AdbCommandResult.Success("OK", latency)
            } catch (_: Exception) {}
        }
        runShell("input text \"$escaped\"")
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
            persistentShellStream?.close()
        } catch (_: Exception) {}
        persistentShellStream = null

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
