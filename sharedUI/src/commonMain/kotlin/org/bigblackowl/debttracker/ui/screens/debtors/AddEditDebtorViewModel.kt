package org.bigblackowl.debttracker.ui.screens.debtors

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ionspin.kotlin.bignum.decimal.BigDecimal
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
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
import org.bigblackowl.debttracker.domain.model.Debtor
import org.bigblackowl.debttracker.domain.model.DebtStatus
import org.bigblackowl.debttracker.domain.model.DebtTransaction
import org.bigblackowl.debttracker.domain.model.SyncStatus
import org.bigblackowl.debttracker.domain.model.TransactionType
import org.bigblackowl.debttracker.domain.usecase.FindProfileByEmailUseCase
import org.bigblackowl.debttracker.domain.usecase.debtor.AddDebtTransactionUseCase
import org.bigblackowl.debttracker.domain.usecase.debtor.AddOrUpdateDebtorUseCase
import org.bigblackowl.debttracker.domain.usecase.debtor.DeleteDebtorUseCase
import org.bigblackowl.debttracker.domain.usecase.debtor.ObserveDebtorUseCase
import org.bigblackowl.debttracker.domain.validation.isValidEmail
import org.bigblackowl.debttracker.domain.validation.isValidFullName

private const val EMAIL_LOOKUP_DEBOUNCE_MS = 500L

/**
 * Loads the existing [org.bigblackowl.debttracker.domain.model.Debtor] when [debtorId] is
 * non-null, validates the form, and saves it (plus an optional initial transaction) on [Save][AddEditDebtorIntent.Save].
 * While the user types an [AddEditDebtorIntent.EmailChanged] email, debounces a lookup against
 * [findProfileByEmail] to offer a name/photo autofill suggestion if that email belongs to a
 * registered app user (§ProfileLookup) — purely additive, never blocks saving.
 */
@OptIn(ExperimentalUuidApi::class)
class AddEditDebtorViewModel(
    private val debtorId: String?,
    private val observeDebtor: ObserveDebtorUseCase,
    private val addOrUpdateDebtor: AddOrUpdateDebtorUseCase,
    private val addTransaction: AddDebtTransactionUseCase,
    private val deleteDebtor: DeleteDebtorUseCase,
    private val appSettings: AppSettings,
    private val soundPlayer: SoundPlayer,
    private val findProfileByEmail: FindProfileByEmailUseCase,
) : ViewModel() {

    private var loadedDebtor: Debtor? = null
    private var emailLookupJob: Job? = null

    private val _state = MutableStateFlow(
        AddEditDebtorState(isEditing = debtorId != null, isLoading = debtorId != null)
    )
    val state: StateFlow<AddEditDebtorState> = _state.asStateFlow()

    private val effectsChannel = Channel<AddEditDebtorEffect>()
    val effects = effectsChannel.receiveAsFlow()

    init {
        if (debtorId != null) {
            viewModelScope.launch {
                val existing = observeDebtor(debtorId).firstOrNull()
                loadedDebtor = existing
                _state.update {
                    it.copy(
                        isLoading = false,
                        fullName = existing?.fullName.orEmpty(),
                        phone = existing?.phone.orEmpty(),
                        email = existing?.email.orEmpty(),
                        comment = existing?.comment.orEmpty(),
                        currency = existing?.currency ?: it.currency,
                    )
                }
            }
        }
    }

    fun onIntent(intent: AddEditDebtorIntent) {
        when (intent) {
            is AddEditDebtorIntent.FullNameChanged -> _state.update { it.copy(fullName = intent.value, fullNameError = null) }
            is AddEditDebtorIntent.PhoneChanged -> _state.update { it.copy(phone = intent.value) }
            is AddEditDebtorIntent.EmailChanged -> onEmailChanged(intent.value)
            is AddEditDebtorIntent.CommentChanged -> _state.update { it.copy(comment = intent.value) }
            is AddEditDebtorIntent.InitialAmountChanged -> _state.update { it.copy(initialAmountText = intent.value, amountError = null) }
            is AddEditDebtorIntent.CurrencyChanged -> _state.update { it.copy(currency = intent.value) }
            is AddEditDebtorIntent.MethodChanged -> _state.update { it.copy(method = intent.value) }
            is AddEditDebtorIntent.CardLastDigitsChanged -> _state.update { it.copy(cardLastDigits = intent.value) }
            AddEditDebtorIntent.ApplyProfileSuggestion -> applySuggestion()
            AddEditDebtorIntent.DismissProfileSuggestion -> _state.update { it.copy(profileSuggestion = null) }
            AddEditDebtorIntent.Save -> save()
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

        var parsedAmount: BigDecimal? = null
        if (!current.isEditing) {
            val parsed = runCatching { BigDecimal.parseString(current.initialAmountText.trim()) }.getOrNull()
            if (parsed == null || parsed <= BigDecimal.ZERO) {
                _state.update { it.copy(amountError = strings.amountError) }
                return
            }
            parsedAmount = parsed
        }

        val isNew = !(current.isEditing && loadedDebtor != null)
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true) }
            val now = Clock.System.now()
            val debtor = if (current.isEditing && loadedDebtor != null) {
                loadedDebtor!!.copy(
                    fullName = current.fullName.trim(),
                    phone = current.phone.trim().ifBlank { null },
                    email = current.email.trim().ifBlank { null },
                    avatarUrl = current.suggestedAvatarUrl ?: loadedDebtor!!.avatarUrl,
                    comment = current.comment.trim().ifBlank { null },
                    updatedAt = now,
                )
            } else {
                Debtor(
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
                )
            }

            runCatching { addOrUpdateDebtor(debtor) }.onFailure {
                _state.update { it.copy(isSaving = false) }
                effectsChannel.send(AddEditDebtorEffect.Error(strings.saveError))
                return@launch
            }

            val amount = parsedAmount
            if (amount != null) {
                runCatching {
                    addTransaction(
                        DebtTransaction(
                            id = Uuid.random().toString(),
                            debtorId = debtor.id,
                            amount = amount.negate(),
                            type = TransactionType.LEND,
                            method = current.method,
                            cardLastDigits = current.cardLastDigits.trim().ifBlank { null },
                            date = now,
                            comment = null,
                            createdAt = now,
                            updatedAt = now,
                            syncStatus = SyncStatus.PENDING,
                        )
                    )
                }.onFailure {
                    // The debtor write already committed but its opening transaction didn't —
                    // don't leave an orphaned 0-balance debtor behind; undo the half-finished save.
                    if (isNew) runCatching { deleteDebtor(debtor.id) }
                    _state.update { it.copy(isSaving = false) }
                    effectsChannel.send(AddEditDebtorEffect.Error(strings.saveError))
                    return@launch
                }
            }

            _state.update { it.copy(isSaving = false) }
            if (appSettings.soundEnabled) soundPlayer.play(SoundEffect.ADD)
            effectsChannel.send(AddEditDebtorEffect.Saved)
        }
    }
}
