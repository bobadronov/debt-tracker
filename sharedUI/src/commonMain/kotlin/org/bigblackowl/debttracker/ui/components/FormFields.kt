package org.bigblackowl.debttracker.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Money
import androidx.compose.material3.Button
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Devices.DESKTOP
import androidx.compose.ui.tooling.preview.Preview
import org.bigblackowl.debttracker.core.i18n.LocalStrings
import org.bigblackowl.debttracker.domain.model.Currency
import org.bigblackowl.debttracker.domain.model.PaymentMethod
import org.bigblackowl.debttracker.preview.DebtTrackerPreview
import org.bigblackowl.debttracker.theme.Dimens

/**
 * [OutlinedTextField] + [ClipboardPasteHint] wired together — this pairing (track focus,
 * show a dismissible "paste from clipboard" suggestion while the field is empty and focused)
 * was copy-pasted across every form screen (Add/Edit debtor/creditor, account edit, auth,
 * amount sheet); consolidated here so the paste behavior only needs fixing in one place.
 */
@Composable
fun PasteableOutlinedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    clipboardText: String?,
    isPasteRelevant: (String) -> Boolean,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    isError: Boolean = false,
    supportingText: String? = null,
    singleLine: Boolean = true,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    readOnly: Boolean = false,
    enabled: Boolean = true,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    onPaste: (String) -> Unit = onValueChange,
) {
    var focused by remember { mutableStateOf(false) }
    Column(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            placeholder = placeholder?.let { { Text(it) } },
            isError = isError,
            supportingText = supportingText?.let { { Text(it) } },
            singleLine = singleLine,
            maxLines = maxLines,
            readOnly = readOnly,
            enabled = enabled,
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            visualTransformation = visualTransformation,
            modifier = Modifier.fillMaxWidth().onFocusChanged { focused = it.isFocused },
        )
        ClipboardPasteHint(
            clipboardText = clipboardText,
            fieldValue = value,
            isFieldFocused = focused,
            isRelevant = isPasteRelevant,
            onPaste = onPaste,
        )
    }
}

/** Read-only currency picker used by the initial-amount row on the Add/Edit debtor/creditor forms. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrencyDropdownField(
    selected: Currency,
    onSelect: (Currency) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = selected.symbol,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            Currency.entries.forEach { currency ->
                DropdownMenuItem(
                    text = { Text(currency.symbol) },
                    onClick = {
                        onSelect(currency)
                        expanded = false
                    },
                )
            }
        }
    }
}

/** Cash/Card [FilterChip] pair shared by the Add/Edit forms and [AmountBottomSheet]. */
@Composable
fun PaymentMethodChipRow(
    selected: PaymentMethod,
    onSelect: (PaymentMethod) -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStrings.current
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(
            Dimens.space8,
            alignment = Alignment.CenterHorizontally
        ),
    ) {
        val chipModifier = Modifier.weight(1f).defaultMinSize(minHeight = OutlinedTextFieldDefaults.MinHeight)
        FilterChip(
            selected = selected == PaymentMethod.CASH,
            onClick = { onSelect(PaymentMethod.CASH) },
            label = { Text(strings.cash) },
            modifier = chipModifier,
            trailingIcon = { Icon(Icons.Default.Money, null) },
        )
        FilterChip(
            selected = selected == PaymentMethod.CARD,
            onClick = { onSelect(PaymentMethod.CARD) },
            label = { Text(strings.card) },
            modifier = chipModifier,
            trailingIcon = { Icon(Icons.Default.CreditCard, null) },
        )
    }
}

/** [Button] that swaps its content for a spinner while [isLoading], used by every form's submit action. */
@Composable
fun LoadingButton(
    onClick: () -> Unit,
    isLoading: Boolean,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: (@Composable () -> Unit)? = null,
    label: @Composable () -> Unit,
) {
    Button(
        onClick = onClick,
        enabled = enabled && !isLoading,
        modifier = modifier,
    ) {
        if (isLoading) {
            CircularWavyProgressIndicator(modifier = Modifier.padding(end = Dimens.space8))
        } else {
            leadingIcon?.invoke()
            label()
        }
    }
}

@Composable
private fun FormFieldsSample() {
    var text by remember { mutableStateOf("") }
    var currency by remember { mutableStateOf(Currency.UAH) }
    var method by remember { mutableStateOf(PaymentMethod.CASH) }
    var saving by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.padding(Dimens.space16),
        verticalArrangement = Arrangement.spacedBy(Dimens.space12),
    ) {
        PasteableOutlinedTextField(
            value = text,
            onValueChange = { text = it },
            label = "Full name",
            clipboardText = null,
            isPasteRelevant = { it.isNotBlank() },
        )
        CurrencyDropdownField(selected = currency, onSelect = { currency = it }, label = "Currency")
        PaymentMethodChipRow(selected = method, onSelect = { method = it })
        LoadingButton(onClick = { saving = !saving }, isLoading = saving, label = { Text("Save") })
    }
}

@Preview
@Composable
private fun FormFieldsLightPhonePreview() =
    DebtTrackerPreview(darkTheme = false) { FormFieldsSample() }

@Preview
@Composable
private fun FormFieldsDarkPhonePreview() =
    DebtTrackerPreview(darkTheme = true) { FormFieldsSample() }

@Preview(device = DESKTOP)
@Composable
private fun FormFieldsLightDesktopPreview() =
    DebtTrackerPreview(darkTheme = false) { FormFieldsSample() }

@Preview(device = DESKTOP)
@Composable
private fun FormFieldsDarkDesktopPreview() =
    DebtTrackerPreview(darkTheme = true) { FormFieldsSample() }
