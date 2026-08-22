package com.tysonmakes.tvremoteapp.adb

import android.content.Context
import com.tysonmakes.tvremoteapp.model.KeycodeMapper
import dadb.AdbKeyPair
import dadb.AdbStream
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
    private var persistentShellStream: AdbStream? = null
    private var drainJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val writeLock = Any()

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
            
            // Start background stream drainer to prevent socket buffer congestion/stalls
            startStreamDrainer(stream)

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

    private fun startStreamDrainer(stream: AdbStream) {
        drainJob?.cancel()
        drainJob = scope.launch {
            val buffer = ByteArray(2048)
            try {
                while (isActive) {
                    val read = stream.source.read(buffer)
                    if (read == -1) break
                }
            } catch (_: Exception) {}
        }
    }

    /**
     * Native High-Speed Key Event Execution (<5ms on TV CPU).
     * Uses 'cmd input keyevent <num>' which calls Android's native C++ binder service
     * directly, completely skipping the heavy 'app_process' / Dalvik VM boot overhead (~500ms)
     * of traditional 'input keyevent' shell script.
     */
    suspend fun sendKeyFast(keycode: String): AdbCommandResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        val numCode = KeycodeMapper.toNumeric(keycode)
        // Command executes via native 'cmd' tool directly with fallback to 'input'
        val rawCommand = "cmd input keyevent $numCode || input keyevent $numCode\n"

        val stream = persistentShellStream
        if (stream != null) {
            try {
                synchronized(writeLock) {
                    stream.sink.writeUtf8(rawCommand)
                    stream.sink.flush()
                }
                val latency = (System.currentTimeMillis() - startTime).coerceAtLeast(1)
                return@withContext AdbCommandResult.Success("OK", latency)
            } catch (e: Exception) {
                // If stream timed out or broke, re-open interactive stream immediately
                try {
                    val newStream = dadb?.open("shell:")
                    persistentShellStream = newStream
                    if (newStream != null) {
                        startStreamDrainer(newStream)
                        synchronized(writeLock) {
                            newStream.sink.writeUtf8(rawCommand)
                            newStream.sink.flush()
                        }
                        val latency = (System.currentTimeMillis() - startTime).coerceAtLeast(1)
                        return@withContext AdbCommandResult.Success("OK", latency)
                    }
                } catch (_: Exception) {}
            }
        }
        // Fallback to standard execution with native cmd prioritized
        runShell("cmd input keyevent $numCode || input keyevent $numCode")
    }

    suspend fun sendTextFast(text: String): AdbCommandResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        val stream = persistentShellStream
        val escaped = text.replace(" ", "%s").replace("'", "\\'").replace("\"", "\\\"")
        val rawCommand = "cmd input text \"$escaped\" || input text \"$escaped\"\n"
        if (stream != null) {
            try {
                synchronized(writeLock) {
                    stream.sink.writeUtf8(rawCommand)
                    stream.sink.flush()
                }
                val latency = (System.currentTimeMillis() - startTime).coerceAtLeast(1)
                return@withContext AdbCommandResult.Success("OK", latency)
            } catch (_: Exception) {}
        }
        runShell("cmd input text \"$escaped\" || input text \"$escaped\"")
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
        drainJob?.cancel()
        drainJob = null

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
