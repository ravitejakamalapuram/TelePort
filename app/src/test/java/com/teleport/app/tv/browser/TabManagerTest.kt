package com.teleport.app.tv.browser

import android.content.Context
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * Regression test for POR-158: a headless cast (`OpenUrl(headless = true)`) must leave
 * `isResolvingHeadlessly` observably true so TvEventBus.updateTvState() broadcasts a state the
 * connected phone can see. Previously `cancelHeadlessExtraction()`'s own `launch(Dispatchers.Main)`
 * queued *after* the rest of openTab()'s body (even though it was invoked first), so it ran last
 * and reverted isResolvingHeadlessly/resolvingUrl/headlessWebView right back to their pre-cast
 * values before the main looper went idle.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class TabManagerTest {

    @Test
    fun `headless openTab leaves isResolvingHeadlessly true after the main looper drains`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val scope = CoroutineScope(Dispatchers.Main)
        val tabManager = TabManager(context, scope)

        tabManager.openTab("https://example.com", headless = true)
        shadowOf(Looper.getMainLooper()).idle()

        assertTrue(tabManager.isResolvingHeadlessly.value)
        assertEquals("https://example.com", tabManager.resolvingUrl.value)
        assertNotNull(tabManager.headlessWebView.value)
    }

    @Test
    fun `a second headless openTab still cancels the first resolution`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val scope = CoroutineScope(Dispatchers.Main)
        val tabManager = TabManager(context, scope)

        tabManager.openTab("https://first.example.com", headless = true)
        shadowOf(Looper.getMainLooper()).idle()
        val firstWebView = tabManager.headlessWebView.value

        tabManager.openTab("https://second.example.com", headless = true)
        shadowOf(Looper.getMainLooper()).idle()

        assertTrue(tabManager.isResolvingHeadlessly.value)
        assertEquals("https://second.example.com", tabManager.resolvingUrl.value)
        assertNotNull(tabManager.headlessWebView.value)
        assertTrue(tabManager.headlessWebView.value !== firstWebView)
    }
}
