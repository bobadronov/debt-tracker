package org.bigblackowl.debttracker.core.security

import com.russhwolf.settings.Settings

/**
 * Encrypts/decrypts a single opaque string value, with no user-entered password — implemented
 * per-platform against whatever secure key store the OS offers (Android Keystore, an OS credential
 * store on Desktop). Backs [EncryptingSettings], which in turn backs
 * [io.github.jan.supabase.auth.SettingsSessionManager] (see [org.bigblackowl.debttracker.core.di.platformDataModule])
 * so the Supabase session (JWT + refresh token) never touches disk in plaintext.
 */
interface StringCipher {
    fun encrypt(plaintext: String): String
    fun decrypt(ciphertext: String): String
}

/**
 * [Settings] decorator that transparently runs every string value through [cipher]. Used on
 * Android/Desktop to back [io.github.jan.supabase.auth.SettingsSessionManager] — iOS uses
 * Keychain-backed `com.russhwolf.settings.KeychainSettings` directly instead, since Keychain items
 * are already OS-encrypted and don't need this extra layer.
 *
 * Only [putString]/[getString]/[getStringOrNull] are overridden — every other [Settings] member
 * (ints, booleans, [clear], [remove], ...) passes through unchanged via `by delegate`. Safe here
 * because [delegate] is always a dedicated store used only for the Supabase session, never shared
 * with plain (unencrypted) app settings.
 *
 * A decrypt failure (corrupt value, or the platform key became unavailable — e.g. Keystore state
 * lost) returns `null` rather than throwing, same as a missing key: [io.github.jan.supabase.auth.SessionManager.loadSessionOrNull]
 * already treats that as "no session", so the user is just prompted to sign in again instead of
 * the app crashing.
 */
class EncryptingSettings(
    private val delegate: Settings,
    private val cipher: StringCipher,
) : Settings by delegate {
    override fun putString(key: String, value: String) {
        delegate.putString(key, cipher.encrypt(value))
    }

    override fun getString(key: String, defaultValue: String): String =
        getStringOrNull(key) ?: defaultValue

    override fun getStringOrNull(key: String): String? =
        delegate.getStringOrNull(key)?.let { runCatching { cipher.decrypt(it) }.getOrNull() }
}
