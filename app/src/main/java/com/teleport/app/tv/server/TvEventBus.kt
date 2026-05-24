package com.teleport.app.tv.server

import com.teleport.app.protocol.Command
import com.teleport.app.protocol.TvState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

data class ClientCommand(val clientId: String, val command: Command)

object TvEventBus {
    private val _commands = MutableSharedFlow<ClientCommand>(extraBufferCapacity = 64)
    val commands: SharedFlow<ClientCommand> = _commands.asSharedFlow()

    private val _tvState = MutableStateFlow<TvState?>(null)
    val tvState: StateFlow<TvState?> = _tvState.asStateFlow()

    private val _clientConnected = MutableStateFlow(false)
    val clientConnected: StateFlow<Boolean> = _clientConnected.asStateFlow()

    private val _activeClientIds = MutableStateFlow<Set<String>>(emptySet())
    val activeClientIds: StateFlow<Set<String>> = _activeClientIds.asStateFlow()

    fun postCommand(clientId: String, command: Command) {
        _commands.tryEmit(ClientCommand(clientId, command))
    }

    fun updateTvState(state: TvState) {
        _tvState.value = state
    }

    fun setClientConnected(connected: Boolean) {
        _clientConnected.value = connected
    }

    fun registerClient(clientId: String) {
        val current = _activeClientIds.value.toMutableSet()
        if (current.add(clientId)) {
            _activeClientIds.value = current
            _clientConnected.value = true
        }
    }

    fun unregisterClient(clientId: String) {
        val current = _activeClientIds.value.toMutableSet()
        if (current.remove(clientId)) {
            _activeClientIds.value = current
            _clientConnected.value = current.isNotEmpty()
        }
    }

    private val _mirrorFrames = MutableSharedFlow<ByteArray>(extraBufferCapacity = 256)
    val mirrorFrames: SharedFlow<ByteArray> = _mirrorFrames.asSharedFlow()

    fun postMirrorFrame(data: ByteArray) {
        _mirrorFrames.tryEmit(data)
    }
}
