package com.teleport.app.mobile

import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.teleport.app.billing.BillingManager
import com.teleport.app.ui.theme.ThemeTokens

// ──────────────────────────────────────────────────────────────────────────────
// TipJarSheet – bottom-sheet-style tip jar for one-time supporter purchases
// ──────────────────────────────────────────────────────────────────────────────

private data class TipOption(
    val emoji: String,
    val label: String,
    val price: String,
    val productId: String,
    val gradientColors: List<Color>
)

@Composable
fun TipJarSheet(
    billingManager: BillingManager,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    val tipOptions = remember {
        listOf(
            TipOption(
                emoji = "☕",
                label = "Coffee",
                price = "$1.99",
                productId = BillingManager.PRODUCT_TIP_SMALL,
                gradientColors = listOf(
                    Color(0xFF1A1A2E),
                    Color(0xFF16213E)
                )
            ),
            TipOption(
                emoji = "🍕",
                label = "Pizza",
                price = "$4.99",
                productId = BillingManager.PRODUCT_TIP_MEDIUM,
                gradientColors = listOf(
                    Color(0xFF1A1A2E),
                    Color(0xFF1B1340)
                )
            ),
            TipOption(
                emoji = "🎉",
                label = "Party",
                price = "$9.99",
                productId = BillingManager.PRODUCT_TIP_LARGE,
                gradientColors = listOf(
                    Color(0xFF1A1A2E),
                    Color(0xFF0D2137)
                )
            )
        )
    }

    var selectedIndex by remember { mutableIntStateOf(-1) }
    var showThankYou by remember { mutableStateOf(false) }

    // ── Sheet container ──
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .background(ThemeTokens.Background)
            .padding(horizontal = 24.dp)
            .padding(top = 20.dp, bottom = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ── Drag handle indicator ──
        Box(
            modifier = Modifier
                .width(40.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(ThemeTokens.TextSub.copy(alpha = 0.3f))
        )

        Spacer(modifier = Modifier.height(20.dp))

        if (showThankYou) {
            // ═══════════════════════════════════════════════════════
            // THANK-YOU STATE
            // ═══════════════════════════════════════════════════════
            ThankYouContent(onDismiss = onDismiss)
        } else {
            // ═══════════════════════════════════════════════════════
            // 1. HEADER
            // ═══════════════════════════════════════════════════════
            Text(
                text = "☕ Support TelePort",
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = ThemeTokens.TextMain
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Your tips help keep TelePort free and ad-light",
                fontSize = 14.sp,
                color = ThemeTokens.TextSub,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // ═══════════════════════════════════════════════════════
            // 2. TIP CARDS ROW
            // ═══════════════════════════════════════════════════════
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                tipOptions.forEachIndexed { index, option ->
                    TipCard(
                        option = option,
                        isSelected = selectedIndex == index,
                        onClick = { selectedIndex = index },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // ═══════════════════════════════════════════════════════
            // 3. CTA BUTTON
            // ═══════════════════════════════════════════════════════
            Button(
                onClick = {
                    // Billing products aren't set up in Play Console yet.
                    // For now, show a placeholder toast.
                    Toast.makeText(
                        context,
                        "Tip jar coming soon!",
                        Toast.LENGTH_SHORT
                    ).show()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(16.dp),
                enabled = selectedIndex >= 0,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ThemeTokens.Accent,
                    contentColor = ThemeTokens.Background,
                    disabledContainerColor = ThemeTokens.Accent.copy(alpha = 0.30f),
                    disabledContentColor = ThemeTokens.Background.copy(alpha = 0.4f)
                )
            ) {
                val buttonLabel = if (selectedIndex >= 0) {
                    "Send ${tipOptions[selectedIndex].price} Tip"
                } else {
                    "Send Tip"
                }
                Text(
                    text = buttonLabel,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ═══════════════════════════════════════════════════════
            // 4. DISMISS
            // ═══════════════════════════════════════════════════════
            TextButton(onClick = onDismiss) {
                Text(
                    text = "Maybe Later",
                    color = ThemeTokens.TextSub,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Helper: Individual tip option card
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun TipCard(
    option: TipOption,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) ThemeTokens.Accent else ThemeTokens.Border,
        animationSpec = tween(250),
        label = "tipBorder"
    )
    val borderWidth by animateDpAsState(
        targetValue = if (isSelected) 2.dp else 1.dp,
        animationSpec = tween(250),
        label = "tipBorderWidth"
    )
    val bgAlpha by animateColorAsState(
        targetValue = if (isSelected) ThemeTokens.Accent.copy(alpha = 0.08f) else Color.Transparent,
        animationSpec = tween(250),
        label = "tipBgOverlay"
    )

    val cardGradient = Brush.verticalGradient(option.gradientColors)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(cardGradient, RoundedCornerShape(16.dp))
            .background(bgAlpha, RoundedCornerShape(16.dp))
            .border(borderWidth, borderColor, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 20.dp, horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Emoji icon
            Text(
                text = option.emoji,
                fontSize = 32.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Label
            Text(
                text = option.label,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isSelected) ThemeTokens.TextMain else ThemeTokens.TextSub
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Price
            Text(
                text = option.price,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (isSelected) ThemeTokens.Accent else ThemeTokens.TextMain
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Selection dot
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .then(
                        if (isSelected)
                            Modifier.background(ThemeTokens.Accent, RoundedCornerShape(6.dp))
                        else
                            Modifier.border(
                                1.5.dp,
                                ThemeTokens.TextSub.copy(alpha = 0.4f),
                                RoundedCornerShape(6.dp)
                            )
                    )
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Helper: Thank-you confirmation after successful purchase
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ThankYouContent(onDismiss: () -> Unit) {
    Spacer(modifier = Modifier.height(16.dp))

    Text(
        text = "🎉",
        fontSize = 56.sp
    )

    Spacer(modifier = Modifier.height(12.dp))

    Text(
        text = "Thank You!",
        fontSize = 26.sp,
        fontWeight = FontWeight.ExtraBold,
        color = ThemeTokens.TextMain
    )

    Spacer(modifier = Modifier.height(8.dp))

    Text(
        text = "Your support means the world.\nTelePort will keep getting better because of you!",
        fontSize = 14.sp,
        color = ThemeTokens.TextSub,
        textAlign = TextAlign.Center,
        lineHeight = 20.sp
    )

    Spacer(modifier = Modifier.height(28.dp))

    Button(
        onClick = onDismiss,
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = ThemeTokens.Accent,
            contentColor = ThemeTokens.Background
        )
    ) {
        Text(
            text = "Awesome!",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
    }

    Spacer(modifier = Modifier.height(8.dp))
}
