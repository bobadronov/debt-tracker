package org.bigblackowl.debttracker.ui.screens.qr

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.bigblackowl.debttracker.core.settings.AppSettings
import org.bigblackowl.debttracker.domain.model.ContactQrPayload
import org.bigblackowl.debttracker.domain.model.ScannedContact
import org.bigblackowl.debttracker.domain.repository.AuthRepository
import org.bigblackowl.debttracker.domain.validation.sanitizePhoneInput

/**
 * Share: "my card" (name/phone/email) defaults to [AppSettings.myCard]* if already saved, else
 * the signed-in [AuthRepository] profile if any. Signed-in users never see the fields — the card
 * comes straight from the account, nothing local to edit; signed-out users get an Edit button
 * ([QrHubState.fieldsExpanded]) that reveals them, and every edit persists immediately (same
 * instant-persist UX as other [AppSettings] fields).
 * Scan: camera on Android/iOS, a local-file picker on Desktop/Web (see [QrHubScreen],
 * QR_SCAN_CAPABLE_PLATFORMS) — either way it lands here as the same raw [QrHubIntent.ScanResult].
 * [ContactQrPayload.decode] rejects anything that isn't a Debt Tracker contact card silently
 * (foreign QR codes are simply not recognized, no error shown); a valid decode pops the
 * debtor/creditor chooser via [QrHubState.scannedContact].
 */
class QrHubViewModel(
    private val appSettings: AppSettings,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(QrHubState())
    val state: StateFlow<QrHubState> = _state.asStateFlow()

    private val effectsChannel = Channel<QrHubEffect>()
    val effects = effectsChannel.receiveAsFlow()

    init {
        val isAuthenticated = authRepository.isAuthenticated.value
        val name = appSettings.myCardName.ifBlank { authRepository.displayName.value.orEmpty() }
        val phone = sanitizePhoneInput(appSettings.myCardPhone.ifBlank { authRepository.phone.value.orEmpty() })
        val email = appSettings.myCardEmail.ifBlank { authRepository.email.value.orEmpty() }
        _state.update {
            it.copy(
                isAuthenticated = isAuthenticated,
                myName = name,
                myPhone = phone,
                myEmail = email,
                qrPayload = payloadFor(name, phone, email),
            )
        }
    }

    fun onIntent(intent: QrHubIntent) {
        when (intent) {
            QrHubIntent.SwitchToScan -> _state.update { it.copy(mode = QrHubMode.SCAN, cameraPermissionDenied = false) }
            QrHubIntent.SwitchToShare -> _state.update { it.copy(mode = QrHubMode.SHARE, cameraPermissionDenied = false) }
            QrHubIntent.EditClicked -> _state.update { it.copy(fieldsExpanded = !it.fieldsExpanded) }
            is QrHubIntent.MyNameChanged -> updateMyCard(name = intent.value)
            is QrHubIntent.MyPhoneChanged -> updateMyCard(phone = intent.value)
            is QrHubIntent.MyEmailChanged -> updateMyCard(email = intent.value)
            is QrHubIntent.ScanResult -> onScanResult(intent.rawPayload)
            QrHubIntent.CameraPermissionDenied -> _state.update { it.copy(cameraPermissionDenied = true) }
            is QrHubIntent.ConfirmScannedContact -> confirmScannedContact(intent.asDebtor)
            QrHubIntent.DismissScannedContact -> _state.update { it.copy(scannedContact = null) }
        }
    }

    private fun updateMyCard(
        name: String = _state.value.myName,
        phone: String = _state.value.myPhone,
        email: String = _state.value.myEmail,
    ) {
        appSettings.myCardName = name
        appSettings.myCardPhone = phone
        appSettings.myCardEmail = email
        _state.update { it.copy(myName = name, myPhone = phone, myEmail = email, qrPayload = payloadFor(name, phone, email)) }
    }

    private fun onScanResult(rawPayload: String) {
        val contact = ContactQrPayload.decode(rawPayload) ?: return
        _state.update { it.copy(scannedContact = contact) }
    }

    private fun confirmScannedContact(asDebtor: Boolean) {
        val contact = _state.value.scannedContact ?: return
        _state.update { it.copy(scannedContact = null) }
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
