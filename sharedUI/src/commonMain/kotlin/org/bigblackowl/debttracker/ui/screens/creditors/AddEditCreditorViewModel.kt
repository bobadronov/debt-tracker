package org.bigblackowl.debttracker.ui.screens.creditors

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ionspin.kotlin.bignum.decimal.BigDecimal
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
import org.bigblackowl.debttracker.domain.model.Creditor
import org.bigblackowl.debttracker.domain.model.CreditorTransaction
import org.bigblackowl.debttracker.domain.model.DebtStatus
import org.bigblackowl.debttracker.domain.model.MyDebtTransactionType
import org.bigblackowl.debttracker.domain.model.SyncStatus
import org.bigblackowl.debttracker.domain.usecase.FindProfileByEmailUseCase
import org.bigblackowl.debttracker.domain.usecase.creditor.AddCreditorTransactionUseCase
import org.bigblackowl.debttracker.domain.usecase.creditor.AddOrUpdateCreditorUseCase
import org.bigblackowl.debttracker.domain.usecase.creditor.DeleteCreditorUseCase
import org.bigblackowl.debttracker.domain.validation.isValidEmail
import org.bigblackowl.debttracker.domain.validation.isValidFullName

private const val EMAIL_LOOKUP_DEBOUNCE_MS = 500L

/**
 * Validates the form and saves a new [org.bigblackowl.debttracker.domain.model.Creditor] (plus its
 * opening transaction) on [Save][AddEditCreditorIntent.Save].
 * While the user types an [AddEditCreditorIntent.EmailChanged] email, debounces a lookup against
 * [findProfileByEmail] to offer a name/photo autofill suggestion if that email belongs to a
 * registered app user (§ProfileLookup) — purely additive, never blocks saving.
 */
@OptIn(ExperimentalUuidApi::class)
class AddEditCreditorViewModel(
    private val addOrUpdateCreditor: AddOrUpdateCreditorUseCase,
    private val addTransaction: AddCreditorTransactionUseCase,
    private val deleteCreditor: DeleteCreditorUseCase,
    private val appSettings: AppSettings,
    private val soundPlayer: SoundPlayer,
    private val findProfileByEmail: FindProfileByEmailUseCase,
) : ViewModel() {

    private var emailLookupJob: Job? = null

    private val _state = MutableStateFlow(AddEditCreditorState())
    val state: StateFlow<AddEditCreditorState> = _state.asStateFlow()

    private val effectsChannel = Channel<AddEditCreditorEffect>()
    val effects = effectsChannel.receiveAsFlow()

    fun onIntent(intent: AddEditCreditorIntent) {
        when (intent) {
            is AddEditCreditorIntent.FullNameChanged -> _state.update { it.copy(fullName = intent.value, fullNameError = null) }
            is AddEditCreditorIntent.PhoneChanged -> _state.update { it.copy(phone = intent.value) }
            is AddEditCreditorIntent.EmailChanged -> onEmailChanged(intent.value)
            is AddEditCreditorIntent.CommentChanged -> _state.update { it.copy(comment = intent.value) }
            is AddEditCreditorIntent.InitialAmountChanged -> _state.update { it.copy(initialAmountText = intent.value, amountError = null) }
            is AddEditCreditorIntent.CurrencyChanged -> _state.update { it.copy(currency = intent.value) }
            is AddEditCreditorIntent.MethodChanged -> _state.update { it.copy(method = intent.value) }
            is AddEditCreditorIntent.CardLastDigitsChanged -> _state.update { it.copy(cardLastDigits = intent.value) }
            AddEditCreditorIntent.ApplyProfileSuggestion -> applySuggestion()
            AddEditCreditorIntent.DismissProfileSuggestion -> _state.update { it.copy(profileSuggestion = null) }
            AddEditCreditorIntent.Save -> save()
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

        val parsedAmount = runCatching { BigDecimal.parseString(current.initialAmountText.trim()) }.getOrNull()
        if (parsedAmount == null || parsedAmount <= BigDecimal.ZERO) {
            _state.update { it.copy(amountError = strings.amountError) }
            return
        }

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
            )

            runCatching { addOrUpdateCreditor(creditor) }.onFailure {
                _state.update { it.copy(isSaving = false) }
                effectsChannel.send(AddEditCreditorEffect.Error(strings.saveError))
                return@launch
            }

            runCatching {
                addTransaction(
                    CreditorTransaction(
                        id = Uuid.random().toString(),
                        creditorId = creditor.id,
                        amount = parsedAmount.negate(),
                        type = MyDebtTransactionType.BORROW,
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
                // The creditor write already committed but its opening transaction didn't —
                // don't leave an orphaned 0-balance creditor behind; undo the half-finished save.
                runCatching { deleteCreditor(creditor.id) }
                _state.update { it.copy(isSaving = false) }
                effectsChannel.send(AddEditCreditorEffect.Error(strings.saveError))
                return@launch
            }

            _state.update { it.copy(isSaving = false) }
            if (appSettings.soundEnabled) soundPlayer.play(SoundEffect.ADD)
            effectsChannel.send(AddEditCreditorEffect.Saved)
        }
    }
}
