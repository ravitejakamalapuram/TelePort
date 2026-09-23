package com.teleport.app.tv

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.carfry369.teleport.R
import com.teleport.app.ui.theme.ThemeTokens

/**
 * Prominent in-app disclosure required by the Google Play Accessibility API policy.
 *
 * Must be shown (and explicitly agreed to) before the user is sent to the system
 * Accessibility settings to enable [com.teleport.app.tv.server.TelePortAccessibilityService].
 * [onAgree] is the only path that should open Accessibility settings; [onDecline]
 * (including back / outside dismissal) must not enable anything.
 */
@Composable
fun AccessibilityDisclosureDialog(
    onAgree: () -> Unit,
    onDecline: () -> Unit
) {
    // Focus "Decline" first on TV so a stray D-pad centre press never counts as consent.
    val declineFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        runCatching { declineFocus.requestFocus() }
    }

    AlertDialog(
        onDismissRequest = onDecline,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        ),
        modifier = Modifier.widthIn(max = 640.dp),
        containerColor = ThemeTokens.CardBg,
        shape = RoundedCornerShape(16.dp),
        title = {
            Text(
                text = stringResource(R.string.accessibility_disclosure_title),
                color = ThemeTokens.TextMain,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                DisclosureText(stringResource(R.string.accessibility_disclosure_intro))
                Spacer(Modifier.height(4.dp))
                DisclosureText(
                    stringResource(R.string.accessibility_disclosure_access_header),
                    bold = true
                )
                DisclosureText(stringResource(R.string.accessibility_disclosure_access_1))
                DisclosureText(stringResource(R.string.accessibility_disclosure_access_2))
                DisclosureText(stringResource(R.string.accessibility_disclosure_access_3))
                Spacer(Modifier.height(4.dp))
                DisclosureText(stringResource(R.string.accessibility_disclosure_data), bold = true)
                DisclosureText(stringResource(R.string.accessibility_disclosure_revoke))
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDecline,
                border = BorderStroke(1.dp, ThemeTokens.Border),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = ThemeTokens.TextMain),
                modifier = Modifier.focusRequester(declineFocus)
            ) {
                Text(stringResource(R.string.accessibility_disclosure_decline))
            }
        },
        confirmButton = {
            Button(
                onClick = onAgree,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ThemeTokens.Accent,
                    contentColor = ThemeTokens.Background
                )
            ) {
                Text(
                    text = stringResource(R.string.accessibility_disclosure_agree),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    )
}

@Composable
private fun DisclosureText(text: String, bold: Boolean = false) {
    Text(
        text = text,
        color = if (bold) ThemeTokens.TextMain else ThemeTokens.TextSub,
        fontSize = 14.sp,
        fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Normal
    )
}
