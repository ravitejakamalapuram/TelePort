package com.teleport.app.tv.player

import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
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
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
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

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        val player = exoPlayer ?: return super.onKeyDown(keyCode, event)
        when (keyCode) {
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                if (player.isPlaying) player.pause() else player.play()
                return true
            }
            KeyEvent.KEYCODE_MEDIA_PLAY -> {
                player.play()
                return true
            }
            KeyEvent.KEYCODE_MEDIA_PAUSE -> {
                player.pause()
                return true
            }
            KeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> {
                player.seekTo((player.currentPosition + 15000L).coerceAtMost(player.duration))
                return true
            }
            KeyEvent.KEYCODE_MEDIA_REWIND -> {
                player.seekTo((player.currentPosition - 15000L).coerceAtLeast(0L))
                return true
            }
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                if (player.isPlaying) player.pause() else player.play()
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    @OptIn(UnstableApi::class)
    @Composable
    fun VideoPlayer(streamUrl: String) {
        val context = LocalContext.current

        // 1. Configure default track selector with dynamic resolution cap & hardware tunneling
        val trackSelector = remember {
            val metrics = context.resources.displayMetrics
            DefaultTrackSelector(context).apply {
                parameters = buildUponParameters()
                    .setMaxVideoSize(metrics.widthPixels, metrics.heightPixels)
                    .setTunnelingEnabled(true)
                    .build()
            }
        }

        // 2. Customized Buffering Control to manage buffer depth and memory footprints
        val loadControl = remember {
            DefaultLoadControl.Builder()
                .setBufferDurationsMs(
                    15_000, // minBufferMs (15s minimum buffer before pausing)
                    50_000, // maxBufferMs (50s maximum buffer)
                    2_500,  // bufferForPlaybackMs (2.5s to start playback quickly)
                    5_000   // bufferForPlaybackAfterRebufferMs (5s stability buffer)
                )
                .setPrioritizeTimeOverSizeThresholds(true)
                .build()
        }

        // 3. Custom Http connection timeout factory + cache layer wrapper
        val dataSourceFactory = remember {
            val httpFactory = DefaultHttpDataSource.Factory()
                .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .setConnectTimeoutMs(15_000)
                .setReadTimeoutMs(15_000)
                .setAllowCrossProtocolRedirects(true)

            val cache = VideoCacheManager.getCache(context)
            if (cache != null) {
                CacheDataSource.Factory()
                    .setCache(cache)
                    .setUpstreamDataSourceFactory(httpFactory)
                    .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
            } else {
                httpFactory
            }
        }

        val mediaSourceFactory = remember {
            DefaultMediaSourceFactory(dataSourceFactory)
        }

        // Initialize ExoPlayer with optimized settings
        val player = remember {
            ExoPlayer.Builder(context)
                .setTrackSelector(trackSelector)
                .setLoadControl(loadControl)
                .setMediaSourceFactory(mediaSourceFactory)
                .build().apply {
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
                if (command is Command.MoveCursor || command is Command.Scroll) return@collectLatest
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
                            player.seekTo((player.currentPosition + seekOffset).coerceAtMost(player.duration))
                        } else if (command.dx < -50) {
                            player.seekTo((player.currentPosition - seekOffset).coerceAtLeast(0L))
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
                    
                    // Request focus to intercept physical remote control events
                    isFocusable = true
                    isFocusableInTouchMode = true
                    requestFocus()
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
