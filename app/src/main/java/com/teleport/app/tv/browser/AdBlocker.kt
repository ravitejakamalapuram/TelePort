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

    fun isAd(url: String): Boolean {
        try {
            // android.net.Uri.parse is relatively fast compared to java.net.URL
            // However, avoiding lowercasing the whole URL string.
            val host = android.net.Uri.parse(url).host?.lowercase() ?: return false

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
