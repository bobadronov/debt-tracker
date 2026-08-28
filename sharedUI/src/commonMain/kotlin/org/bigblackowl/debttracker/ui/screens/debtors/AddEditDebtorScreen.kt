package org.bigblackowl.debttracker.ui.screens.debtors

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

/** Create form for a new [org.bigblackowl.debttracker.domain.model.Debtor]. */
@Composable
fun AddEditDebtorScreen(
    onDone: () -> Unit,
    prefill: ScannedContact? = null,
    viewModel: AddEditDebtorViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val strings = LocalStrings.current

    LaunchedEffect(Unit) {
        prefill?.let { viewModel.onIntent(AddEditDebtorIntent.ApplyScannedContact(it)) }
    }

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                AddEditDebtorEffect.Saved -> onDone()
                is AddEditDebtorEffect.Error -> snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    AddEditContactForm(
        title = strings.addEditDebtorTitleNew,
        onDone = onDone,
        snackbarHostState = snackbarHostState,
        fullName = state.fullName,
        onFullNameChange = { viewModel.onIntent(AddEditDebtorIntent.FullNameChanged(it)) },
        fullNameError = state.fullNameError,
        nameSuggestions = state.nameSuggestions,
        onSelectNameSuggestion = { viewModel.onIntent(AddEditDebtorIntent.NameSuggestionSelected(it)) },
        phone = state.phone,
        onPhoneChange = { viewModel.onIntent(AddEditDebtorIntent.PhoneChanged(sanitizePhoneInput(it))) },
        email = state.email,
        onEmailChange = { viewModel.onIntent(AddEditDebtorIntent.EmailChanged(it)) },
        profileSuggestion = state.profileSuggestion,
        onApplySuggestion = { viewModel.onIntent(AddEditDebtorIntent.ApplyProfileSuggestion) },
        onDismissSuggestion = { viewModel.onIntent(AddEditDebtorIntent.DismissProfileSuggestion) },
        comment = state.comment,
        onCommentChange = { viewModel.onIntent(AddEditDebtorIntent.CommentChanged(it)) },
        initialAmountLabel = strings.addEditDebtorInitialAmount,
        initialAmountText = state.initialAmountText,
        onInitialAmountChange = {
            viewModel.onIntent(
                AddEditDebtorIntent.InitialAmountChanged(
                    sanitizeAmountInput(it)
                )
            )
        },
        amountError = state.amountError,
        currency = state.currency,
        onCurrencyChange = { viewModel.onIntent(AddEditDebtorIntent.CurrencyChanged(it)) },
        method = state.method,
        onMethodChange = { viewModel.onIntent(AddEditDebtorIntent.MethodChanged(it)) },
        isSaving = state.isSaving,
        onSave = { viewModel.onIntent(AddEditDebtorIntent.Save) },
        onScannedContact = { contact -> viewModel.onIntent(AddEditDebtorIntent.ApplyScannedContact(contact)) },
    )
}

@Preview
@Composable
private fun AddEditDebtorScreenLightPhonePreview() = DebtTrackerPreview(darkTheme = false) {
    AddEditDebtorScreen(onDone = {})
}

@Preview
@Composable
private fun AddEditDebtorScreenDarkPhonePreview() = DebtTrackerPreview(darkTheme = true) {
    AddEditDebtorScreen(onDone = {})
}

@Preview(device = DESKTOP)
@Composable
private fun AddEditDebtorScreenLightDesktopPreview() = DebtTrackerPreview(darkTheme = false) {
    AddEditDebtorScreen(onDone = {})
}

@Preview(device = DESKTOP)
@Composable
private fun AddEditDebtorScreenDarkDesktopPreview() = DebtTrackerPreview(darkTheme = true) {
    AddEditDebtorScreen(onDone = {})
}
