package com.teleport.app.tv.browser

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.SystemClock
import android.view.MotionEvent
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import com.teleport.app.protocol.TabInfo
import com.teleport.app.protocol.TvState
import com.teleport.app.tv.player.NativePlayerActivity
import com.teleport.app.tv.server.TvEventBus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.ByteArrayInputStream

class TabManager(private val context: Context, private val coroutineScope: CoroutineScope) {

    private val _tabs = MutableStateFlow<List<WebView>>(emptyList())
    val tabs: StateFlow<List<WebView>> = _tabs.asStateFlow()

    private val _activeTabIndex = MutableStateFlow(-1)
    val activeTabIndex: StateFlow<Int> = _activeTabIndex.asStateFlow()

    // Currently detected media stream on active web page
    private val detectedStreamUrl = MutableStateFlow<String?>(null)

    // Virtual Cursor Coordinates (in pixels)
    val cursorX = MutableStateFlow(500f)
    val cursorY = MutableStateFlow(300f)

    // WebView Dimensions (cached to bounds check cursor)
    private var webViewWidth = 1920
    private var webViewHeight = 1080

    init {
        // Collect state updates and push them to the TvEventBus
        coroutineScope.launch {
            _tabs.collect { syncState() }
        }
        coroutineScope.launch {
            _activeTabIndex.collect { syncState() }
        }
        coroutineScope.launch {
            detectedStreamUrl.collect { syncState() }
        }
    }

    fun updateDimensions(width: Int, height: Int) {
        webViewWidth = width
        webViewHeight = height
        clampCursor()
    }

    fun openTab(url: String) {
        coroutineScope.launch(Dispatchers.Main) {
            val webView = createWebView(url)
            val currentList = _tabs.value.toMutableList()
            currentList.add(webView)
            _tabs.value = currentList
            _activeTabIndex.value = currentList.size - 1
            detectedStreamUrl.value = null // Reset stream state
        }
    }

    fun closeTab(index: Int) {
        coroutineScope.launch(Dispatchers.Main) {
            val currentList = _tabs.value.toMutableList()
            if (index in currentList.indices) {
                val webView = currentList.removeAt(index)
                webView.destroy()
                _tabs.value = currentList

                val activeIndex = _activeTabIndex.value
                when {
                    currentList.isEmpty() -> {
                        _activeTabIndex.value = -1
                        detectedStreamUrl.value = null
                    }
                    activeIndex >= currentList.size -> _activeTabIndex.value = currentList.size - 1
                    activeIndex == index -> _activeTabIndex.value = (activeIndex - 1).coerceAtLeast(0)
                }
            }
        }
    }

    fun selectTab(index: Int) {
        if (index in _tabs.value.indices) {
            _activeTabIndex.value = index
            detectedStreamUrl.value = null // Reset stream for tab swap (will re-extract on resource load)
        }
    }

    fun getActiveWebView(): WebView? {
        val index = _activeTabIndex.value
        return if (index in _tabs.value.indices) _tabs.value[index] else null
    }

    // --- Commands execution ---

    fun scrollActive(dx: Float, dy: Float) {
        coroutineScope.launch(Dispatchers.Main) {
            getActiveWebView()?.scrollBy(dx.toInt(), dy.toInt())
        }
    }

    fun moveCursor(dx: Float, dy: Float) {
        cursorX.value = (cursorX.value + dx).coerceIn(0f, webViewWidth.toFloat())
        cursorY.value = (cursorY.value + dy).coerceIn(0f, webViewHeight.toFloat())
    }

    fun clickActive() {
        coroutineScope.launch(Dispatchers.Main) {
            val webView = getActiveWebView() ?: return@launch
            val x = cursorX.value
            val y = cursorY.value

            val downTime = SystemClock.uptimeMillis()
            val eventTime = SystemClock.uptimeMillis()

            val downEvent = MotionEvent.obtain(downTime, eventTime, MotionEvent.ACTION_DOWN, x, y, 0)
            val upEvent = MotionEvent.obtain(downTime, eventTime + 50, MotionEvent.ACTION_UP, x, y, 0)

            webView.dispatchTouchEvent(downEvent)
            webView.dispatchTouchEvent(upEvent)

            downEvent.recycle()
            upEvent.recycle()
        }
    }

