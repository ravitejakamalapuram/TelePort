package com.teleport.app.review

import android.app.Activity
import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.google.android.play.core.ktx.launchReview
import com.google.android.play.core.ktx.requestReview
import com.google.android.play.core.review.ReviewManagerFactory

private const val TAG = "ReviewPromptManager"

/** Epoch-ms timestamp of the one-shot review request; absent means never asked. */
const val KEY_REVIEW_PROMPT_REQUESTED_AT = "review_prompt_v1_requested_at"

private const val MIN_SESSION_DURATION_MS = 30_000L

interface ReviewLauncher {
    suspend fun launch(activity: Activity): Boolean
}

class PlayReviewLauncher : ReviewLauncher {
    override suspend fun launch(activity: Activity): Boolean {
        return try {
            val manager = ReviewManagerFactory.create(activity)
            val reviewInfo = manager.requestReview()
            manager.launchReview(activity, reviewInfo)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error launching in-app review flow", e)
            false
        }
    }
}

/**
 * Gates the Play in-app review card to at most one request per install, fired when a phone-remote
 * session that demonstrably worked ends. See docs/adr/0001-in-app-review-prompt.md.
 */
class ReviewPromptManager(
    private val prefs: SharedPreferences,
    private val launcher: ReviewLauncher,
    private val now: () -> Long = { System.currentTimeMillis() },
) {
    /** Set by the caller while an in-app update flow is showing, so the two never collide. */
    var isUpdateFlowInProgress: Boolean = false

    private var sessionStartedAt: Long = 0L
    private var sessionHadCast = false
    private var tvStateSeenAfterCast = false

    fun onSessionStarted() {
        sessionStartedAt = now()
        sessionHadCast = false
        tvStateSeenAfterCast = false
    }

    fun onCastSent() {
        sessionHadCast = true
    }

    fun onTvStateReceived() {
        if (sessionHadCast) {
            tvStateSeenAfterCast = true
        }
    }

    suspend fun onSessionEnded(activity: Activity) {
        val hadCast = sessionHadCast
        val sawTvStateAfterCast = tvStateSeenAfterCast
        val durationMs = now() - sessionStartedAt
        sessionHadCast = false
        tvStateSeenAfterCast = false

        try {
            if (prefs.contains(KEY_REVIEW_PROMPT_REQUESTED_AT)) return
            if (!hadCast) return
            if (!sawTvStateAfterCast) return
            if (durationMs < MIN_SESSION_DURATION_MS) return
            if (isUpdateFlowInProgress) return
            val lifecycleOwner = activity as? LifecycleOwner ?: return
            if (lifecycleOwner.lifecycle.currentState != Lifecycle.State.RESUMED) return

            if (launcher.launch(activity)) {
                prefs.edit().putLong(KEY_REVIEW_PROMPT_REQUESTED_AT, now()).apply()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error evaluating in-app review gate", e)
        }
    }
}
