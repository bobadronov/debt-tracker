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
import org.bigblackowl.debttracker.domain.model.ContactSuggestion
import org.bigblackowl.debttracker.domain.model.Currency
import org.bigblackowl.debttracker.domain.model.DebtDirection
import org.bigblackowl.debttracker.domain.model.PaymentMethod
import org.bigblackowl.debttracker.domain.model.ScannedContact
import org.bigblackowl.debttracker.domain.validation.sanitizeAmountInput
import org.bigblackowl.debttracker.domain.validation.sanitizePhoneInput
import org.bigblackowl.debttracker.preview.DebtTrackerPreview
import org.bigblackowl.debttracker.ui.components.UnsavedChangesGuard
import org.bigblackowl.debttracker.ui.components.contact.AddEditContactForm
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

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                AddEditContactEffect.Saved -> onDone()
                is AddEditContactEffect.Error -> snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    AddEditContactContent(
        state = state,
        snackbarHostState = snackbarHostState,
        onDone = onDone,
        onDirectionChange = { viewModel.onIntent(AddEditContactIntent.DirectionChanged(it)) },
        onFullNameChange = { viewModel.onIntent(AddEditContactIntent.FullNameChanged(it)) },
        onSelectNameSuggestion = { viewModel.onIntent(AddEditContactIntent.NameSuggestionSelected(it)) },
        onPhoneChange = { viewModel.onIntent(AddEditContactIntent.PhoneChanged(sanitizePhoneInput(it))) },
        onEmailChange = { viewModel.onIntent(AddEditContactIntent.EmailChanged(it)) },
        onApplySuggestion = { viewModel.onIntent(AddEditContactIntent.ApplyProfileSuggestion) },
        onDismissSuggestion = { viewModel.onIntent(AddEditContactIntent.DismissProfileSuggestion) },
        onCommentChange = { viewModel.onIntent(AddEditContactIntent.CommentChanged(it)) },
        onInitialAmountChange = { viewModel.onIntent(AddEditContactIntent.InitialAmountChanged(sanitizeAmountInput(it))) },
        onCurrencyChange = { viewModel.onIntent(AddEditContactIntent.CurrencyChanged(it)) },
        onMethodChange = { viewModel.onIntent(AddEditContactIntent.MethodChanged(it)) },
        onDueDateChange = { viewModel.onIntent(AddEditContactIntent.DueDateChanged(it)) },
        onToggleReminderLead = { viewModel.onIntent(AddEditContactIntent.ToggleReminderLead(it)) },
        onSave = { viewModel.onIntent(AddEditContactIntent.Save) },
        onScannedContact = { viewModel.onIntent(AddEditContactIntent.ApplyScannedContact(it)) },
    )
}

