package com.teleport.app

import com.teleport.app.tv.browser.AdBlocker
import org.junit.Test
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse

class AdBlockerTest {
    @Test
    fun testAdBlocker() {
        // Exact match
        assertTrue(AdBlocker.isAd("https://doubleclick.net/path"))

        // Subdomain match
        assertTrue(AdBlocker.isAd("https://ads.doubleclick.net/test"))
        assertTrue(AdBlocker.isAd("https://deep.sub.googleads.g.doubleclick.net/ad"))

        // Non-matching domains
        assertFalse(AdBlocker.isAd("https://example.com"))
        assertFalse(AdBlocker.isAd("https://notdoubleclick.net")) // Similar prefix

        // Malformed URLs
        assertFalse(AdBlocker.isAd("not_a_url"))
    }
}
