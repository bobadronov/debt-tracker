package org.bigblackowl.debttracker.ui.screens.creditors

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Devices.DESKTOP
import androidx.compose.ui.tooling.preview.Preview
import com.ionspin.kotlin.bignum.decimal.BigDecimal
import org.bigblackowl.debttracker.core.i18n.LocalStrings
import org.bigblackowl.debttracker.domain.model.Currency
import org.bigblackowl.debttracker.domain.model.PaymentMethod
import org.bigblackowl.debttracker.domain.validation.isValidEmail
import org.bigblackowl.debttracker.domain.validation.isValidFullName
import org.bigblackowl.debttracker.domain.validation.sanitizeAmountInput
import org.bigblackowl.debttracker.preview.DebtTrackerPreview
import org.bigblackowl.debttracker.theme.Dimens
import org.bigblackowl.debttracker.ui.components.BackButton
import org.bigblackowl.debttracker.ui.components.ClipboardPasteHint
import org.bigblackowl.debttracker.ui.components.ProfileSuggestionCard
import org.bigblackowl.debttracker.ui.components.UkrainianPhoneVisualTransformation
import org.bigblackowl.debttracker.ui.components.rememberClipboardText
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/** Create/edit form for a [org.bigblackowl.debttracker.domain.model.Creditor] — `creditorId == null` means "new". */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditCreditorScreen(
    creditorId: String?,
    onDone: () -> Unit,
) {
    val viewModel: AddEditCreditorViewModel = koinViewModel { parametersOf(creditorId) }
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val strings = LocalStrings.current
    var currencyMenuExpanded by remember { mutableStateOf(false) }
    val clipboardText by rememberClipboardText()

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                AddEditCreditorEffect.Saved -> onDone()
                is AddEditCreditorEffect.Error -> snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.isEditing) strings.addEditCreditorTitleEdit else strings.addEditCreditorTitleNew) },
                navigationIcon = { BackButton(onClick = onDone) },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .padding(Dimens.space16)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier.width(Dimens.contentMaxWidth).fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(Dimens.space12),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                var fullNameFocused by remember { mutableStateOf(false) }
                OutlinedTextField(
                    value = state.fullName,
                    onValueChange = { viewModel.onIntent(AddEditCreditorIntent.FullNameChanged(it)) },
                    label = { Text(strings.fullName) },
                    placeholder = { Text(strings.fullNamePlaceholder) },
                    isError = state.fullNameError != null,
                    supportingText = { state.fullNameError?.let { Text(it) } },
                    modifier = Modifier.fillMaxWidth().onFocusChanged { fullNameFocused = it.isFocused },
                    singleLine = true,
                )
                ClipboardPasteHint(
                    clipboardText = clipboardText,
                    fieldValue = state.fullName,
                    isFieldFocused = fullNameFocused,
                    isRelevant = ::isValidFullName,
                    onPaste = { viewModel.onIntent(AddEditCreditorIntent.FullNameChanged(it)) },
                )
                var phoneFocused by remember { mutableStateOf(false) }
                OutlinedTextField(
                    value = state.phone,
                    onValueChange = {
                        viewModel.onIntent(AddEditCreditorIntent.PhoneChanged(it.filter { c -> c.isDigit() }.take(10)))
                    },
                    label = { Text(strings.phone) },
                    modifier = Modifier.fillMaxWidth().onFocusChanged { phoneFocused = it.isFocused }.semantics { contentType = ContentType.PhoneNumber },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    visualTransformation = remember { UkrainianPhoneVisualTransformation() },
                )
                ClipboardPasteHint(
                    clipboardText = clipboardText,
                    fieldValue = state.phone,
                    isFieldFocused = phoneFocused,
                    isRelevant = { it.filter(Char::isDigit).length >= 9 },
                    onPaste = {
                        viewModel.onIntent(AddEditCreditorIntent.PhoneChanged(it.filter(Char::isDigit).take(10)))
                    },
                )
                var emailFocused by remember { mutableStateOf(false) }
                OutlinedTextField(
                    value = state.email,
                    onValueChange = { viewModel.onIntent(AddEditCreditorIntent.EmailChanged(it)) },
                    label = { Text(strings.email) },
                    modifier = Modifier.fillMaxWidth().onFocusChanged { emailFocused = it.isFocused }.semantics { contentType = ContentType.EmailAddress },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                )
                ClipboardPasteHint(
                    clipboardText = clipboardText,
                    fieldValue = state.email,
                    isFieldFocused = emailFocused,
                    isRelevant = ::isValidEmail,
                    onPaste = { viewModel.onIntent(AddEditCreditorIntent.EmailChanged(it)) },
                )
                state.profileSuggestion?.let { suggestion ->
                    ProfileSuggestionCard(
                        suggestion = suggestion,
                        onUse = { viewModel.onIntent(AddEditCreditorIntent.ApplyProfileSuggestion) },
                        onDismiss = { viewModel.onIntent(AddEditCreditorIntent.DismissProfileSuggestion) },
                    )
                }
                var commentFocused by remember { mutableStateOf(false) }
                OutlinedTextField(
                    value = state.comment,
                    onValueChange = { viewModel.onIntent(AddEditCreditorIntent.CommentChanged(it)) },
                    label = { Text(strings.comment) },
                    modifier = Modifier.fillMaxWidth().onFocusChanged { commentFocused = it.isFocused },
                    maxLines = 4,
                )
                ClipboardPasteHint(
                    clipboardText = clipboardText,
                    fieldValue = state.comment,
                    isFieldFocused = commentFocused,
                    isRelevant = { it.trim().length in 1..500 },
                    onPaste = { viewModel.onIntent(AddEditCreditorIntent.CommentChanged(it)) },
                )

                if (!state.isEditing) {
                    var initialAmountFocused by remember { mutableStateOf(false) }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Dimens.space5)
                    ) {
                        OutlinedTextField(
                            value = state.initialAmountText,
                            onValueChange = {
                                viewModel.onIntent(
                                    AddEditCreditorIntent.InitialAmountChanged(
                                        sanitizeAmountInput(it)
                                    )
                                )
                            },
                            label = { Text(strings.addEditCreditorInitialAmount) },
                            isError = state.amountError != null,
                            supportingText = { state.amountError?.let { Text(it) } },
                            modifier = Modifier.weight(1f).onFocusChanged { initialAmountFocused = it.isFocused },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )

                        ExposedDropdownMenuBox(
                            expanded = currencyMenuExpanded,
                            onExpandedChange = { currencyMenuExpanded = it },
                            modifier = Modifier.widthIn(max = Dimens.space120),
                        ) {
                            OutlinedTextField(
                                value = state.currency.symbol,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text(strings.currency) },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = currencyMenuExpanded) },
                                modifier = Modifier
                                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                            )
                            ExposedDropdownMenu(
                                expanded = currencyMenuExpanded,
                                onDismissRequest = { currencyMenuExpanded = false },
                            ) {
                                Currency.entries.forEach { currency ->
                                    DropdownMenuItem(
                                        text = { Text(currency.symbol) },
                                        onClick = {
                                            viewModel.onIntent(
                                                AddEditCreditorIntent.CurrencyChanged(
                                                    currency
                                                )
                                            )
                                            currencyMenuExpanded = false
                                        },
                                    )
                                }
                            }
                        }
                    }
                    ClipboardPasteHint(
                        clipboardText = clipboardText,
                        fieldValue = state.initialAmountText,
                        isFieldFocused = initialAmountFocused,
                        isRelevant = { text ->
                            val sanitized = sanitizeAmountInput(text)
                            sanitized.isNotBlank() &&
                                runCatching { BigDecimal.parseString(sanitized) }.getOrNull()
                                    ?.let { it > BigDecimal.ZERO } == true
                        },
                        onPaste = {
                            viewModel.onIntent(AddEditCreditorIntent.InitialAmountChanged(sanitizeAmountInput(it)))
                        },
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(
                            Dimens.space8,
                            alignment = Alignment.CenterHorizontally
                        )
                    ) {
                        FilterChip(
                            selected = state.method == PaymentMethod.CASH,
                            onClick = {
                                viewModel.onIntent(
                                    AddEditCreditorIntent.MethodChanged(
                                        PaymentMethod.CASH
                                    )
                                )
                            },
                            label = { Text(strings.cash) },
                            trailingIcon = { Icon(Icons.Default.Money, null) }
                        )
                        FilterChip(
                            selected = state.method == PaymentMethod.CARD,
                            onClick = {
                                viewModel.onIntent(
                                    AddEditCreditorIntent.MethodChanged(
                                        PaymentMethod.CARD
                                    )
                                )
                            },
                            label = { Text(strings.card) },
                            trailingIcon = { Icon(Icons.Default.CreditCard, null) }
                        )
                    }
                    if (state.method == PaymentMethod.CARD) {
                        var cardLastDigitsFocused by remember { mutableStateOf(false) }
                        OutlinedTextField(
                            value = state.cardLastDigits,
                            onValueChange = {
                                viewModel.onIntent(
                                    AddEditCreditorIntent.CardLastDigitsChanged(
                                        it
                                    )
                                )
                            },
                            label = { Text(strings.cardLastDigits) },
                            modifier = Modifier.fillMaxWidth().onFocusChanged { cardLastDigitsFocused = it.isFocused },
                        )
                        ClipboardPasteHint(
                            clipboardText = clipboardText,
                            fieldValue = state.cardLastDigits,
                            isFieldFocused = cardLastDigitsFocused,
                            isRelevant = { it.filter(Char::isDigit).length in 3..6 },
                            onPaste = { viewModel.onIntent(AddEditCreditorIntent.CardLastDigitsChanged(it)) },
                        )
                    }
                }
            }
            Button(
                onClick = { viewModel.onIntent(AddEditCreditorIntent.Save) },
                enabled = !state.isSaving && !state.isLoading,
                modifier = Modifier.widthIn(max = Dimens.contentMaxWidth),
            ) {
                if (state.isSaving)
                    CircularWavyProgressIndicator(modifier = Modifier.padding(end = Dimens.space8))
                else
                    Text(strings.save)
            }
        }
    }
}

@Preview
@Composable
private fun AddEditCreditorScreenLightPhonePreview() = DebtTrackerPreview(darkTheme = false) {
    AddEditCreditorScreen(creditorId = null, onDone = {})
}

@Preview
@Composable
private fun AddEditCreditorScreenDarkPhonePreview() = DebtTrackerPreview(darkTheme = true) {
    AddEditCreditorScreen(creditorId = null, onDone = {})
}

@Preview(device = DESKTOP)
@Composable
private fun AddEditCreditorScreenLightDesktopPreview() = DebtTrackerPreview(darkTheme = false) {
    AddEditCreditorScreen(creditorId = null, onDone = {})
}

@Preview(device = DESKTOP)
@Composable
private fun AddEditCreditorScreenDarkDesktopPreview() = DebtTrackerPreview(darkTheme = true) {
    AddEditCreditorScreen(creditorId = null, onDone = {})
}
