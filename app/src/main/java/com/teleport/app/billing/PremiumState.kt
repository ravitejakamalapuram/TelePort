package com.teleport.app.billing

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Singleton that holds the user's premium subscription state.
 * Observed by AdManager (to suppress ads) and UI components (to gate features).
 */
object PremiumState {
    enum class Tier {
        FREE,
        PRO,
        PRO_PLUS
    }

    private val _tier = MutableStateFlow(Tier.FREE)
    val tier: StateFlow<Tier> = _tier.asStateFlow()

    /** Whether the user has any paid tier (Pro or Pro+). */
    val isPremium: Boolean get() = _tier.value != Tier.FREE

    /** Whether the user has the highest tier. */
    val isProPlus: Boolean get() = _tier.value == Tier.PRO_PLUS

    fun updateTier(newTier: Tier) {
        _tier.value = newTier
    }
}
