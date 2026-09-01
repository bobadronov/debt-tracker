package org.bigblackowl.debttracker.ui.screens.contacts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ionspin.kotlin.bignum.decimal.BigDecimal
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import org.bigblackowl.debttracker.core.i18n.resolveStrings
import org.bigblackowl.debttracker.core.settings.AppSettings
import org.bigblackowl.debttracker.core.sound.SoundEffect
import org.bigblackowl.debttracker.core.sound.SoundPlayer
import org.bigblackowl.debttracker.domain.model.ContactPrefill
import org.bigblackowl.debttracker.domain.model.ContactSuggestion
import org.bigblackowl.debttracker.domain.model.Creditor
import org.bigblackowl.debttracker.domain.model.CreditorTransaction
import org.bigblackowl.debttracker.domain.model.DebtDirection
import org.bigblackowl.debttracker.domain.model.DebtStatus
import org.bigblackowl.debttracker.domain.model.DebtTransaction
import org.bigblackowl.debttracker.domain.model.Debtor
import org.bigblackowl.debttracker.domain.model.MyDebtTransactionType
import org.bigblackowl.debttracker.domain.model.ScannedContact
import org.bigblackowl.debttracker.domain.model.SyncStatus
import org.bigblackowl.debttracker.domain.model.TransactionType
import org.bigblackowl.debttracker.domain.usecase.FindProfileByEmailUseCase
import org.bigblackowl.debttracker.domain.usecase.ObserveContactSuggestionsUseCase
import org.bigblackowl.debttracker.domain.usecase.creditor.AddCreditorTransactionUseCase
import org.bigblackowl.debttracker.domain.usecase.creditor.ObserveCreditorUseCase
import org.bigblackowl.debttracker.domain.usecase.creditor.AddOrUpdateCreditorUseCase
import org.bigblackowl.debttracker.domain.usecase.creditor.DeleteCreditorUseCase
import org.bigblackowl.debttracker.domain.usecase.creditor.LinkCreditorToRegisteredUserUseCase
import org.bigblackowl.debttracker.domain.usecase.debtor.AddDebtTransactionUseCase
import org.bigblackowl.debttracker.domain.usecase.debtor.AddOrUpdateDebtorUseCase
import org.bigblackowl.debttracker.domain.usecase.debtor.DeleteDebtorUseCase
import org.bigblackowl.debttracker.domain.usecase.debtor.LinkDebtorToRegisteredUserUseCase
import org.bigblackowl.debttracker.domain.usecase.debtor.ObserveDebtorUseCase
import org.bigblackowl.debttracker.domain.validation.isValidEmail
import org.bigblackowl.debttracker.domain.validation.isValidFullName
import org.bigblackowl.debttracker.domain.validation.sanitizePhoneInput

private const val EMAIL_LOOKUP_DEBOUNCE_MS = 500L
private const val NAME_SUGGESTIONS_LIMIT = 5

/**
 * Validates the merged "Add record" form and saves a new debtor or creditor (plus its opening
 * transaction) on [AddEditContactIntent.Save], choosing the target by [AddEditContactState.direction]
 * ([AddEditContactIntent.DirectionChanged]). Merges the former `AddEditDebtorViewModel` and
 * `AddEditCreditorViewModel`, which were identical apart from those domain types.
 *
 * While the user types an email, debounces a lookup against [findProfileByEmail] to offer a
 * name/photo autofill suggestion (§ProfileLookup) — purely additive, never blocks saving.
 * Separately, [observeContactSuggestions] feeds the inline name-autocomplete list; picking one
 * ([AddEditContactIntent.NameSuggestionSelected]) carries over its phone/email/comment too.
 */
