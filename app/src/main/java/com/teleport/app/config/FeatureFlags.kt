package com.teleport.app.config

object FeatureFlags {
    /**
     * Set to false to disable paid subscriptions.
     * When disabled, the Pro upgrade paywall is inaccessible, and premium features can be
     * temporarily unlocked by watching a rewarded ad.
     */
    const val ENABLE_PAID_SUBSCRIPTIONS = false

    /**
     * Set to false to disable all advertisements and monetization flows (including rewarded ads).
     * When disabled, no ads (banner, interstitial, rewarded) will be shown, and all premium
     * features are unlocked directly for all users.
     */
    const val ENABLE_ADVERTISEMENTS = false
}