    fun sendTextActive(text: String) {
        coroutineScope.launch(Dispatchers.Main) {
            val escapedText = text.replace("'", "\\'")
            val js = "(function() { " +
                    "  var el = document.activeElement; " +
                    "  if (el && (el.tagName === 'INPUT' || el.tagName === 'TEXTAREA' || el.isContentEditable)) { " +
                    "    el.value = '$escapedText'; " +
                    "    el.dispatchEvent(new Event('input', { bubbles: true })); " +
                    "    el.dispatchEvent(new Event('change', { bubbles: true })); " +
                    "  } " +
                    "})();"
            getActiveWebView()?.evaluateJavascript(js, null)
        }
    }

    fun playPauseActive() {
        coroutineScope.launch(Dispatchers.Main) {
            val js = "(function() { " +
                    "  var videos = document.querySelectorAll('video'); " +
                    "  if (videos.length > 0) { " +
                    "    var firstVideo = videos[0]; " +
                    "    if (firstVideo.paused) { firstVideo.play(); } else { firstVideo.pause(); } " +
                    "  } " +
                    "})();"
            getActiveWebView()?.evaluateJavascript(js, null)
        }
    }

    fun goBackActive() {
        coroutineScope.launch(Dispatchers.Main) {
            val webView = getActiveWebView()
            if (webView?.canGoBack() == true) {
                webView.goBack()
            }
        }
    }

    fun playNatively(streamUrl: String) {
        coroutineScope.launch(Dispatchers.Main) {
            val intent = Intent(context, NativePlayerActivity::class.java).apply {
                putExtra("STREAM_URL", streamUrl)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }

    // --- Helper UI Sync ---

    private fun syncState() {
        val tabInfos = _tabs.value.map {
            TabInfo(
                url = it.url ?: "",
                title = it.title ?: "Loading...",
                isLoading = false
            )
        }
        TvEventBus.updateTvState(
            TvState(
                tabs = tabInfos,
                activeTabIndex = _activeTabIndex.value,
                detectedStreamUrl = detectedStreamUrl.value
            )
        )
    }

    private fun clampCursor() {
        cursorX.value = cursorX.value.coerceIn(0f, webViewWidth.toFloat())
        cursorY.value = cursorY.value.coerceIn(0f, webViewHeight.toFloat())
    }

    private fun isStreamUrl(url: String): Boolean {
        val lowercase = url.lowercase()
        return lowercase.contains(".m3u8") ||
               lowercase.contains(".mp4") ||
               lowercase.contains(".mkv") ||
               lowercase.contains("googlevideo.com/videoplayback")
    }

    private fun createWebView(url: String): WebView {
        return WebView(context).apply {
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                databaseEnabled = true
                mediaPlaybackRequiresUserGesture = false
                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
            }

            webViewClient = object : WebViewClient() {
                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                    super.onPageStarted(view, url, favicon)
                    detectedStreamUrl.value = null // Reset stream url on new page
                    syncState()
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    syncState()
                }

                // AD-BLOCKING AND MEDIA STREAM EXTRACTION
                override fun shouldInterceptRequest(
                    view: WebView?,
                    request: WebResourceRequest?
                ): WebResourceResponse? {
                    val reqUrl = request?.url?.toString() ?: return null

                    // 1. Intercept Ads
                    if (AdBlocker.isAd(reqUrl)) {
                        // Return empty response to block the request
                        return WebResourceResponse(
                            "text/plain",
                            "UTF-8",
                            ByteArrayInputStream("".toByteArray())
                        )
                    }

                    // 2. Extract media stream URLs
                    if (isStreamUrl(reqUrl) && detectedStreamUrl.value != reqUrl) {
                        detectedStreamUrl.value = reqUrl
                    }

                    return super.shouldInterceptRequest(view, request)
                }
            }

            webChromeClient = object : WebChromeClient() {
                override fun onReceivedTitle(view: WebView?, title: String?) {
                    super.onReceivedTitle(view, title)
                    syncState()
                }
            }

            loadUrl(url)
        }
    }
}
