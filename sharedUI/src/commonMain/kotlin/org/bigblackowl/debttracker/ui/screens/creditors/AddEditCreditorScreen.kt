package org.bigblackowl.debttracker.ui.screens.creditors

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Devices.DESKTOP
import androidx.compose.ui.tooling.preview.Preview
import org.bigblackowl.debttracker.core.i18n.LocalStrings
import org.bigblackowl.debttracker.domain.validation.sanitizeAmountInput
import org.bigblackowl.debttracker.domain.validation.sanitizePhoneInput
import org.bigblackowl.debttracker.preview.DebtTrackerPreview
import org.bigblackowl.debttracker.ui.components.AddEditContactForm
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/** Create/edit form for a [org.bigblackowl.debttracker.domain.model.Creditor] — `creditorId == null` means "new". */
@Composable
fun AddEditCreditorScreen(
    creditorId: String?,
    onDone: () -> Unit,
) {
    val viewModel: AddEditCreditorViewModel = koinViewModel { parametersOf(creditorId) }
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val strings = LocalStrings.current

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                AddEditCreditorEffect.Saved -> onDone()
                is AddEditCreditorEffect.Error -> snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    AddEditContactForm(
        title = if (state.isEditing) strings.addEditCreditorTitleEdit else strings.addEditCreditorTitleNew,
        onDone = onDone,
        snackbarHostState = snackbarHostState,
        fullName = state.fullName,
        onFullNameChange = { viewModel.onIntent(AddEditCreditorIntent.FullNameChanged(it)) },
        fullNameError = state.fullNameError,
        phone = state.phone,
        onPhoneChange = { viewModel.onIntent(AddEditCreditorIntent.PhoneChanged(sanitizePhoneInput(it))) },
        email = state.email,
        onEmailChange = { viewModel.onIntent(AddEditCreditorIntent.EmailChanged(it)) },
        profileSuggestion = state.profileSuggestion,
        onApplySuggestion = { viewModel.onIntent(AddEditCreditorIntent.ApplyProfileSuggestion) },
        onDismissSuggestion = { viewModel.onIntent(AddEditCreditorIntent.DismissProfileSuggestion) },
        comment = state.comment,
        onCommentChange = { viewModel.onIntent(AddEditCreditorIntent.CommentChanged(it)) },
        isEditing = state.isEditing,
        initialAmountLabel = strings.addEditCreditorInitialAmount,
        initialAmountText = state.initialAmountText,
        onInitialAmountChange = { viewModel.onIntent(AddEditCreditorIntent.InitialAmountChanged(sanitizeAmountInput(it))) },
        amountError = state.amountError,
        currency = state.currency,
        onCurrencyChange = { viewModel.onIntent(AddEditCreditorIntent.CurrencyChanged(it)) },
        method = state.method,
        onMethodChange = { viewModel.onIntent(AddEditCreditorIntent.MethodChanged(it)) },
        cardLastDigits = state.cardLastDigits,
        onCardLastDigitsChange = { viewModel.onIntent(AddEditCreditorIntent.CardLastDigitsChanged(it)) },
        isSaving = state.isSaving,
        isLoading = state.isLoading,
        onSave = { viewModel.onIntent(AddEditCreditorIntent.Save) },
    )
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
