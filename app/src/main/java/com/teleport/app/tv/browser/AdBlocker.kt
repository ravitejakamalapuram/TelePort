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

    fun isAd(uri: android.net.Uri?): Boolean {
        if (uri == null) return false
        try {
            var host = uri.host ?: return false

            // .lowercase() internally checks for case and avoids allocation if already lowercase
            host = host.lowercase()

            // O(1) domain lookup checking host and its parent domains
            var currentHost = host
            while (currentHost.isNotEmpty()) {
                if (AD_DOMAINS.contains(currentHost)) {
                    try {
                        Log.d(TAG, "Blocked Ad Request: $uri")
                    } catch (e: Throwable) {
                        println("Blocked Ad Request: $uri")
                    }
                    return true
                }
                val dotIndex = currentHost.indexOf('.')
                if (dotIndex == -1) break
                currentHost = currentHost.substring(dotIndex + 1)
            }
        } catch (e: Exception) {
            // Ignore
        }
        return false
    }

    fun isAd(url: String?): Boolean {
        if (url.isNullOrEmpty()) return false
        return try {
            isAd(android.net.Uri.parse(url))
        } catch (e: Exception) {
            false
        }
    }
}
