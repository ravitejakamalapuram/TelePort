package com.teleport.app.tv.browser

import android.util.Log

object AdBlocker {
    private val TAG = "AdBlocker"

    // A compiled list of common advertising, tracking, and redirect domains
    private val AD_DOMAINS = hashSetOf(
        "doubleclick.net",
        "googleads.g.doubleclick.net",
        "googlesyndication.com",
        "adservice.google.com",
        "pagead2.googlesyndication.com",
        "ads.pubmatic.com",
        "adnxs.com",
        "openx.net",
        "casalemedia.com",
        "rubiconproject.com",
        "criteo.com",
        "outbrain.com",
        "taboola.com",
        "adroll.com",
        "popads.net",
        "popcash.net",
        "propellerads.com",
        "exoclick.com",
        "juicyads.com",
        "onclickads.net",
        "adsterra.com",
        "yandex.ru",
        "an.yandex.ru",
        "adnxs-simple.com"
    )

    // Optimization: avoid Uri.parse() allocations in high-frequency callbacks
    // like WebViewClient.shouldInterceptRequest to reduce garbage collection overhead
    private fun extractHost(url: String): String? {
        val schemeEnd = url.indexOf("://")
        if (schemeEnd == -1) return null

        var start = schemeEnd + 3
        var end = url.length

        for (i in start until url.length) {
            val c = url[i]
            if (c == '/' || c == '?' || c == '#') {
                end = i
                break
            }
        }

        val atIndex = url.indexOf('@', start)
        if (atIndex != -1 && atIndex < end) start = atIndex + 1

        val portIndex = url.indexOf(':', start)
        if (portIndex != -1 && portIndex < end) end = portIndex

        if (start >= end) return null
        return url.substring(start, end).lowercase()
    }

    fun isAd(url: String): Boolean {
        try {
            val host = extractHost(url) ?: return false

            // O(1) domain lookup checking host and its parent domains
            var currentHost = host
            while (currentHost.isNotEmpty()) {
                if (AD_DOMAINS.contains(currentHost)) {
                    try {
                        Log.d(TAG, "Blocked Ad Request: $url")
                    } catch (e: Throwable) {
                        println("Blocked Ad Request: $url")
                    }
                    return true
                }
                val dotIndex = currentHost.indexOf('.')
                if (dotIndex == -1) break
                currentHost = currentHost.substring(dotIndex + 1)
            }
        } catch (e: Exception) {
            // Ignore malformed URLs
        }
        return false
    }
}
