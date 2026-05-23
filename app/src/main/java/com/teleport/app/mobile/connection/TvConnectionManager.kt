package com.teleport.app.mobile.connection

import android.util.Log
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

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

    private val _tvState = MutableStateFlow<TvState?>(null)
    val tvState: StateFlow<TvState?> = _tvState.asStateFlow()

    private var session: DefaultClientWebSocketSession? = null
    private var connectionJob: Job? = null

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun connect(ip: String, port: Int) {
        disconnect()

        _connectionState.value = ConnectionState.Connecting
        connectionJob = coroutineScope.launch(Dispatchers.IO) {
            try {
                val hostUrl = "ws://$ip:$port/control"
                Log.d(TAG, "Connecting to TV at $hostUrl")
                
                client.webSocket(hostUrl) {
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
            } catch (e: Exception) {
                Log.e(TAG, "Connection failed or interrupted", e)
                _connectionState.value = ConnectionState.Error(e.localizedMessage ?: "Unknown connection error")
            } finally {
                session = null
                _tvState.value = null
                if (_connectionState.value is ConnectionState.Connected) {
                    _connectionState.value = ConnectionState.Disconnected
                }
            }
        }
    }

    fun disconnect() {
        connectionJob?.cancel()
        connectionJob = null
        session = null
        _tvState.value = null
        _connectionState.value = ConnectionState.Disconnected
    }

    fun sendCommand(command: Command) {
        val currentSession = session
        if (currentSession != null && _connectionState.value == ConnectionState.Connected) {
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    val jsonString = json.encodeToString(command)
                    currentSession.send(Frame.Text(jsonString))
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to send command: $command", e)
                }
            }
        } else {
            Log.w(TAG, "Cannot send command, not connected. Session is null: ${currentSession == null}")
        }
    }
}
