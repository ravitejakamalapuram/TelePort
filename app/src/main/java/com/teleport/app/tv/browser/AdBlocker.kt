package com.teleport.app.tv.browser

import android.net.Uri
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
            val uri = Uri.parse(url)
            val host = uri.host?.lowercase() ?: return false

            // Check if host matches or ends with any ad domain
            for (adDomain in AD_DOMAINS) {
                if (host == adDomain || host.endsWith(".$adDomain")) {
                    Log.d(TAG, "Blocked Ad Request: $url")
                    return true
                }
            }
        } catch (e: Exception) {
            // Ignore malformed URLs
        }
        return false
    }
}
