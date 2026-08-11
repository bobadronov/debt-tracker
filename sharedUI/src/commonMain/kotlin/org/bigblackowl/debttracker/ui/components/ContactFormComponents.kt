package org.bigblackowl.debttracker.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import org.bigblackowl.debttracker.core.i18n.LocalStrings
import org.bigblackowl.debttracker.domain.model.Currency
import org.bigblackowl.debttracker.domain.model.PaymentMethod
import org.bigblackowl.debttracker.domain.model.ProfileSuggestion
import org.bigblackowl.debttracker.domain.validation.isPhonePasteRelevant
import org.bigblackowl.debttracker.domain.validation.isValidAmountText
import org.bigblackowl.debttracker.domain.validation.isValidEmail
import org.bigblackowl.debttracker.domain.validation.isValidFullName
import org.bigblackowl.debttracker.theme.Dimens

/**
 * Shared create/edit form for AddEditDebtorScreen/AddEditCreditorScreen (спек §4.1): identical
 * fields and layout over a different domain model, mirroring how [ContactListScaffold]/
 * [ContactDetailScaffold] do it for the list/detail screens — each screen only supplies its own
 * state values and intent-dispatching callbacks.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditContactForm(
    title: String,
    onDone: () -> Unit,
    snackbarHostState: SnackbarHostState,
    fullName: String,
    onFullNameChange: (String) -> Unit,
    fullNameError: String?,
    phone: String,
    onPhoneChange: (String) -> Unit,
    email: String,
    onEmailChange: (String) -> Unit,
    profileSuggestion: ProfileSuggestion?,
    onApplySuggestion: () -> Unit,
    onDismissSuggestion: () -> Unit,
    comment: String,
    onCommentChange: (String) -> Unit,
    isEditing: Boolean,
    initialAmountLabel: String,
    initialAmountText: String,
    onInitialAmountChange: (String) -> Unit,
    amountError: String?,
    currency: Currency,
    onCurrencyChange: (Currency) -> Unit,
    method: PaymentMethod,
    onMethodChange: (PaymentMethod) -> Unit,
    cardLastDigits: String,
    onCardLastDigitsChange: (String) -> Unit,
    isSaving: Boolean,
    isLoading: Boolean,
    onSave: () -> Unit,
) {
    val strings = LocalStrings.current
    val clipboardText by rememberClipboardText()

    Scaffold(
        topBar = { BackTopAppBar(title = title, onBack = onDone) },
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
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier = Modifier.width(Dimens.contentMaxWidth),
                verticalArrangement = Arrangement.spacedBy(Dimens.space12),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                PasteableOutlinedTextField(
                    value = fullName,
                    onValueChange = onFullNameChange,
                    label = strings.fullName,
                    placeholder = strings.fullNamePlaceholder,
                    isError = fullNameError != null,
                    supportingText = fullNameError,
                    clipboardText = clipboardText,
                    isPasteRelevant = ::isValidFullName,
                )
                PasteableOutlinedTextField(
                    value = phone,
                    onValueChange = onPhoneChange,
                    label = strings.phone,
                    modifier = Modifier.semantics { contentType = ContentType.PhoneNumber },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    visualTransformation = remember { UkrainianPhoneVisualTransformation() },
                    clipboardText = clipboardText,
                    isPasteRelevant = ::isPhonePasteRelevant,
                )
                PasteableOutlinedTextField(
                    value = email,
                    onValueChange = onEmailChange,
                    label = strings.email,
                    modifier = Modifier.semantics { contentType = ContentType.EmailAddress },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    clipboardText = clipboardText,
                    isPasteRelevant = ::isValidEmail,
                )
                profileSuggestion?.let { suggestion ->
                    ProfileSuggestionCard(
                        suggestion = suggestion,
                        onUse = onApplySuggestion,
                        onDismiss = onDismissSuggestion,
                    )
                }
                PasteableOutlinedTextField(
                    value = comment,
                    onValueChange = onCommentChange,
                    label = strings.comment,
                    clipboardText = clipboardText,
                    isPasteRelevant = { it.trim().length in 1..500 },
                )

                if (!isEditing) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Dimens.space5),
                    ) {
                        PasteableOutlinedTextField(
                            value = initialAmountText,
                            onValueChange = onInitialAmountChange,
                            label = initialAmountLabel,
                            isError = amountError != null,
                            supportingText = amountError,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            clipboardText = clipboardText,
                            isPasteRelevant = ::isValidAmountText,
                            onPaste = onInitialAmountChange,
                            modifier = Modifier.weight(1f),
                        )
                        CurrencyDropdownField(
                            selected = currency,
                            onSelect = onCurrencyChange,
                            label = strings.currency,
                            modifier = Modifier.widthIn(max = Dimens.space120),
                        )
                    }
                    PaymentMethodChipRow(
                        selected = method,
                        onSelect = onMethodChange,
                    )
                    if (method == PaymentMethod.CARD) {
                        PasteableOutlinedTextField(
                            value = cardLastDigits,
                            onValueChange = onCardLastDigitsChange,
                            label = strings.cardLastDigits,
                            clipboardText = clipboardText,
                            isPasteRelevant = { it.filter(Char::isDigit).length in 3..6 },
                        )
                    }
                }
            }
            LoadingButton(
                onClick = onSave,
                isLoading = isSaving,
                enabled = !isLoading,
                modifier = Modifier.widthIn(max = Dimens.contentMaxWidth),
                label = { Text(strings.save) },
            )
        }
    }
}
