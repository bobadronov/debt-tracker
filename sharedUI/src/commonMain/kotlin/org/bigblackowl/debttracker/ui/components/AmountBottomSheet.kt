package org.bigblackowl.debttracker.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Devices.DESKTOP
import androidx.compose.ui.tooling.preview.Preview
import com.ionspin.kotlin.bignum.decimal.BigDecimal
import org.bigblackowl.debttracker.core.i18n.LocalStrings
import org.bigblackowl.debttracker.core.settings.AppSettings
import org.bigblackowl.debttracker.core.sound.SoundEffect
import org.bigblackowl.debttracker.core.sound.SoundPlayer
import org.bigblackowl.debttracker.domain.model.Currency
import org.bigblackowl.debttracker.domain.model.PaymentMethod
import org.bigblackowl.debttracker.domain.validation.sanitizeAmountInput
import org.bigblackowl.debttracker.preview.DebtTrackerPreview
import org.bigblackowl.debttracker.theme.Dimens
import org.koin.compose.koinInject


/**
 * Спільний bottom sheet для введення суми/способу оплати — використовується
 * для "Повернути"/"Ще позичив" (Debtor) і "Повернути борг"/"Ще взяв у борг" (Creditor).
 * [currency] — валюта батьківського боржника/кредитора (транзакція успадковує її, без вибору тут).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AmountBottomSheet(
    title: String,
    prefillAmount: String = "",
    currency: Currency = Currency.UAH,
    onDismiss: () -> Unit,
    onConfirm: (amount: BigDecimal, method: PaymentMethod, cardLastDigits: String?) -> Unit,
) {
    var amountText by remember { mutableStateOf(prefillAmount) }
    var method by remember { mutableStateOf(PaymentMethod.CASH) }
    var cardLastDigits by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val sheetState = rememberModalBottomSheetState()
    val appSettings = koinInject<AppSettings>()
    val soundPlayer = koinInject<SoundPlayer>()
    val haptics = LocalHapticFeedback.current
    val strings = LocalStrings.current
    val clipboardText by rememberClipboardText()

    LaunchedEffect(Unit) {
        if (appSettings.soundEnabled) soundPlayer.play(SoundEffect.DIALOG_OPEN)
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(Dimens.space16),
            verticalArrangement = Arrangement.spacedBy(Dimens.space12),
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = sanitizeAmountInput(it); error = null },
                label = { Text("${strings.amount} (${currency.symbol})") },
                isError = error != null,
                supportingText = { error?.let { Text(it) } },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
            ClipboardPasteHint(
                clipboardText = clipboardText,
                fieldValue = amountText,
                isRelevant = { text ->
                    val sanitized = sanitizeAmountInput(text)
                    sanitized.isNotBlank() &&
                        runCatching { BigDecimal.parseString(sanitized) }.getOrNull()
                            ?.let { it > BigDecimal.ZERO } == true
                },
                onPaste = { amountText = sanitizeAmountInput(it); error = null },
            )
            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.space8)) {
                FilterChip(
                    selected = method == PaymentMethod.CASH,
                    onClick = { method = PaymentMethod.CASH },
                    label = { Text(strings.cash) },
                )
                FilterChip(
                    selected = method == PaymentMethod.CARD,
                    onClick = { method = PaymentMethod.CARD },
                    label = { Text(strings.card) },
                )
            }
            if (method == PaymentMethod.CARD) {
                OutlinedTextField(
                    value = cardLastDigits,
                    onValueChange = { cardLastDigits = it },
                    label = { Text(strings.cardLastDigits) },
                    modifier = Modifier.fillMaxWidth(),
                )
                ClipboardPasteHint(
                    clipboardText = clipboardText,
                    fieldValue = cardLastDigits,
                    isRelevant = { it.filter(Char::isDigit).length in 3..6 },
                    onPaste = { cardLastDigits = it },
                )
            }
            Button(
                onClick = {
                    val parsed = runCatching { BigDecimal.parseString(amountText.trim()) }.getOrNull()
                    if (parsed == null || parsed <= BigDecimal.ZERO) {
                        error = strings.amountError
                        return@Button
                    }
                    if (appSettings.hapticEnabled) haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                    onConfirm(parsed, method, cardLastDigits.trim().ifBlank { null })
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(strings.confirm) }
        }
    }
}

@Preview
@Composable
private fun AmountBottomSheetLightPhonePreview() = DebtTrackerPreview(darkTheme = false) {
    AmountBottomSheet(
        title = "Repay debt",
        currency = Currency.UAH,
        onDismiss = {},
        onConfirm = { _, _, _ -> },
    )
}

@Preview
@Composable
private fun AmountBottomSheetDarkPhonePreview() = DebtTrackerPreview(darkTheme = true) {
    AmountBottomSheet(
        title = "Repay debt",
        currency = Currency.UAH,
        onDismiss = {},
        onConfirm = { _, _, _ -> },
    )
}

@Preview(device = DESKTOP)
@Composable
private fun AmountBottomSheetLightDesktopPreview() = DebtTrackerPreview(darkTheme = false) {
    AmountBottomSheet(
        title = "Repay debt",
        currency = Currency.UAH,
        onDismiss = {},
        onConfirm = { _, _, _ -> },
    )
}

@Preview(device = DESKTOP)
@Composable
private fun AmountBottomSheetDarkDesktopPreview() = DebtTrackerPreview(darkTheme = true) {
    AmountBottomSheet(
        title = "Repay debt",
        currency = Currency.UAH,
        onDismiss = {},
        onConfirm = { _, _, _ -> },
    )
}
