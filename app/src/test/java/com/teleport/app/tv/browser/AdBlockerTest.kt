package com.teleport.app.tv.browser

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class AdBlockerTest {

    @Test
    fun testIsAd_withAdDomains_shouldReturnTrue() {
        assertTrue(AdBlocker.isAd(android.net.Uri.parse("https://doubleclick.net/ads/banner.js")))
        assertTrue(AdBlocker.isAd(android.net.Uri.parse("http://googleads.g.doubleclick.net/pagead/ads")))
        assertTrue(AdBlocker.isAd(android.net.Uri.parse("https://pagead2.googlesyndication.com/gampad/ads")))
        assertTrue(AdBlocker.isAd(android.net.Uri.parse("https://subdomain.popads.net/index.html")))
        assertTrue(AdBlocker.isAd(android.net.Uri.parse("https://exoclick.com/")))
    }

    @Test
    fun testIsAd_withSafeDomains_shouldReturnFalse() {
        assertFalse(AdBlocker.isAd(android.net.Uri.parse("https://google.com/")))
        assertFalse(AdBlocker.isAd(android.net.Uri.parse("https://github.com/ravitejakamalapuram/TelePort")))
        assertFalse(AdBlocker.isAd(android.net.Uri.parse("https://en.wikipedia.org/wiki/Android_TV")))
    }

    @Test
    fun testIsAd_withMalformedUrls_shouldReturnFalse() {
        assertFalse(AdBlocker.isAd(android.net.Uri.parse("not-a-valid-url")))
        assertFalse(AdBlocker.isAd(android.net.Uri.parse("")))
    }
}
