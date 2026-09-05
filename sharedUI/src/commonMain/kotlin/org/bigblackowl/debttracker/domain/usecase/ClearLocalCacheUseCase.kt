package org.bigblackowl.debttracker.domain.usecase

import org.bigblackowl.debttracker.core.settings.AppSettings
import org.bigblackowl.debttracker.domain.repository.CreditorRepository
import org.bigblackowl.debttracker.domain.repository.DebtorRepository

/**
 * Sign-out cleanup — wipes only this device's local cache, unlike [DeleteAllDataUseCase] which
 * really deletes Supabase data too. On Room platforms that's the offline cache (so the next
 * account signing in on this device doesn't see stale data); on Web there's no local cache, so
 * it's a no-op there and Supabase data survives sign-out as expected.
 *
 * Also drops the device's app-lock PIN and turns protection off: the PIN gates "whoever is signed
 * in here", so once nobody is, it must not survive to gate the next person's sign-in (or lock the
 * next person out — [AppSettings.verifyPinCode] would never match). Local settings only, so this
 * never reaches Supabase.
 *
 * Also wipes the "my contact card" fields (name/phone/email) — otherwise the next account signing
 * in on a shared device would see the previous person's info prefilled on the QR share screen.
 */
class ClearLocalCacheUseCase(
    private val debtorRepository: DebtorRepository,
    private val creditorRepository: CreditorRepository,
    private val appSettings: AppSettings,
) {
    suspend operator fun invoke() {
        debtorRepository.clearLocalCache()
        creditorRepository.clearLocalCache()
        appSettings.protectionEnabled = false
        appSettings.biometricEnabled = false
        appSettings.clearPinCode()
        appSettings.myCardName = ""
        appSettings.myCardPhone = ""
        appSettings.myCardEmail = ""
        appSettings.lastSyncedUserId = null
    }
}