@Composable
private fun AddEditContactContent(
    state: AddEditContactState,
    snackbarHostState: SnackbarHostState,
    onDone: () -> Unit,
    onDirectionChange: (DebtDirection) -> Unit,
    onFullNameChange: (String) -> Unit,
    onSelectNameSuggestion: (ContactSuggestion) -> Unit,
    onPhoneChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onApplySuggestion: () -> Unit,
    onDismissSuggestion: () -> Unit,
    onCommentChange: (String) -> Unit,
    onInitialAmountChange: (String) -> Unit,
    onCurrencyChange: (Currency) -> Unit,
    onMethodChange: (PaymentMethod) -> Unit,
    onDueDateChange: (kotlin.time.Instant?) -> Unit,
    onToggleReminderLead: (Int) -> Unit,
    onSave: () -> Unit,
    onScannedContact: (ScannedContact) -> Unit,
) {
    val strings = LocalStrings.current
    UnsavedChangesGuard(
        hasUnsavedChanges = state.hasUnsavedChanges,
        onSave = onSave,
        onDiscard = onDone,
    )

    val title = when (state.direction) {
        DebtDirection.DEBTOR ->
            if (state.isEditMode) strings.addEditDebtor.titleEdit else strings.addEditDebtor.titleNew
        DebtDirection.CREDITOR ->
            if (state.isEditMode) strings.addEditCreditor.titleEdit else strings.addEditCreditor.titleNew
    }
    val initialAmountLabel = when (state.direction) {
        DebtDirection.DEBTOR -> strings.addEditDebtor.initialAmount
        DebtDirection.CREDITOR -> strings.addEditCreditor.initialAmount
    }

    AddEditContactForm(
        title = title,
        onDone = onDone,
        snackbarHostState = snackbarHostState,
        isEditMode = state.isEditMode,
        // In edit mode a debtor stays a debtor — the direction toggle is hidden (null).
        direction = state.direction.takeUnless { state.isEditMode },
        onDirectionChange = onDirectionChange,
        avatarUrl = state.suggestedAvatarUrl,
        fullName = state.fullName,
        onFullNameChange = onFullNameChange,
        fullNameError = state.fullNameError,
        nameSuggestions = state.nameSuggestions,
        onSelectNameSuggestion = onSelectNameSuggestion,
        phone = state.phone,
        onPhoneChange = onPhoneChange,
        email = state.email,
        onEmailChange = onEmailChange,
        profileSuggestion = state.profileSuggestion,
        onApplySuggestion = onApplySuggestion,
        onDismissSuggestion = onDismissSuggestion,
        comment = state.comment,
        onCommentChange = onCommentChange,
        initialAmountLabel = initialAmountLabel,
        initialAmountText = state.initialAmountText,
        onInitialAmountChange = onInitialAmountChange,
        amountError = state.amountError,
        currency = state.currency,
        onCurrencyChange = onCurrencyChange,
        method = state.method,
        onMethodChange = onMethodChange,
        dueDate = state.dueDate,
        onDueDateChange = onDueDateChange,
        reminderLeadDays = state.reminderLeadDays,
        onToggleReminderLead = onToggleReminderLead,
        isSaving = state.isSaving,
        onSave = onSave,
        onScannedContact = if (state.isEditMode) null else onScannedContact,
    )
}

@Composable
private fun Preview(state: AddEditContactState) = AddEditContactContent(
    state = state,
    snackbarHostState = remember { SnackbarHostState() },
    onDone = {},
    onDirectionChange = {},
    onFullNameChange = {},
    onSelectNameSuggestion = {},
    onPhoneChange = {},
    onEmailChange = {},
    onApplySuggestion = {},
    onDismissSuggestion = {},
    onCommentChange = {},
    onInitialAmountChange = {},
    onCurrencyChange = {},
    onMethodChange = {},
    onDueDateChange = {},
    onToggleReminderLead = {},
    onSave = {},
    onScannedContact = {},
)

@Preview
@Composable
private fun AddEditContactScreenLightPhonePreview() = DebtTrackerPreview(darkTheme = false) {
    Preview(AddEditContactState(direction = DebtDirection.DEBTOR))
}

@Preview
@Composable
private fun AddEditContactScreenDarkPhonePreview() = DebtTrackerPreview(darkTheme = true) {
    Preview(AddEditContactState(direction = DebtDirection.CREDITOR))
}

@Preview(device = DESKTOP)
@Composable
private fun AddEditContactScreenLightDesktopPreview() = DebtTrackerPreview(darkTheme = false) {
    Preview(AddEditContactState(direction = DebtDirection.DEBTOR))
}

@Preview(device = DESKTOP)
@Composable
private fun AddEditContactScreenDarkDesktopPreview() = DebtTrackerPreview(darkTheme = true) {
    Preview(AddEditContactState(direction = DebtDirection.CREDITOR))
}

@Preview
@Composable
private fun AddEditContactScreenEditModePreview() = DebtTrackerPreview(darkTheme = false) {
    Preview(AddEditContactState(direction = DebtDirection.DEBTOR, isEditMode = true, fullName = "Тарас Шевченко", phone = "0501234567"))
}

@Preview
@Composable
private fun AddEditContactScreenErrorsPreview() = DebtTrackerPreview(darkTheme = false) {
    Preview(
        AddEditContactState(
            direction = DebtDirection.DEBTOR,
            fullNameError = "Введіть ім'я",
            amountError = "Некоректна сума",
        ),
    )
}

@Preview
@Composable
private fun AddEditContactScreenSavingPreview() = DebtTrackerPreview(darkTheme = false) {
    Preview(AddEditContactState(direction = DebtDirection.DEBTOR, fullName = "Тарас Шевченко", isSaving = true))
}
