package org.bigblackowl.debttracker.ui.screens.qr

import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.bigblackowl.debttracker.core.settings.AppSettings
import org.bigblackowl.debttracker.domain.model.ContactQrPayload
import org.bigblackowl.debttracker.domain.model.ScannedContact
import org.bigblackowl.debttracker.domain.repository.AuthRepository
import org.bigblackowl.debttracker.domain.validation.sanitizePhoneInput

private data class Card(val name: String, val phone: String, val email: String)
private data class Account(val isAuthenticated: Boolean, val name: String?, val phone: String?, val email: String?)

/**
 * Share: "my card" (name/phone/email) defaults to [AppSettings.myCardName]* if already saved, else
 * the signed-in [AuthRepository] profile if any. Signed-in users never see the fields — the card
 * comes straight from the account, nothing local to edit; signed-out users edit them on the
 * separate [EditContactCardScreen]. [AppSettings]' `myCard*` properties are Compose-reactive
 * (`mutableStateOf`-backed), so [snapshotFlow] here is what makes an edit made there show up back
 * on this screen without any explicit "reload" call — no ViewModel-to-ViewModel wiring needed.
 * Scan: lands here as the same [QrHubIntent.ScanResult] regardless of whether it came from the
 * camera or a picked gallery image (see [org.bigblackowl.debttracker.ui.components.contact.ContactQrScanOverlay],
 * which already decodes the raw payload before calling back). A valid scan pops the
 * debtor/creditor chooser via [QrHubState.scannedContact].
 */
class QrHubViewModel(
    appSettings: AppSettings,
    authRepository: AuthRepository,
) : ViewModel() {

    private val cardFlow: Flow<Card> = combine(
        snapshotFlow { appSettings.myCardName },
        snapshotFlow { appSettings.myCardPhone },
        snapshotFlow { appSettings.myCardEmail },
    ) { name, phone, email -> Card(name, phone, email) }

    private val accountFlow: Flow<Account> = combine(
        authRepository.isAuthenticated,
        authRepository.displayName,
        authRepository.phone,
        authRepository.email,
    ) { isAuthenticated, name, phone, email -> Account(isAuthenticated, name, phone, email) }

    private val effectsChannel = Channel<QrHubEffect>()
    val effects = effectsChannel.receiveAsFlow()

    private val _scannedContact = MutableStateFlow<ScannedContact?>(null)

    val state: StateFlow<QrHubState> = combine(cardFlow, accountFlow, _scannedContact) { card, account, scanned ->
        val name = card.name.ifBlank { account.name.orEmpty() }
        val phone = sanitizePhoneInput(card.phone.ifBlank { account.phone.orEmpty() })
        val email = card.email.ifBlank { account.email.orEmpty() }
        QrHubState(
            isAuthenticated = account.isAuthenticated,
            qrPayload = payloadFor(name, phone, email),
            scannedContact = scanned,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, QrHubState())

    fun onIntent(intent: QrHubIntent) {
        when (intent) {
            is QrHubIntent.ScanResult -> _scannedContact.value = intent.contact
            is QrHubIntent.ConfirmScannedContact -> confirmScannedContact(intent.asDebtor)
            QrHubIntent.DismissScannedContact -> _scannedContact.value = null
        }
    }

    private fun confirmScannedContact(asDebtor: Boolean) {
        val contact = _scannedContact.value ?: return
        _scannedContact.value = null
        viewModelScope.launch {
            effectsChannel.send(
                if (asDebtor) QrHubEffect.NavigateToAddDebtor(contact) else QrHubEffect.NavigateToAddCreditor(contact)
            )
        }
    }

    private fun payloadFor(name: String, phone: String, email: String): String? {
        if (name.isBlank()) return null
        return ContactQrPayload.encode(
            ScannedContact(fullName = name.trim(), phone = phone.trim().ifBlank { null }, email = email.trim().ifBlank { null })
        )
    }
}
