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
            val host = java.net.URL(url).host?.lowercase() ?: return false

            // Check exact match
            if (AD_DOMAINS.contains(host)) {
                logBlock(url)
                return true
            }

            // Check parent domains
            var dotIndex = host.indexOf('.')
            while (dotIndex != -1) {
                if (AD_DOMAINS.contains(host.substring(dotIndex + 1))) {
                    logBlock(url)
                    return true
                }
                dotIndex = host.indexOf('.', dotIndex + 1)
            }
        } catch (e: Exception) {
            // Ignore malformed URLs
        }
        return false
    }

    private fun logBlock(url: String) {
        try {
            Log.d(TAG, "Blocked Ad Request: $url")
        } catch (e: Throwable) {
            println("Blocked Ad Request: $url")
        }
    }
}
