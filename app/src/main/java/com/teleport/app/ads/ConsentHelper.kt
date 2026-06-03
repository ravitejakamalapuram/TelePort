package com.teleport.app.ads

import android.app.Activity
import android.util.Log
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform

/**
 * Manages user consent for personalized ads using Google's UMP SDK.
 * Must be called before initializing AdMob.
 */
object ConsentHelper {
    private const val TAG = "ConsentHelper"
    private var consentInformation: ConsentInformation? = null

    /**
     * Request consent info and show the consent form if required.
     * @param activity The hosting Activity
     * @param onConsentResult Called with `true` if ads can be requested, `false` otherwise
     * @param isDebug If true, uses debug geography to simulate EU consent flow
     */
    fun requestConsent(
        activity: Activity,
        onConsentResult: (canRequestAds: Boolean) -> Unit,
        isDebug: Boolean = false
    ) {
        val paramsBuilder = ConsentRequestParameters.Builder()
            .setTagForUnderAgeOfConsent(false)

        if (isDebug) {
            val debugSettings = ConsentDebugSettings.Builder(activity)
                .setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA)
                .build()
            paramsBuilder.setConsentDebugSettings(debugSettings)
        }

        val params = paramsBuilder.build()
        val consent = UserMessagingPlatform.getConsentInformation(activity)
        consentInformation = consent

        consent.requestConsentInfoUpdate(
            activity,
            params,
            {
                // Consent info updated successfully
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { formError ->
                    if (formError != null) {
                        Log.e(TAG, "Consent form error: ${formError.message}")
                    }
                    onConsentResult(consent.canRequestAds())
                }
            },
            { requestError ->
                Log.e(TAG, "Consent info update failed: ${requestError.message}")
                // If consent check fails, default to allowing contextual (non-personalized) ads
                onConsentResult(true)
            }
        )
    }

    /** Check if consent has been obtained and ads can be requested. */
    fun canRequestAds(): Boolean {
        return consentInformation?.canRequestAds() ?: false
    }

    /** Reset consent state (for testing or user-requested privacy reset). */
    fun reset(activity: Activity) {
        UserMessagingPlatform.getConsentInformation(activity).reset()
        consentInformation = null
    }
}
