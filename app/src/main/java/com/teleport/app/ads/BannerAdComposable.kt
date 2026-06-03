package com.teleport.app.ads

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

/**
 * A Compose-friendly banner ad component.
 * Automatically hides when the user is premium or in a temporary ad-free window.
 */
@Composable
fun BannerAd(modifier: Modifier = Modifier) {
    val shouldShow = AdManager.shouldShowAds()
    if (!shouldShow) return

    AndroidView(
        factory = { ctx -> AdManager.createBannerAdView(ctx) },
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
    )
}
