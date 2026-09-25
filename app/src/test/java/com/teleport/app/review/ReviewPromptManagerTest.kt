package com.teleport.app.review

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import androidx.activity.ComponentActivity
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

private class FakeReviewLauncher(
    private val result: Boolean = true,
    private val throwOnLaunch: Boolean = false,
) : ReviewLauncher {
    var launchCount = 0
        private set

    override suspend fun launch(activity: Activity): Boolean {
        launchCount++
        if (throwOnLaunch) throw RuntimeException("boom")
        return result
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ReviewPromptManagerTest {

    private var clockMs = 0L
    private lateinit var prefs: SharedPreferences

    @Before
    fun setUp() {
        clockMs = 0L
        prefs = ApplicationProvider.getApplicationContext<Context>()
            .getSharedPreferences("review_prompt_manager_test_prefs", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
    }

    private fun manager(launcher: ReviewLauncher) =
        ReviewPromptManager(prefs = prefs, launcher = launcher, now = { clockMs })

    private fun resumedActivity(): Activity =
        Robolectric.buildActivity(ComponentActivity::class.java).setup().get()

    /** Drives a full session through the manager; `durationMs` elapses before it ends. */
    private fun runSession(
        manager: ReviewPromptManager,
        activity: Activity,
        cast: Boolean = true,
        tvStateAfterCast: Boolean = true,
        durationMs: Long = 30_000L,
    ) = runBlocking {
        manager.onSessionStarted()
        if (cast) manager.onCastSent()
        if (tvStateAfterCast) manager.onTvStateReceived()
        clockMs += durationMs
        manager.onSessionEnded(activity)
    }

    @Test
    fun `qualifying session asks exactly once`() {
        val launcher = FakeReviewLauncher()
        val manager = manager(launcher)
        runSession(manager, resumedActivity())

        assertEquals(1, launcher.launchCount)
        assertTrue(prefs.contains(KEY_REVIEW_PROMPT_REQUESTED_AT))
    }

    @Test
    fun `second qualifying session does not ask again`() {
        val launcher = FakeReviewLauncher()
        val manager = manager(launcher)
        val activity = resumedActivity()
        runSession(manager, activity)
        runSession(manager, activity)

        assertEquals(1, launcher.launchCount)
    }

    @Test
    fun `session with no cast never asks`() {
        val launcher = FakeReviewLauncher()
        val manager = manager(launcher)
        runSession(manager, resumedActivity(), cast = false)

        assertEquals(0, launcher.launchCount)
        assertFalse(prefs.contains(KEY_REVIEW_PROMPT_REQUESTED_AT))
    }

    @Test
    fun `cast with no TvState after it never asks`() {
        val launcher = FakeReviewLauncher()
        val manager = manager(launcher)
        runSession(manager, resumedActivity(), tvStateAfterCast = false)

        assertEquals(0, launcher.launchCount)
        assertFalse(prefs.contains(KEY_REVIEW_PROMPT_REQUESTED_AT))
    }

    @Test
    fun `session shorter than 30s never asks`() {
        val launcher = FakeReviewLauncher()
        val manager = manager(launcher)
        runSession(manager, resumedActivity(), durationMs = 29_999L)

        assertEquals(0, launcher.launchCount)
        assertFalse(prefs.contains(KEY_REVIEW_PROMPT_REQUESTED_AT))
    }

    @Test
    fun `not resumed suppresses without spending the shot`() {
        val launcher = FakeReviewLauncher()
        val manager = manager(launcher)
        val activity = Robolectric.buildActivity(ComponentActivity::class.java).create().get()
        runSession(manager, activity)

        assertEquals(0, launcher.launchCount)
        assertFalse(prefs.contains(KEY_REVIEW_PROMPT_REQUESTED_AT))
    }

    @Test
    fun `update flow in progress suppresses without spending the shot`() {
        val launcher = FakeReviewLauncher()
        val manager = manager(launcher)
        manager.isUpdateFlowInProgress = true
        runSession(manager, resumedActivity())

        assertEquals(0, launcher.launchCount)
        assertFalse(prefs.contains(KEY_REVIEW_PROMPT_REQUESTED_AT))
    }

    @Test
    fun `requestReview failure suppresses without spending the shot`() {
        val launcher = FakeReviewLauncher(result = false)
        val manager = manager(launcher)
        runSession(manager, resumedActivity())

        assertEquals(1, launcher.launchCount)
        assertFalse(prefs.contains(KEY_REVIEW_PROMPT_REQUESTED_AT))
    }

    @Test
    fun `a throwing launcher cannot break session end`() {
        val manager = manager(FakeReviewLauncher(throwOnLaunch = true))

        // Must not throw out of onSessionEnded.
        runSession(manager, resumedActivity())

        assertFalse(prefs.contains(KEY_REVIEW_PROMPT_REQUESTED_AT))
    }
}
