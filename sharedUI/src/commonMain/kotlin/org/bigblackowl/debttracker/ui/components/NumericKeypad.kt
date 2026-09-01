package org.bigblackowl.debttracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Devices.DESKTOP
import androidx.compose.ui.tooling.preview.Preview
import org.bigblackowl.debttracker.core.i18n.LocalStrings
import org.bigblackowl.debttracker.preview.DebtTrackerPreview
import org.bigblackowl.debttracker.theme.Dimens

/**
 * On-screen 3×4 numeric pad for PIN entry — the same control on every platform, so the unlock
 * screen never depends on the system IME (which was part of why PIN entry felt inconsistent).
 * Desktop still accepts the physical keyboard in parallel (see [org.bigblackowl.debttracker.ui.screens.authgate.AuthGateScreen]).
 */
@Composable
fun NumericKeypad(
    onDigit: (Char) -> Unit,
    onBackspace: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val strings = LocalStrings.current
    // null — an empty slot to keep the "0" centered under "8".
    val layout = listOf(
        listOf('1', '2', '3'),
        listOf('4', '5', '6'),
        listOf('7', '8', '9'),
        listOf(null, '0', '\b'),
    )
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(Dimens.space12),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        layout.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.space16)) {
                row.forEach { key ->
                    when (key) {
                        null -> Box(Modifier.size(Dimens.space72))
                        '\b' -> KeypadKey(
                            onClick = onBackspace,
                            enabled = enabled,
                            contentDescription = strings.authGateBackspace,
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Backspace, contentDescription = null)
                        }
                        else -> KeypadKey(
                            onClick = { onDigit(key) },
                            enabled = enabled,
                            contentDescription = key.toString(),
                        ) {
                            Text(key.toString(), style = MaterialTheme.typography.headlineSmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KeypadKey(
    onClick: () -> Unit,
    enabled: Boolean,
    contentDescription: String,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(Dimens.space72)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(enabled = enabled, onClickLabel = contentDescription, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

@Preview
@Composable
private fun NumericKeypadLightPhonePreview() = DebtTrackerPreview(darkTheme = false) {
    NumericKeypad(onDigit = {}, onBackspace = {})
}

@Preview(device = DESKTOP)
@Composable
private fun NumericKeypadDarkDesktopPreview() = DebtTrackerPreview(darkTheme = true) {
    NumericKeypad(onDigit = {}, onBackspace = {})
}
