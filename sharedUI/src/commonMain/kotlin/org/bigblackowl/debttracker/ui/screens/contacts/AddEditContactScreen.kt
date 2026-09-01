package org.bigblackowl.debttracker.ui.screens.contacts

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Devices.DESKTOP
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.bigblackowl.debttracker.core.i18n.LocalStrings
import org.bigblackowl.debttracker.domain.model.ContactPrefill
import org.bigblackowl.debttracker.domain.model.DebtDirection
import org.bigblackowl.debttracker.domain.validation.sanitizeAmountInput
import org.bigblackowl.debttracker.domain.validation.sanitizePhoneInput
import org.bigblackowl.debttracker.preview.DebtTrackerPreview
import org.bigblackowl.debttracker.ui.components.AddEditContactForm
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * Merged "Add record" screen — creates a new debtor ("owes me") or creditor ("I owe") depending on
 * the [DebtDirection] toggle. Replaces the former separate `AddEditDebtorScreen`/`AddEditCreditorScreen`.
 */
@Composable
fun AddEditContactScreen(
    direction: DebtDirection,
    onDone: () -> Unit,
    prefill: ContactPrefill? = null,
    editId: String? = null,
    viewModel: AddEditContactViewModel = koinViewModel { parametersOf(direction, prefill, editId) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val strings = LocalStrings.current

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                AddEditContactEffect.Saved -> onDone()
                is AddEditContactEffect.Error -> snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    val title = when (state.direction) {
        DebtDirection.DEBTOR ->
            if (state.isEditMode) strings.addEditDebtorTitleEdit else strings.addEditDebtorTitleNew
        DebtDirection.CREDITOR ->
            if (state.isEditMode) strings.addEditCreditorTitleEdit else strings.addEditCreditorTitleNew
    }
    val initialAmountLabel = when (state.direction) {
        DebtDirection.DEBTOR -> strings.addEditDebtorInitialAmount
        DebtDirection.CREDITOR -> strings.addEditCreditorInitialAmount
    }

    AddEditContactForm(
        title = title,
        onDone = onDone,
        snackbarHostState = snackbarHostState,
        isEditMode = state.isEditMode,
        // In edit mode a debtor stays a debtor — the direction toggle is hidden (null).
        direction = state.direction.takeUnless { state.isEditMode },
        onDirectionChange = { viewModel.onIntent(AddEditContactIntent.DirectionChanged(it)) },
        avatarUrl = state.suggestedAvatarUrl,
        fullName = state.fullName,
        onFullNameChange = { viewModel.onIntent(AddEditContactIntent.FullNameChanged(it)) },
        fullNameError = state.fullNameError,
        nameSuggestions = state.nameSuggestions,
        onSelectNameSuggestion = { viewModel.onIntent(AddEditContactIntent.NameSuggestionSelected(it)) },
        phone = state.phone,
        onPhoneChange = { viewModel.onIntent(AddEditContactIntent.PhoneChanged(sanitizePhoneInput(it))) },
        email = state.email,
        onEmailChange = { viewModel.onIntent(AddEditContactIntent.EmailChanged(it)) },
        profileSuggestion = state.profileSuggestion,
        onApplySuggestion = { viewModel.onIntent(AddEditContactIntent.ApplyProfileSuggestion) },
        onDismissSuggestion = { viewModel.onIntent(AddEditContactIntent.DismissProfileSuggestion) },
        comment = state.comment,
        onCommentChange = { viewModel.onIntent(AddEditContactIntent.CommentChanged(it)) },
        initialAmountLabel = initialAmountLabel,
        initialAmountText = state.initialAmountText,
        onInitialAmountChange = { viewModel.onIntent(AddEditContactIntent.InitialAmountChanged(sanitizeAmountInput(it))) },
        amountError = state.amountError,
        currency = state.currency,
        onCurrencyChange = { viewModel.onIntent(AddEditContactIntent.CurrencyChanged(it)) },
        method = state.method,
        onMethodChange = { viewModel.onIntent(AddEditContactIntent.MethodChanged(it)) },
        dueDate = state.dueDate,
        onDueDateChange = { viewModel.onIntent(AddEditContactIntent.DueDateChanged(it)) },
        reminderLeadDays = state.reminderLeadDays,
        onToggleReminderLead = { viewModel.onIntent(AddEditContactIntent.ToggleReminderLead(it)) },
        isSaving = state.isSaving,
        onSave = { viewModel.onIntent(AddEditContactIntent.Save) },
        onScannedContact = if (state.isEditMode) null else {
            { contact -> viewModel.onIntent(AddEditContactIntent.ApplyScannedContact(contact)) }
        },
    )
}

@Preview
@Composable
private fun AddEditContactScreenLightPhonePreview() = DebtTrackerPreview(darkTheme = false) {
    AddEditContactScreen(direction = DebtDirection.DEBTOR, onDone = {})
}

@Preview
@Composable
private fun AddEditContactScreenDarkPhonePreview() = DebtTrackerPreview(darkTheme = true) {
    AddEditContactScreen(direction = DebtDirection.CREDITOR, onDone = {})
}

@Preview(device = DESKTOP)
@Composable
private fun AddEditContactScreenLightDesktopPreview() = DebtTrackerPreview(darkTheme = false) {
    AddEditContactScreen(direction = DebtDirection.DEBTOR, onDone = {})
}

@Preview(device = DESKTOP)
@Composable
private fun AddEditContactScreenDarkDesktopPreview() = DebtTrackerPreview(darkTheme = true) {
    AddEditContactScreen(direction = DebtDirection.CREDITOR, onDone = {})
}
