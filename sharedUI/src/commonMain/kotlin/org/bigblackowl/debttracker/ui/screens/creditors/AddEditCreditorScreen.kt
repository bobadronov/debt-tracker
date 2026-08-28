package org.bigblackowl.debttracker.ui.screens.creditors

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Devices.DESKTOP
import androidx.compose.ui.tooling.preview.Preview
import org.bigblackowl.debttracker.core.i18n.LocalStrings
import org.bigblackowl.debttracker.domain.model.ScannedContact
import org.bigblackowl.debttracker.domain.validation.sanitizeAmountInput
import org.bigblackowl.debttracker.domain.validation.sanitizePhoneInput
import org.bigblackowl.debttracker.preview.DebtTrackerPreview
import org.bigblackowl.debttracker.ui.components.AddEditContactForm
import org.koin.compose.viewmodel.koinViewModel

/** Create form for a new [org.bigblackowl.debttracker.domain.model.Creditor]. */
@Composable
fun AddEditCreditorScreen(
    onDone: () -> Unit,
    prefill: ScannedContact? = null,
    viewModel: AddEditCreditorViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val strings = LocalStrings.current

    LaunchedEffect(Unit) {
        prefill?.let { viewModel.onIntent(AddEditCreditorIntent.ApplyScannedContact(it)) }
    }

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                AddEditCreditorEffect.Saved -> onDone()
                is AddEditCreditorEffect.Error -> snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    AddEditContactForm(
        title = strings.addEditCreditorTitleNew,
        onDone = onDone,
        snackbarHostState = snackbarHostState,
        fullName = state.fullName,
        onFullNameChange = { viewModel.onIntent(AddEditCreditorIntent.FullNameChanged(it)) },
        fullNameError = state.fullNameError,
        nameSuggestions = state.nameSuggestions,
        onSelectNameSuggestion = { viewModel.onIntent(AddEditCreditorIntent.NameSuggestionSelected(it)) },
        phone = state.phone,
        onPhoneChange = { viewModel.onIntent(AddEditCreditorIntent.PhoneChanged(sanitizePhoneInput(it))) },
        email = state.email,
        onEmailChange = { viewModel.onIntent(AddEditCreditorIntent.EmailChanged(it)) },
        profileSuggestion = state.profileSuggestion,
        onApplySuggestion = { viewModel.onIntent(AddEditCreditorIntent.ApplyProfileSuggestion) },
        onDismissSuggestion = { viewModel.onIntent(AddEditCreditorIntent.DismissProfileSuggestion) },
        comment = state.comment,
        onCommentChange = { viewModel.onIntent(AddEditCreditorIntent.CommentChanged(it)) },
        initialAmountLabel = strings.addEditCreditorInitialAmount,
        initialAmountText = state.initialAmountText,
        onInitialAmountChange = { viewModel.onIntent(AddEditCreditorIntent.InitialAmountChanged(sanitizeAmountInput(it))) },
        amountError = state.amountError,
        currency = state.currency,
        onCurrencyChange = { viewModel.onIntent(AddEditCreditorIntent.CurrencyChanged(it)) },
        method = state.method,
        onMethodChange = { viewModel.onIntent(AddEditCreditorIntent.MethodChanged(it)) },
        isSaving = state.isSaving,
        onSave = { viewModel.onIntent(AddEditCreditorIntent.Save) },
        onScannedContact = { contact -> viewModel.onIntent(AddEditCreditorIntent.ApplyScannedContact(contact)) },
    )
}

@Preview
@Composable
private fun AddEditCreditorScreenLightPhonePreview() = DebtTrackerPreview(darkTheme = false) {
    AddEditCreditorScreen(onDone = {})
}

@Preview
@Composable
private fun AddEditCreditorScreenDarkPhonePreview() = DebtTrackerPreview(darkTheme = true) {
    AddEditCreditorScreen(onDone = {})
}

@Preview(device = DESKTOP)
@Composable
private fun AddEditCreditorScreenLightDesktopPreview() = DebtTrackerPreview(darkTheme = false) {
    AddEditCreditorScreen(onDone = {})
}

@Preview(device = DESKTOP)
@Composable
private fun AddEditCreditorScreenDarkDesktopPreview() = DebtTrackerPreview(darkTheme = true) {
    AddEditCreditorScreen(onDone = {})
}
