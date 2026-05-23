package com.teleport.app.tv.browser

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdBlockerTest {

    @Test
    fun testIsAd_withAdDomains_shouldReturnTrue() {
        assertTrue(AdBlocker.isAd("https://doubleclick.net/ads/banner.js"))
        assertTrue(AdBlocker.isAd("http://googleads.g.doubleclick.net/pagead/ads"))
        assertTrue(AdBlocker.isAd("https://pagead2.googlesyndication.com/gampad/ads"))
        assertTrue(AdBlocker.isAd("https://subdomain.popads.net/index.html"))
        assertTrue(AdBlocker.isAd("https://exoclick.com/"))
    }

    @Test
    fun testIsAd_withSafeDomains_shouldReturnFalse() {
        assertFalse(AdBlocker.isAd("https://google.com/"))
        assertFalse(AdBlocker.isAd("https://github.com/ravitejakamalapuram/TelePort"))
        assertFalse(AdBlocker.isAd("https://en.wikipedia.org/wiki/Android_TV"))
    }

    @Test
    fun testIsAd_withMalformedUrls_shouldReturnFalse() {
        assertFalse(AdBlocker.isAd("not-a-valid-url"))
        assertFalse(AdBlocker.isAd(""))
    }
}