@OptIn(ExperimentalUuidApi::class)
class AddEditContactViewModel(
    direction: DebtDirection,
    prefill: ContactPrefill?,
    private val editId: String?,
    private val addOrUpdateDebtor: AddOrUpdateDebtorUseCase,
    private val addDebtTransaction: AddDebtTransactionUseCase,
    private val deleteDebtor: DeleteDebtorUseCase,
    private val linkDebtorToRegisteredUser: LinkDebtorToRegisteredUserUseCase,
    private val addOrUpdateCreditor: AddOrUpdateCreditorUseCase,
    private val addCreditorTransaction: AddCreditorTransactionUseCase,
    private val deleteCreditor: DeleteCreditorUseCase,
    private val linkCreditorToRegisteredUser: LinkCreditorToRegisteredUserUseCase,
    private val appSettings: AppSettings,
    private val soundPlayer: SoundPlayer,
    private val findProfileByEmail: FindProfileByEmailUseCase,
    private val observeContactSuggestions: ObserveContactSuggestionsUseCase,
    private val observeDebtor: ObserveDebtorUseCase,
    private val observeCreditor: ObserveCreditorUseCase,
) : ViewModel() {

    private var emailLookupJob: Job? = null
    private var allContacts: List<ContactSuggestion> = emptyList()

    /** The row being edited (kept out of UI state — the screen only needs the flattened fields). */
    private var editingDebtor: Debtor? = null
    private var editingCreditor: Creditor? = null

    private val _state = MutableStateFlow(
        AddEditContactState(
            direction = direction,
            isEditMode = editId != null,
            fullName = prefill?.fullName.orEmpty(),
            phone = prefill?.phone?.let(::sanitizePhoneInput).orEmpty(),
            email = prefill?.email.orEmpty(),
            comment = prefill?.comment.orEmpty(),
        )
    )
    val state: StateFlow<AddEditContactState> = _state.asStateFlow()

    private val effectsChannel = Channel<AddEditContactEffect>()
    val effects = effectsChannel.receiveAsFlow()

    init {
        viewModelScope.launch {
            observeContactSuggestions().collect { contacts ->
                allContacts = contacts
                updateNameSuggestions()
            }
        }
        if (editId != null) loadForEdit(editId, direction)
    }

    /** Prefills the form from the existing debtor/creditor. Server data wins on later sync (LWW). */
    private fun loadForEdit(id: String, direction: DebtDirection) {
        viewModelScope.launch {
            when (direction) {
                DebtDirection.DEBTOR -> {
                    val debtor = observeDebtor(id).filterNotNull().first()
                    editingDebtor = debtor
                    _state.update {
                        it.copy(
                            fullName = debtor.fullName,
                            phone = debtor.phone?.let(::sanitizePhoneInput).orEmpty(),
                            email = debtor.email.orEmpty(),
                            comment = debtor.comment.orEmpty(),
                            currency = debtor.currency,
                            suggestedAvatarUrl = debtor.avatarUrl,
                            dueDate = debtor.dueDate,
                            reminderLeadDays = debtor.reminderLeadDays,
                        )
                    }
                }
                DebtDirection.CREDITOR -> {
                    val creditor = observeCreditor(id).filterNotNull().first()
                    editingCreditor = creditor
                    _state.update {
                        it.copy(
                            fullName = creditor.fullName,
                            phone = creditor.phone?.let(::sanitizePhoneInput).orEmpty(),
                            email = creditor.email.orEmpty(),
                            comment = creditor.comment.orEmpty(),
                            currency = creditor.currency,
                            suggestedAvatarUrl = creditor.avatarUrl,
                            dueDate = creditor.dueDate,
                            reminderLeadDays = creditor.reminderLeadDays,
                        )
                    }
                }
            }
        }
    }

    fun onIntent(intent: AddEditContactIntent) {
        when (intent) {
            is AddEditContactIntent.DirectionChanged -> _state.update { it.copy(direction = intent.value) }
            is AddEditContactIntent.FullNameChanged -> {
                _state.update { it.copy(fullName = intent.value, fullNameError = null) }
                updateNameSuggestions()
            }
            is AddEditContactIntent.PhoneChanged -> _state.update { it.copy(phone = intent.value) }
            is AddEditContactIntent.EmailChanged -> onEmailChanged(intent.value)
            is AddEditContactIntent.CommentChanged -> _state.update { it.copy(comment = intent.value) }
            is AddEditContactIntent.InitialAmountChanged -> _state.update { it.copy(initialAmountText = intent.value, amountError = null) }
            is AddEditContactIntent.CurrencyChanged -> _state.update { it.copy(currency = intent.value) }
            is AddEditContactIntent.MethodChanged -> _state.update { it.copy(method = intent.value) }
            is AddEditContactIntent.DueDateChanged -> _state.update {
                it.copy(dueDate = intent.value, reminderLeadDays = if (intent.value == null) emptySet() else it.reminderLeadDays)
            }
            is AddEditContactIntent.ToggleReminderLead -> _state.update {
                it.copy(
                    reminderLeadDays = if (intent.days in it.reminderLeadDays) {
                        it.reminderLeadDays - intent.days
                    } else {
                        it.reminderLeadDays + intent.days
                    },
                )
            }
            AddEditContactIntent.ApplyProfileSuggestion -> applySuggestion()
            AddEditContactIntent.DismissProfileSuggestion -> _state.update { it.copy(profileSuggestion = null) }
            is AddEditContactIntent.NameSuggestionSelected -> applyNameSuggestion(intent.suggestion)
            is AddEditContactIntent.ApplyScannedContact -> applyScannedContact(intent.contact)
            AddEditContactIntent.Save -> save()
        }
    }

    private fun updateNameSuggestions() {
        val query = _state.value.fullName.trim()
        val matches = if (query.isBlank()) {
            emptyList()
        } else {
            allContacts
                .filter { it.fullName.contains(query, ignoreCase = true) && !it.fullName.equals(query, ignoreCase = true) }
                .take(NAME_SUGGESTIONS_LIMIT)
        }
        _state.update { it.copy(nameSuggestions = matches) }
    }

    private fun applyNameSuggestion(suggestion: ContactSuggestion) {
        _state.update {
            it.copy(
                fullName = suggestion.fullName,
                fullNameError = null,
                phone = suggestion.phone ?: it.phone,
                email = suggestion.email ?: it.email,
                comment = suggestion.comment ?: it.comment,
                suggestedAvatarUrl = suggestion.avatarUrl ?: it.suggestedAvatarUrl,
                nameSuggestions = emptyList(),
            )
        }
    }

    private fun applyScannedContact(contact: ScannedContact) {
        _state.update {
            it.copy(
                fullName = contact.fullName,
                fullNameError = null,
                // sanitizePhoneInput: the phone field's UkrainianPhoneVisualTransformation expects
                // a bare national number (no "+380"/spaces) — a scanned contact.phone can be in
                // whatever format its owner typed into their own "My card" QR.
                phone = contact.phone?.let(::sanitizePhoneInput) ?: it.phone,
                email = contact.email ?: it.email,
            )
        }
    }

    private fun onEmailChanged(value: String) {
        _state.update { it.copy(email = value, profileSuggestion = null) }
        emailLookupJob?.cancel()
        if (!isValidEmail(value)) return
        emailLookupJob = viewModelScope.launch {
            delay(EMAIL_LOOKUP_DEBOUNCE_MS)
            val suggestion = findProfileByEmail(value.trim())
            if (suggestion != null && _state.value.email == value) {
                _state.update { it.copy(profileSuggestion = suggestion) }
            }
        }
    }

    private fun applySuggestion() {
        val suggestion = _state.value.profileSuggestion ?: return
        _state.update {
            it.copy(
                fullName = suggestion.displayName?.takeIf(String::isNotBlank) ?: it.fullName,
                fullNameError = null,
                suggestedAvatarUrl = suggestion.avatarUrl,
                profileSuggestion = null,
            )
        }
    }

    private fun save() {
        val current = _state.value
        val strings = resolveStrings(appSettings.locale)

        if (!isValidFullName(current.fullName)) {
            _state.update { it.copy(fullNameError = strings.fullNameError) }
            return
        }

        if (current.isEditMode) {
            when (current.direction) {
                DebtDirection.DEBTOR -> updateDebtor(current, strings.saveError)
                DebtDirection.CREDITOR -> updateCreditor(current, strings.saveError)
            }
            return
        }

        val parsedAmount = runCatching { BigDecimal.parseString(current.initialAmountText.trim()) }.getOrNull()
        if (parsedAmount == null || parsedAmount <= BigDecimal.ZERO) {
            _state.update { it.copy(amountError = strings.amountError) }
            return
        }

        when (current.direction) {
            DebtDirection.DEBTOR -> saveDebtor(current, parsedAmount, strings.saveError)
            DebtDirection.CREDITOR -> saveCreditor(current, parsedAmount, strings.saveError)
        }
    }

    private fun saveDebtor(current: AddEditContactState, amount: BigDecimal, saveError: String) {
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true) }
            val now = Clock.System.now()
            val debtor = Debtor(
                id = Uuid.random().toString(),
                fullName = current.fullName.trim(),
                phone = current.phone.trim().ifBlank { null },
                email = current.email.trim().ifBlank { null },
                avatarUrl = current.suggestedAvatarUrl,
                comment = current.comment.trim().ifBlank { null },
                createdAt = now,
                updatedAt = now,
                status = DebtStatus.ACTIVE,
                syncStatus = SyncStatus.PENDING,
                currency = current.currency,
                dueDate = current.dueDate,
                reminderLeadDays = current.reminderLeadDays,
            )

            runCatching { addOrUpdateDebtor(debtor) }.onFailure {
                _state.update { it.copy(isSaving = false) }
                effectsChannel.send(AddEditContactEffect.Error(saveError))
                return@launch
            }

            runCatching {
                addDebtTransaction(
                    DebtTransaction(
                        id = Uuid.random().toString(),
                        debtorId = debtor.id,
                        amount = amount.negate(),
                        type = TransactionType.LEND,
                        method = current.method,
                        date = now,
                        comment = null,
                        createdAt = now,
                        updatedAt = now,
                        syncStatus = SyncStatus.PENDING,
                    )
                )
            }.onFailure {
                // The debtor write committed but its opening transaction didn't — undo it.
                runCatching { deleteDebtor(debtor.id) }
                _state.update { it.copy(isSaving = false) }
                effectsChannel.send(AddEditContactEffect.Error(saveError))
                return@launch
            }

            finishSave()
            viewModelScope.launch { runCatching { linkDebtorToRegisteredUser(debtor.id) } }
        }
    }

    private fun saveCreditor(current: AddEditContactState, amount: BigDecimal, saveError: String) {
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true) }
            val now = Clock.System.now()
            val creditor = Creditor(
                id = Uuid.random().toString(),
                fullName = current.fullName.trim(),
                phone = current.phone.trim().ifBlank { null },
                email = current.email.trim().ifBlank { null },
                avatarUrl = current.suggestedAvatarUrl,
                comment = current.comment.trim().ifBlank { null },
                createdAt = now,
                updatedAt = now,
                status = DebtStatus.ACTIVE,
                syncStatus = SyncStatus.PENDING,
                currency = current.currency,
                dueDate = current.dueDate,
                reminderLeadDays = current.reminderLeadDays,
            )

            runCatching { addOrUpdateCreditor(creditor) }.onFailure {
                _state.update { it.copy(isSaving = false) }
                effectsChannel.send(AddEditContactEffect.Error(saveError))
                return@launch
            }

            runCatching {
                addCreditorTransaction(
                    CreditorTransaction(
                        id = Uuid.random().toString(),
                        creditorId = creditor.id,
                        amount = amount.negate(),
                        type = MyDebtTransactionType.BORROW,
                        method = current.method,
                        date = now,
                        comment = null,
                        createdAt = now,
                        updatedAt = now,
                        syncStatus = SyncStatus.PENDING,
                    )
                )
            }.onFailure {
                // The creditor write committed but its opening transaction didn't — undo it.
                runCatching { deleteCreditor(creditor.id) }
                _state.update { it.copy(isSaving = false) }
                effectsChannel.send(AddEditContactEffect.Error(saveError))
                return@launch
            }

            finishSave()
            viewModelScope.launch { runCatching { linkCreditorToRegisteredUser(creditor.id) } }
        }
    }

    private fun updateDebtor(current: AddEditContactState, saveError: String) {
        val original = editingDebtor
        if (original == null) {
            viewModelScope.launch { effectsChannel.send(AddEditContactEffect.Error(saveError)) }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true) }
            val updated = original.copy(
                fullName = current.fullName.trim(),
                phone = current.phone.trim().ifBlank { null },
                email = current.email.trim().ifBlank { null },
                comment = current.comment.trim().ifBlank { null },
                currency = current.currency,
                avatarUrl = current.suggestedAvatarUrl ?: original.avatarUrl,
                dueDate = current.dueDate,
                reminderLeadDays = current.reminderLeadDays,
                updatedAt = Clock.System.now(),
                syncStatus = SyncStatus.PENDING,
            )
            runCatching { addOrUpdateDebtor(updated) }.onFailure {
                _state.update { it.copy(isSaving = false) }
                effectsChannel.send(AddEditContactEffect.Error(saveError))
                return@launch
            }
            finishSave()
            // Re-link in case the email/phone changed to (or away from) a registered user's.
            viewModelScope.launch { runCatching { linkDebtorToRegisteredUser(updated.id) } }
        }
    }

    private fun updateCreditor(current: AddEditContactState, saveError: String) {
        val original = editingCreditor
        if (original == null) {
            viewModelScope.launch { effectsChannel.send(AddEditContactEffect.Error(saveError)) }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true) }
            val updated = original.copy(
                fullName = current.fullName.trim(),
                phone = current.phone.trim().ifBlank { null },
                email = current.email.trim().ifBlank { null },
                comment = current.comment.trim().ifBlank { null },
                currency = current.currency,
                avatarUrl = current.suggestedAvatarUrl ?: original.avatarUrl,
                dueDate = current.dueDate,
                reminderLeadDays = current.reminderLeadDays,
                updatedAt = Clock.System.now(),
                syncStatus = SyncStatus.PENDING,
            )
            runCatching { addOrUpdateCreditor(updated) }.onFailure {
                _state.update { it.copy(isSaving = false) }
                effectsChannel.send(AddEditContactEffect.Error(saveError))
                return@launch
            }
            finishSave()
            viewModelScope.launch { runCatching { linkCreditorToRegisteredUser(updated.id) } }
        }
    }

    private suspend fun finishSave() {
        _state.update { it.copy(isSaving = false) }
        if (appSettings.soundEnabled) soundPlayer.play(SoundEffect.ADD)
        effectsChannel.send(AddEditContactEffect.Saved)
    }
}
