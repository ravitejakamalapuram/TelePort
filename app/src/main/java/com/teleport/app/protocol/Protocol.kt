package com.teleport.app.protocol

import kotlinx.serialization.Serializable

@Serializable
sealed class Command {
    @Serializable
    data class OpenUrl(val url: String) : Command()

    @Serializable
    data class CloseTab(val index: Int) : Command()

    @Serializable
    data class SelectTab(val index: Int) : Command()

    @Serializable
    data class Scroll(val dx: Float, val dy: Float) : Command()

    @Serializable
    data class MoveCursor(val dx: Float, val dy: Float) : Command()

    @Serializable
    object Click : Command()

    @Serializable
    data class SendText(val text: String) : Command()

    @Serializable
    object PlayPause : Command()

    @Serializable
    object GoBack : Command()

    @Serializable
    data class SetAirRemoteMode(val enabled: Boolean) : Command()

    @Serializable
    data class PlayStreamNatively(val streamUrl: String) : Command()

    @Serializable
    data class ToggleDarkMode(val enabled: Boolean) : Command()

    @Serializable
    object StartMirroring : Command()

    @Serializable
    object StopMirroring : Command()
}

@Serializable
data class TabInfo(
    val url: String,
    val title: String,
    val isLoading: Boolean
)

@Serializable
data class TvState(
    val tabs: List<TabInfo>,
    val activeTabIndex: Int,
    val detectedStreamUrl: String? = null // Extracted video stream URL (if any) on active page
)
