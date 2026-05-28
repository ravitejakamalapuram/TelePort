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

    // Virtual Cursors map (clientId -> CursorInfo)
    data class CursorInfo(
        val id: String,
        val x: Float,
        val y: Float,
        val colorHex: String
    )

    private val CURSOR_COLORS = listOf(
        "#00E5FF", // Neon Cyan
        "#FF007F", // Neon Magenta
        "#00E676", // Emerald Green
        "#FFEB3B", // Vivid Yellow
        "#FF6D00", // Vibrant Orange
        "#9C27B0"  // Purple
    )

    private val _cursors = MutableStateFlow<Map<String, CursorInfo>>(emptyMap())
    val cursors: StateFlow<Map<String, CursorInfo>> = _cursors.asStateFlow()

    // Virtual Cursor Coordinates (in pixels) for legacy compatibility
    val cursorX = MutableStateFlow(500f)
    val cursorY = MutableStateFlow(300f)

    // WebView Dimensions (cached to bounds check cursor)
    private var webViewWidth = 1920
    private var webViewHeight = 1080

    val isDarkModeEnabled = MutableStateFlow(false)
    val isMirroring = MutableStateFlow(false)
    val isResolvingHeadlessly = MutableStateFlow(false)
    val resolvingUrl = MutableStateFlow<String?>(null)
    val isNativePlaying = MutableStateFlow(false)

    private val _headlessWebView = MutableStateFlow<WebView?>(null)
    val headlessWebView: StateFlow<WebView?> = _headlessWebView.asStateFlow()

    private var headlessTimeoutJob: kotlinx.coroutines.Job? = null

    fun startMirroring() {
        isMirroring.value = true
    }

    fun stopMirroring() {
        isMirroring.value = false
    }

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
        coroutineScope.launch {
            isResolvingHeadlessly.collect { syncState() }
        }
        coroutineScope.launch {
            resolvingUrl.collect { syncState() }
        }
        coroutineScope.launch {
            TvEventBus.isNativePlaying.collect { playing ->
                isNativePlaying.value = playing
                syncState()
            }
        }
        // Sync cursors with registered active clients
        coroutineScope.launch {
            TvEventBus.activeClientIds.collect { activeIds ->
                val current = _cursors.value.toMutableMap()
                // Remove disconnected clients
                val toRemove = current.keys.filter { it !in activeIds }
                toRemove.forEach { current.remove(it) }

                // Add new connected clients
                activeIds.forEach { id ->
                    if (id !in current) {
                        val colorIdx = current.size % CURSOR_COLORS.size
                        current[id] = CursorInfo(
                            id = id,
                            x = webViewWidth / 2f,
                            y = webViewHeight / 2f,
                            colorHex = CURSOR_COLORS[colorIdx]
                        )
                    }
                }
                _cursors.value = current
            }
        }
    }

    fun updateDimensions(width: Int, height: Int) {
        webViewWidth = width
        webViewHeight = height
        clampCursor()
    }

    fun openTab(url: String, headless: Boolean = false) {
        coroutineScope.launch(Dispatchers.Main) {
            if (headless) {
                cancelHeadlessExtraction()
                isResolvingHeadlessly.value = true
                resolvingUrl.value = url
                detectedStreamUrl.value = null

                val webView = createWebView(url)
                _headlessWebView.value = webView

                // Start 10-second timeout
                headlessTimeoutJob = coroutineScope.launch(Dispatchers.Main) {
                    kotlinx.coroutines.delay(10000)
                    if (isResolvingHeadlessly.value && _headlessWebView.value == webView) {
                        promoteHeadlessToVisible()
                    }
                }
            } else {
                val webView = createWebView(url)
                val currentList = _tabs.value.toMutableList()
                currentList.add(webView)
                _tabs.value = currentList
                _activeTabIndex.value = currentList.size - 1
                detectedStreamUrl.value = null // Reset stream state
            }
        }
    }

    private fun promoteHeadlessToVisible() {
        coroutineScope.launch(Dispatchers.Main) {
            val webView = _headlessWebView.value ?: return@launch
            headlessTimeoutJob?.cancel()
            headlessTimeoutJob = null
            _headlessWebView.value = null

            val currentList = _tabs.value.toMutableList()
            currentList.add(webView)
            _tabs.value = currentList
            _activeTabIndex.value = currentList.size - 1
            isResolvingHeadlessly.value = false
            resolvingUrl.value = null
        }
    }

    fun cancelHeadlessExtraction() {
        coroutineScope.launch(Dispatchers.Main) {
            headlessTimeoutJob?.cancel()
            headlessTimeoutJob = null
            _headlessWebView.value?.apply {
                stopLoading()
                destroy()
            }
            _headlessWebView.value = null
            isResolvingHeadlessly.value = false
            resolvingUrl.value = null
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

    fun moveCursor(clientId: String, dx: Float, dy: Float) {
        val current = _cursors.value.toMutableMap()
        val cursor = current[clientId]
        if (cursor != null) {
            val newX = (cursor.x + dx).coerceIn(0f, webViewWidth.toFloat())
            val newY = (cursor.y + dy).coerceIn(0f, webViewHeight.toFloat())
            val updated = cursor.copy(x = newX, y = newY)
            current[clientId] = updated
            _cursors.value = current

            // Legacy support for single-pointer references
            if (clientId == _cursors.value.keys.firstOrNull()) {
                cursorX.value = newX
                cursorY.value = newY
            }
        }
    }

    fun clickActive(clientId: String) {
        coroutineScope.launch(Dispatchers.Main) {
            val webView = getActiveWebView() ?: return@launch
            val cursor = _cursors.value[clientId] ?: return@launch
            val x = cursor.x
            val y = cursor.y

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

    fun toggleDarkMode(enabled: Boolean) {
        isDarkModeEnabled.value = enabled
        applyDarkModeToActive()
    }

    fun applyDarkModeToActive() {
        coroutineScope.launch(Dispatchers.Main) {
            val enabled = isDarkModeEnabled.value
            val cssFilter = if (enabled) {
                "document.documentElement.style.filter = 'invert(1) hue-rotate(180deg)';" +
                "var media = document.querySelectorAll('img, video, iframe, canvas, [style*=\"background-image\"]');" +
                "media.forEach(el => el.style.filter = 'invert(1) hue-rotate(180deg)');"
            } else {
                "document.documentElement.style.filter = '';" +
                "var media = document.querySelectorAll('img, video, iframe, canvas, [style*=\"background-image\"]');" +
                "media.forEach(el => el.style.filter = '');"
            }
            val js = "(function() { $cssFilter })();"
            getActiveWebView()?.evaluateJavascript(js, null)
        }
    }

    fun sendTextActive(text: String) {
        coroutineScope.launch(Dispatchers.Main) {
            val jsonText = org.json.JSONObject.quote(text)
            val js = "(function() { " +
                    "  var el = document.activeElement; " +
                    "  if (el && (el.tagName === 'INPUT' || el.tagName === 'TEXTAREA' || el.isContentEditable)) { " +
                    "    el.value = $jsonText; " +
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
                detectedStreamUrl = detectedStreamUrl.value,
                isResolvingHeadlessly = isResolvingHeadlessly.value,
                resolvingUrl = resolvingUrl.value,
                isNativePlaying = isNativePlaying.value
            )
        )
    }

    private fun clampCursor() {
        cursorX.value = cursorX.value.coerceIn(0f, webViewWidth.toFloat())
        cursorY.value = cursorY.value.coerceIn(0f, webViewHeight.toFloat())
    }

    private fun isStreamUrl(url: String): Boolean {
        return url.contains(".m3u8", ignoreCase = true) ||
               url.contains(".mp4", ignoreCase = true) ||
               url.contains(".mkv", ignoreCase = true) ||
               url.contains("googlevideo.com/videoplayback", ignoreCase = true)
    }

    private fun createWebView(url: String): WebView {
        return WebView(context).apply {
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                databaseEnabled = true
                allowFileAccess = false // Prevent local file access and path traversal
                mediaPlaybackRequiresUserGesture = false
                mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE // Prevent active mixed content (MITM XSS risk)
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
                    if (isDarkModeEnabled.value) {
                        applyDarkModeToActive()
                    }
                    syncState()
                }

                // AD-BLOCKING AND MEDIA STREAM EXTRACTION
                override fun shouldInterceptRequest(
                    view: WebView?,
                    request: WebResourceRequest?
                ): WebResourceResponse? {
                    val reqUri = request?.url ?: return null

                    // 1. Intercept Ads
                    // Pass Uri directly to avoid redundant string allocations for blocked requests
                    if (AdBlocker.isAd(reqUri)) {
                        // Return empty response to block the request
                        return WebResourceResponse(
                            "text/plain",
                            "UTF-8",
                            ByteArrayInputStream("".toByteArray())
                        )
                    }

                    // Delay string allocation until after ad blocking to save memory/GC
                    val reqUrl = reqUri.toString()

                    // 2. Extract media stream URLs
                    if (isStreamUrl(reqUrl) && detectedStreamUrl.value != reqUrl) {
                        detectedStreamUrl.value = reqUrl
                        if (isResolvingHeadlessly.value) {
                            coroutineScope.launch(Dispatchers.Main) {
                                playNatively(reqUrl)
                                cancelHeadlessExtraction()
                            }
                        }
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
