package org.bigblackowl.debttracker.data.remote

import io.github.aakira.napier.Napier
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.annotations.SupabaseExperimental
import io.github.jan.supabase.auth.auth
import org.bigblackowl.debttracker.BuildConfig
import org.bigblackowl.debttracker.core.security.RestoreCredentialClient
import org.bigblackowl.debttracker.core.settings.AppSettings
import org.bigblackowl.debttracker.domain.repository.RestoreCredentialGateway
import org.bigblackowl.debttracker.domain.repository.RestoreSessionResult

/**
 * [RestoreCredentialGateway] backed by the platform [RestoreCredentialClient] + Supabase Auth's
 * passkey relying party (`client.auth.passkeys`, `@SupabaseExperimental`).
 *
 * Registration: `startRegistration()` yields WebAuthn creation options → the OS mints a restore
 * key and returns the registration response → `verifyRegistration()` persists the public key.
 * Restore: `startAuthentication()` yields request options → the OS returns an assertion for this
 * device's restore key → `verifyAuthentication()` verifies it and installs the returned session.
 */
@OptIn(SupabaseExperimental::class)
class RestoreCredentialCoordinator(
    private val client: SupabaseClient,
    private val restoreClient: RestoreCredentialClient,
    private val appSettings: AppSettings,
) : RestoreCredentialGateway {

    override val isActive: Boolean
        get() = BuildConfig.RESTORE_CREDENTIALS_ENABLED && restoreClient.isSupported

    override suspend fun registerForCurrentSession() {
        if (!isActive || appSettings.restoreCredentialRegistered) return
        runCatching {
            val start = client.auth.passkeys.startRegistration()
            val registrationResponse = restoreClient.createRestoreKey(start.options.toString())
            client.auth.passkeys.verifyRegistration(start.challengeId, registrationResponse)
            appSettings.restoreCredentialRegistered = true
        }.onFailure {
            Napier.w(tag = TAG, throwable = it) { "Restore-key registration skipped" }
        }
    }

    override suspend fun tryRestoreSession(): RestoreSessionResult {
        if (!isActive) return RestoreSessionResult.UNSUPPORTED
        return runCatching {
            val start = client.auth.passkeys.startAuthentication()
            val assertion = restoreClient.getRestoreAssertion(start.options.toString())
                ?: return RestoreSessionResult.NO_CREDENTIAL
            // verifyAuthentication() installs the returned UserSession into the client itself.
            client.auth.passkeys.verifyAuthentication(start.challengeId, assertion)
            appSettings.restoreCredentialRegistered = true
            RestoreSessionResult.RESTORED
        }.getOrElse {
            Napier.w(tag = TAG, throwable = it) { "Silent session restore failed" }
            RestoreSessionResult.FAILED
        }
    }

    override suspend fun clear() {
        appSettings.restoreCredentialRegistered = false
        if (!restoreClient.isSupported) return
        runCatching { restoreClient.clearRestoreKey() }
            .onFailure { Napier.w(tag = TAG, throwable = it) { "Restore-key clear failed" } }
    }

    private companion object {
        const val TAG = "RestoreCredentialCoordinator"
    }
}
