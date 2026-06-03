package com.teleport.app.mobile

import android.app.Activity
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.billingclient.api.ProductDetails
import com.teleport.app.ads.AdManager
import com.teleport.app.billing.BillingManager
import com.teleport.app.billing.PremiumState
import com.teleport.app.ui.theme.ThemeTokens

// ──────────────────────────────────────────────────────────────────────────────
// PaywallScreen – full-screen premium upgrade overlay for TelePort
// ──────────────────────────────────────────────────────────────────────────────

@Composable
fun PaywallScreen(
    billingManager: BillingManager,
    onDismiss: () -> Unit
) {
    val products by billingManager.subscriptionProducts.collectAsState()
    val currentTier by PremiumState.tier.collectAsState()
    val activity = LocalContext.current as Activity

    // ── Selected plan index (0 = Pro, 1 = Pro+) ──
    var selectedPlanIndex by remember { mutableIntStateOf(0) }
    // ── Selected billing period index (0 = Monthly, 1 = Yearly) ──
    var selectedPeriodIndex by remember { mutableIntStateOf(0) }

    val hasBillingProducts = products.isNotEmpty()

    // Resolve product details for selected plan + period
    val selectedProductId = when {
        selectedPlanIndex == 0 && selectedPeriodIndex == 0 -> BillingManager.PRODUCT_PRO_MONTHLY
        selectedPlanIndex == 0 && selectedPeriodIndex == 1 -> BillingManager.PRODUCT_PRO_YEARLY
        selectedPlanIndex == 1 && selectedPeriodIndex == 0 -> BillingManager.PRODUCT_PRO_PLUS_MONTHLY
        else -> BillingManager.PRODUCT_PRO_PLUS_YEARLY
    }
    val selectedProduct = products.firstOrNull { it.productId == selectedProductId }

    // Gradient brushes
    val proGradient = Brush.linearGradient(
        colors = listOf(ThemeTokens.Primary, ThemeTokens.Primary.copy(alpha = 0.6f))
    )
    val proPlusGradient = Brush.linearGradient(
        colors = listOf(ThemeTokens.Accent, ThemeTokens.Primary)
    )

    // ── Full-screen overlay ──
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ThemeTokens.Background.copy(alpha = 0.96f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 16.dp, bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ═════════════════════════════════════════════════════════
            // 1. HEADER
            // ═════════════════════════════════════════════════════════
            Box(modifier = Modifier.fillMaxWidth()) {
                // Close button – top-right
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(40.dp)
                        .background(ThemeTokens.CardBg, CircleShape)
                ) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = "Close",
                        tint = ThemeTokens.TextSub,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Crown icon
                    Text("👑", fontSize = 48.sp)
                    Spacer(modifier = Modifier.height(8.dp))

                    // Title with gradient feel
                    Text(
                        text = "Upgrade to Pro",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = ThemeTokens.TextMain
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Unlock the full power of ${ThemeTokens.APP_NAME}",
                        fontSize = 14.sp,
                        color = ThemeTokens.TextSub,
                        textAlign = TextAlign.Center
                    )

                    // Current tier badge
                    if (currentTier != PremiumState.Tier.FREE) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Current plan: ${currentTier.name.replace('_', ' ')}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = ThemeTokens.Success,
                            modifier = Modifier
                                .background(
                                    ThemeTokens.Success.copy(alpha = 0.12f),
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ═════════════════════════════════════════════════════════
            // 2. FEATURE COMPARISON TABLE
            // ═════════════════════════════════════════════════════════
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = ThemeTokens.CardBg),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Table header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Feature",
                            color = ThemeTokens.TextSub,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1.4f)
                        )
                        Text(
                            "Free",
                            color = ThemeTokens.TextSub,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(0.8f)
                        )
                        Text(
                            "Pro",
                            color = ThemeTokens.Primary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(0.8f)
                        )
                        Text(
                            "Pro+",
                            color = ThemeTokens.Accent,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(0.8f)
                        )
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = ThemeTokens.Border
                    )

                    // Feature rows
                    FeatureRow("Browser Tabs", "3", "Unlimited", "Unlimited")
                    FeatureRow("Advertisements", "Yes", "None", "None")
                    FeatureRow("Ad Blocker", "Basic", "Extended", "Extended")
                    FeatureRow("Trackpad + D-Pad", "✓", "✓", "✓")
                    FeatureRow("Air Mouse", "—", "✓", "✓")
                    FeatureRow("Screen Mirroring", "—", "✓", "✓")
                    FeatureRow("Concurrent Remotes", "1", "1", "5")
                    FeatureRow("Picture-in-Picture", "—", "—", "✓")
                    FeatureRow("Priority Support", "—", "—", "✓")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ═════════════════════════════════════════════════════════
            // 3. PLAN SELECTOR TABS  (Pro / Pro+)
            // ═════════════════════════════════════════════════════════
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ThemeTokens.CardBg, RoundedCornerShape(12.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                PlanTab(
                    label = "⭐  Pro",
                    isSelected = selectedPlanIndex == 0,
                    accentColor = ThemeTokens.Primary,
                    modifier = Modifier.weight(1f),
                    onClick = { selectedPlanIndex = 0 }
                )
                Spacer(modifier = Modifier.width(4.dp))
                PlanTab(
                    label = "👑  Pro+",
                    isSelected = selectedPlanIndex == 1,
                    accentColor = ThemeTokens.Accent,
                    modifier = Modifier.weight(1f),
                    onClick = { selectedPlanIndex = 1 }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ═════════════════════════════════════════════════════════
            // 4. SUBSCRIPTION CARDS  (Monthly / Yearly side-by-side)
            // ═════════════════════════════════════════════════════════
            val planGradient = if (selectedPlanIndex == 0) proGradient else proPlusGradient
            val planAccent = if (selectedPlanIndex == 0) ThemeTokens.Primary else ThemeTokens.Accent

            // Resolve monthly & yearly for current plan
            val monthlyProductId = if (selectedPlanIndex == 0)
                BillingManager.PRODUCT_PRO_MONTHLY else BillingManager.PRODUCT_PRO_PLUS_MONTHLY
            val yearlyProductId = if (selectedPlanIndex == 0)
                BillingManager.PRODUCT_PRO_YEARLY else BillingManager.PRODUCT_PRO_PLUS_YEARLY

            val monthlyProduct = products.firstOrNull { it.productId == monthlyProductId }
            val yearlyProduct = products.firstOrNull { it.productId == yearlyProductId }

            val monthlyPrice = monthlyProduct?.formattedPrice() ?: if (selectedPlanIndex == 0) "$2.99" else "$4.99"
            val yearlyPrice = yearlyProduct?.formattedPrice() ?: if (selectedPlanIndex == 0) "$24.99" else "$39.99"

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Max),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Monthly card
                SubscriptionCard(
                    label = "Monthly",
                    price = monthlyPrice,
                    period = "/month",
                    savingsBadge = null,
                    isSelected = selectedPeriodIndex == 0,
                    gradient = planGradient,
                    accentColor = planAccent,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    onClick = { selectedPeriodIndex = 0 }
                )

                // Yearly card
                SubscriptionCard(
                    label = "Yearly",
                    price = yearlyPrice,
                    period = "/year",
                    savingsBadge = "Save ~30%",
                    isSelected = selectedPeriodIndex == 1,
                    gradient = planGradient,
                    accentColor = planAccent,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    onClick = { selectedPeriodIndex = 1 }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ═════════════════════════════════════════════════════════
            // 5. CTA BUTTON
            // ═════════════════════════════════════════════════════════
            Button(
                onClick = {
                    if (hasBillingProducts && selectedProduct != null) {
                        val offerToken = selectedProduct.subscriptionOfferDetails
                            ?.firstOrNull()?.offerToken ?: return@Button
                        billingManager.launchPurchaseFlow(activity, selectedProduct, offerToken)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                enabled = hasBillingProducts,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ThemeTokens.Accent,
                    contentColor = ThemeTokens.Background,
                    disabledContainerColor = ThemeTokens.Accent.copy(alpha = 0.35f),
                    disabledContentColor = ThemeTokens.Background.copy(alpha = 0.5f)
                )
            ) {
                Text(
                    text = if (hasBillingProducts) "Subscribe Now" else "Coming Soon",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            if (!hasBillingProducts) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(12.dp),
                        color = ThemeTokens.Accent,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Billing is connecting… prices shown are estimates.",
                        fontSize = 11.sp,
                        color = ThemeTokens.TextSub,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ═════════════════════════════════════════════════════════
            // 6. FOOTER
            // ═════════════════════════════════════════════════════════
            // Restore purchases
            TextButton(onClick = {
                // Re-query existing purchases to restore state
                billingManager.connect()
            }) {
                Text(
                    "Restore Purchase",
                    color = ThemeTokens.TextSub,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    textDecoration = TextDecoration.Underline
                )
            }

            // Terms + Privacy
            Row(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Terms of Service",
                    color = ThemeTokens.TextSub.copy(alpha = 0.7f),
                    fontSize = 11.sp,
                    textDecoration = TextDecoration.Underline
                )
                Text(
                    "  •  ",
                    color = ThemeTokens.TextSub.copy(alpha = 0.5f),
                    fontSize = 11.sp
                )
                Text(
                    "Privacy Policy",
                    color = ThemeTokens.TextSub.copy(alpha = 0.7f),
                    fontSize = 11.sp,
                    textDecoration = TextDecoration.Underline
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider(color = ThemeTokens.Border)
            Spacer(modifier = Modifier.height(16.dp))

            // ═════════════════════════════════════════════════════════
            // 7. REWARDED AD OPTION
            // ═════════════════════════════════════════════════════════
            TextButton(
                onClick = {
                    AdManager.showRewarded(activity) {
                        AdManager.grantTemporaryAdFree()
                        onDismiss()
                    }
                }
            ) {
                Text(
                    text = "🎬  Or watch an ad for 1 hour ad-free",
                    color = ThemeTokens.Accent,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Helper: Feature comparison row
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun FeatureRow(
    feature: String,
    free: String,
    pro: String,
    proPlus: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            feature,
            color = ThemeTokens.TextMain,
            fontSize = 13.sp,
            modifier = Modifier.weight(1.4f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        CellText(free, ThemeTokens.TextSub, Modifier.weight(0.8f))
        CellText(pro, ThemeTokens.Primary, Modifier.weight(0.8f))
        CellText(proPlus, ThemeTokens.Accent, Modifier.weight(0.8f))
    }
}

@Composable
private fun CellText(value: String, color: Color, modifier: Modifier = Modifier) {
    Text(
        text = value,
        color = if (value == "—") ThemeTokens.TextSub.copy(alpha = 0.4f) else color,
        fontSize = 12.sp,
        fontWeight = if (value == "✓") FontWeight.Bold else FontWeight.Normal,
        textAlign = TextAlign.Center,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Helper: Plan tab selector (Pro / Pro+)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun PlanTab(
    label: String,
    isSelected: Boolean,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) accentColor.copy(alpha = 0.15f) else Color.Transparent,
        animationSpec = tween(200),
        label = "planTabBg"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) accentColor else Color.Transparent,
        animationSpec = tween(200),
        label = "planTabBorder"
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor, RoundedCornerShape(10.dp))
            .border(1.5.dp, borderColor, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (isSelected) accentColor else ThemeTokens.TextSub,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            fontSize = 15.sp
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Helper: Subscription pricing card
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SubscriptionCard(
    label: String,
    price: String,
    period: String,
    savingsBadge: String?,
    isSelected: Boolean,
    gradient: Brush,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) accentColor else ThemeTokens.Border,
        animationSpec = tween(200),
        label = "subCardBorder"
    )
    val containerColor by animateColorAsState(
        targetValue = if (isSelected) accentColor.copy(alpha = 0.08f) else ThemeTokens.CardBg,
        animationSpec = tween(200),
        label = "subCardBg"
    )

    Card(
        modifier = modifier
            .border(2.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Savings badge
            if (savingsBadge != null) {
                Text(
                    text = savingsBadge,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = ThemeTokens.Background,
                    modifier = Modifier
                        .background(
                            brush = gradient,
                            shape = RoundedCornerShape(6.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
            } else {
                // Spacer to align cards when one has a badge
                Spacer(modifier = Modifier.height(19.dp))
            }

            Text(
                text = label,
                color = ThemeTokens.TextSub,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = price,
                color = ThemeTokens.TextMain,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = period,
                color = ThemeTokens.TextSub,
                fontSize = 12.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Selection indicator
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .border(
                        width = 2.dp,
                        color = if (isSelected) accentColor else ThemeTokens.Border,
                        shape = CircleShape
                    )
                    .padding(3.dp)
                    .then(
                        if (isSelected) Modifier.background(accentColor, CircleShape)
                        else Modifier
                    )
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Helper: Extract formatted price from ProductDetails
// ─────────────────────────────────────────────────────────────────────────────

private fun ProductDetails.formattedPrice(): String? {
    return subscriptionOfferDetails
        ?.firstOrNull()
        ?.pricingPhases
        ?.pricingPhaseList
        ?.firstOrNull()
        ?.formattedPrice
}
