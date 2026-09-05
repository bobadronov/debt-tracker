package org.bigblackowl.debttracker.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Devices.DESKTOP
import androidx.compose.ui.tooling.preview.Preview
import com.ionspin.kotlin.bignum.decimal.BigDecimal
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import org.bigblackowl.debttracker.core.i18n.LocalStrings
import org.bigblackowl.debttracker.domain.model.Currency
import org.bigblackowl.debttracker.domain.model.PaymentMethod
import org.bigblackowl.debttracker.domain.model.formatDueDateTime
import org.bigblackowl.debttracker.domain.validation.sanitizeAmountInput
import org.bigblackowl.debttracker.preview.DebtTrackerPreview
import org.bigblackowl.debttracker.theme.Dimens
import kotlin.time.Clock

/**
 * Bottom sheet for editing one existing transaction from a debtor/creditor history: amount
 * (magnitude only — the lend/repay direction is kept), payment method, optional comment and the
 * date+time. [onConfirm] hands back the edited fields; the caller re-applies the direction and
 * persists. Mirrors [AmountBottomSheet]'s look for the add flow.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionEditSheet(
    initialAmount: BigDecimal,
    initialMethod: PaymentMethod,
    initialComment: String?,
    initialDate: kotlin.time.Instant,
    currency: Currency,
    onDismiss: () -> Unit,
    onConfirm: (amount: BigDecimal, method: PaymentMethod, comment: String?, date: kotlin.time.Instant) -> Unit,
) {
    val strings = LocalStrings.current
    val sheetState = rememberModalBottomSheetState()
    val clipboardText by rememberClipboardText()

    // Show the magnitude only — direction stays whatever the original row was.
    var amountText by remember { mutableStateOf(initialAmount.abs().toStringExpanded()) }
    var method by remember { mutableStateOf(initialMethod) }
    var comment by remember { mutableStateOf(initialComment.orEmpty()) }
    var date by remember { mutableStateOf(initialDate) }
    var error by remember { mutableStateOf<String?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var pendingDateMillis by remember { mutableStateOf<Long?>(null) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier.fillMaxWidth().imePadding().padding(Dimens.space16),
            verticalArrangement = Arrangement.spacedBy(Dimens.space12),
        ) {
            Text(strings.transactionEdit.editTitle, style = MaterialTheme.typography.titleMedium)

            PasteableOutlinedTextField(
                value = amountText,
                onValueChange = { amountText = sanitizeAmountInput(it); error = null },
                label = "${strings.amount} (${currency.symbol})",
                clipboardText = clipboardText,
                isPasteRelevant = { text ->
                    val sanitized = sanitizeAmountInput(text)
                    sanitized.isNotBlank() &&
                        runCatching { BigDecimal.parseString(sanitized) }.getOrNull()
                            ?.let { it > BigDecimal.ZERO } == true
                },
                isError = error != null,
                supportingText = error,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            ) { amountText = sanitizeAmountInput(it); error = null }

            PaymentMethodChipRow(selected = method, onSelect = { method = it })

            PasteableOutlinedTextField(
                value = comment,
                onValueChange = { comment = it },
                label = strings.comment,
                clipboardText = clipboardText,
                isPasteRelevant = { it.isNotBlank() },
            )

            OutlinedCard(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(Dimens.space16),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(strings.transactionEdit.dateLabel, style = MaterialTheme.typography.labelMedium)
                        Text(date.formatDueDateTime(), style = MaterialTheme.typography.bodyLarge)
                    }
                    Icon(Icons.Filled.EditCalendar, contentDescription = strings.transactionEdit.dateLabel)
                }
            }

            Button(
                onClick = {
                    val parsed = runCatching { BigDecimal.parseString(amountText.trim()) }.getOrNull()
                    if (parsed == null || parsed <= BigDecimal.ZERO) {
                        error = strings.amountError
                        return@Button
                    }
                    onConfirm(parsed, method, comment.trim().ifBlank { null }, date)
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(strings.save) }
        }
    }

    if (showDatePicker) {
        val dpState = rememberDatePickerState(initialSelectedDateMillis = date.toEpochMilliseconds())
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pendingDateMillis = dpState.selectedDateMillis
                    showDatePicker = false
                    if (pendingDateMillis != null) showTimePicker = true
                }) { Text(strings.confirm) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text(strings.cancel) }
            },
        ) {
            DatePicker(state = dpState)
        }
    }

    if (showTimePicker) {
        val existing = date.toLocalDateTime(TimeZone.currentSystemDefault())
        val tpState = rememberTimePickerState(
            initialHour = existing.hour,
            initialMinute = existing.minute,
            is24Hour = true,
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val dateMillis = pendingDateMillis
                    showTimePicker = false
                    if (dateMillis != null) {
                        // DatePicker keeps the picked calendar day as UTC-midnight millis (M3 default).
                        val pickedDate = kotlin.time.Instant.fromEpochMilliseconds(dateMillis)
                            .toLocalDateTime(TimeZone.UTC).date
                        val local = LocalDateTime(pickedDate, LocalTime(tpState.hour, tpState.minute))
                        date = local.toInstant(TimeZone.currentSystemDefault())
                    }
                }) { Text(strings.confirm) }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text(strings.cancel) }
            },
            text = { TimePicker(state = tpState) },
        )
    }
}

@Preview
@Composable
private fun TransactionEditSheetLightPhonePreview() = DebtTrackerPreview(darkTheme = false) {
    TransactionEditSheet(
        initialAmount = BigDecimal.parseString("-1200"),
        initialMethod = PaymentMethod.CARD,
        initialComment = "Repaid half",
        initialDate = Clock.System.now(),
        currency = Currency.UAH,
        onDismiss = {},
        onConfirm = { _, _, _, _ -> },
    )
}

@Preview(device = DESKTOP)
@Composable
private fun TransactionEditSheetDarkDesktopPreview() = DebtTrackerPreview(darkTheme = true) {
    TransactionEditSheet(
        initialAmount = BigDecimal.parseString("500"),
        initialMethod = PaymentMethod.CASH,
        initialComment = null,
        initialDate = Clock.System.now(),
        currency = Currency.UAH,
        onDismiss = {},
        onConfirm = { _, _, _, _ -> },
    )
}
