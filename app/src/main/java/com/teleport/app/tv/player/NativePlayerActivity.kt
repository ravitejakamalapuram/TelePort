package com.teleport.app.tv.player

import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.teleport.app.protocol.Command
import com.teleport.app.tv.server.TvEventBus
import kotlinx.coroutines.flow.collectLatest

class NativePlayerActivity : ComponentActivity() {
    private val TAG = "NativePlayerActivity"
    private var exoPlayer: ExoPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val streamUrl = intent.getStringExtra("STREAM_URL") ?: ""
        if (streamUrl.isBlank()) {
            finish()
            return
        }

        setContent {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                if (streamUrl.isNotBlank()) {
                    VideoPlayer(streamUrl)
                } else {
                    Text("Invalid Stream Link", color = Color.White, fontSize = 24.sp)
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        TvEventBus.setNativePlaying(true)
    }

    override fun onStop() {
        super.onStop()
        TvEventBus.setNativePlaying(false)
    }

    @OptIn(UnstableApi::class)
    @Composable
    fun VideoPlayer(streamUrl: String) {
        val context = LocalContext.current

        // Initialize ExoPlayer
        val player = remember {
            ExoPlayer.Builder(context).build().apply {
                val mediaItem = MediaItem.fromUri(streamUrl)
                setMediaItem(mediaItem)
                prepare()
                playWhenReady = true
                repeatMode = Player.REPEAT_MODE_OFF
            }
        }
        exoPlayer = player

        // Connect remote controller events to player control
        LaunchedEffect(player) {
            TvEventBus.commands.collectLatest { clientCommand ->
                val command = clientCommand.command
                Log.d(TAG, "Native Player executing remote command: $command")
                when (command) {
                    is Command.PlayPause -> {
                        if (player.isPlaying) player.pause() else player.play()
                    }
                    is Command.Click -> {
                        if (player.isPlaying) player.pause() else player.play()
                    }
                    is Command.GoBack -> {
                        finish()
                    }
                    is Command.Scroll -> {
                        // Map horizontal scrolling to seek forwards/backwards
                        val seekOffset = 10000L // 10 seconds
                        if (command.dx > 50) {
                            player.seekTo(player.currentPosition + seekOffset)
                        } else if (command.dx < -50) {
                            player.seekTo(player.currentPosition - seekOffset)
                        }
                    }
                    else -> {} // Ignore other browser-specific commands inside media player
                }
            }
        }

        DisposableEffect(Unit) {
            onDispose {
                player.release()
                exoPlayer = null
            }
        }

        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    this.player = player
                    useController = true // Enable visual play/pause/timeline controls on TV
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        exoPlayer?.release()
        exoPlayer = null
    }
}
