package org.bigblackowl.debttracker.core.security

/**
 * Thin platform bridge to the OS restore-credential store.
 *
 * Android backs this with Credential Manager + Block Store (the Restore Credentials API); the
 * WebAuthn option/response JSON is passed straight through — the relying party is Supabase Auth
 * passkeys, orchestrated by [org.bigblackowl.debttracker.data.remote.RestoreCredentialCoordinator].
 * Every other platform binds [UnsupportedRestoreCredentialClient].
 */
interface RestoreCredentialClient {

    /** Whether the OS on this device exposes a restore-credential store at all. */
    val isSupported: Boolean

    /**
     * Create a system restore key from a `PublicKeyCredentialCreationOptionsJSON` string.
     * @return the registration-response JSON to hand back to the relying party.
     */
    suspend fun createRestoreKey(creationOptionsJson: String): String

    /**
     * Fetch an assertion for a `PublicKeyCredentialRequestOptionsJSON` string.
     * @return the authentication-response JSON, or `null` when this device holds no restore key.
     */
    suspend fun getRestoreAssertion(requestOptionsJson: String): String?

    /** Delete this device's restore key. */
    suspend fun clearRestoreKey()
}

/** Binding for platforms with no OS restore-credential store (iOS, Desktop, Web). */
object UnsupportedRestoreCredentialClient : RestoreCredentialClient {
    override val isSupported: Boolean = false
    override suspend fun createRestoreKey(creationOptionsJson: String): String =
        error("Restore credentials are not supported on this platform")
    override suspend fun getRestoreAssertion(requestOptionsJson: String): String? = null
    override suspend fun clearRestoreKey() = Unit
}
