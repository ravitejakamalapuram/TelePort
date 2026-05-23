package com.teleport.app.tv.server

import com.teleport.app.protocol.Command
import com.teleport.app.protocol.TvState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

object TvEventBus {
    private val _commands = MutableSharedFlow<Command>(extraBufferCapacity = 64)
    val commands: SharedFlow<Command> = _commands.asSharedFlow()

    private val _tvState = MutableStateFlow<TvState?>(null)
    val tvState: StateFlow<TvState?> = _tvState.asStateFlow()

    private val _clientConnected = MutableStateFlow(false)
    val clientConnected: StateFlow<Boolean> = _clientConnected.asStateFlow()

    fun postCommand(command: Command) {
        _commands.tryEmit(command)
    }

    fun updateTvState(state: TvState) {
        _tvState.value = state
    }

    fun setClientConnected(connected: Boolean) {
        _clientConnected.value = connected
    }
}
