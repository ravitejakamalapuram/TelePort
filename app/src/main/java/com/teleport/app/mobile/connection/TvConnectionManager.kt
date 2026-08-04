package com.teleport.app.mobile.connection

import android.util.Log
import android.os.Build
import com.teleport.app.protocol.Command
import com.teleport.app.protocol.TvState
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import io.ktor.websocket.send
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

sealed class ConnectionState {
    object Disconnected : ConnectionState()
    object Connecting : ConnectionState()
    object Connected : ConnectionState()
    data class Error(val message: String) : ConnectionState()
}

class TvConnectionManager(private val coroutineScope: CoroutineScope) {
    private val TAG = "TvConnectionManager"

    private val client = HttpClient(OkHttp) {
        install(WebSockets)
    }

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    var activeIp: String? = null
        private set

    val clientId: String = UUID.randomUUID().toString()

    private val _tvState = MutableStateFlow<TvState?>(null)
    val tvState: StateFlow<TvState?> = _tvState.asStateFlow()

    private var session: DefaultClientWebSocketSession? = null
    private var connectionJob: Job? = null

    // Bolt: Use a Channel to buffer commands instead of launching a new coroutine per command.
    // This prevents GC thrashing and thread starvation when sending high-frequency cursor commands at 200Hz.
    // Using a bounded channel with DROP_OLDEST prevents OOM and node allocation overhead from UNLIMITED.
    private val commandChannel = Channel<Command>(capacity = 64, onBufferOverflow = kotlinx.coroutines.channels.BufferOverflow.DROP_OLDEST)
    private var senderJob: Job? = null

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun connect(ip: String, port: Int) {
        disconnect()

        // Start the single worker coroutine that consumes from the channel
        senderJob = coroutineScope.launch(Dispatchers.IO) {
            // Pre-allocate a single buffer for binary commands to avoid GC thrashing
            // since this coroutine processes commands sequentially.
            val binaryData = ByteArray(9)
            val byteBuffer = java.nio.ByteBuffer.wrap(binaryData, 1, 8)

            for (command in commandChannel) {
                val currentSession = session
                if (currentSession != null && _connectionState.value == ConnectionState.Connected) {
                    try {
                        val frame = when (command) {
                            is Command.MoveCursor -> {
                                binaryData[0] = 0x01.toByte()
                                byteBuffer.clear()
                                byteBuffer.position(1)
                                byteBuffer.putFloat(command.dx)
                                byteBuffer.putFloat(command.dy)
                                Frame.Binary(true, binaryData.copyOf())
                            }
                            is Command.Scroll -> {
                                binaryData[0] = 0x02.toByte()
                                byteBuffer.clear()
                                byteBuffer.position(1)
                                byteBuffer.putFloat(command.dx)
                                byteBuffer.putFloat(command.dy)
                                Frame.Binary(true, binaryData.copyOf())
                            }
                            else -> {
                                val jsonString = json.encodeToString(command)
                                Frame.Text(jsonString)
                            }
                        }
                        currentSession.send(frame)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to send command: $command", e)
                    }
                }
            }
        }

        activeIp = ip
        _connectionState.value = ConnectionState.Connecting
        connectionJob = coroutineScope.launch(Dispatchers.IO) {
            var localSession: DefaultClientWebSocketSession? = null
            try {
                val deviceName = java.net.URLEncoder.encode(Build.MODEL, "UTF-8")
                val hostUrl = "ws://$ip:$port/control?device=$deviceName&clientId=$clientId"
                Log.d(TAG, "Connecting to TV at $hostUrl")
                
                client.webSocket(hostUrl) {
                    localSession = this
                    session = this
                    _connectionState.value = ConnectionState.Connected
                    Log.d(TAG, "Connected successfully")

                    for (frame in incoming) {
                        if (frame is Frame.Text) {
                            val text = frame.readText()
                            try {
                                val state = json.decodeFromString<TvState>(text)
                                _tvState.value = state
                            } catch (e: Exception) {
                                Log.e(TAG, "Error decoding state update from TV: $text", e)
                            }
                        }
                    }
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Connection failed or interrupted", e)
                _connectionState.value = ConnectionState.Error(e.localizedMessage ?: "Unknown connection error")
            } finally {
                if (session == localSession) {
                    session = null
                    _tvState.value = null
                }
                if (_connectionState.value is ConnectionState.Connected) {
                    _connectionState.value = ConnectionState.Disconnected
                }
            }
        }
    }

    fun disconnect() {
        connectionJob?.cancel()
        connectionJob = null
        senderJob?.cancel()
        senderJob = null
        session = null
        _tvState.value = null
        activeIp = null
        _connectionState.value = ConnectionState.Disconnected
    }

    fun sendCommand(command: Command) {
        val currentSession = session
        if (currentSession != null && _connectionState.value == ConnectionState.Connected) {
            // Bolt: Simply push to the channel without allocating a new Coroutine per command
            commandChannel.trySend(command)
        } else {
            Log.w(TAG, "Cannot send command, not connected. Session is null: ${currentSession == null}")
        }
    }
}
