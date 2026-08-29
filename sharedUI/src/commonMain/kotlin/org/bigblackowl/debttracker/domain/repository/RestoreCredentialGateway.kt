package org.bigblackowl.debttracker.domain.repository

/** Outcome of a silent, no-UI attempt to restore a session from an OS-managed restore credential. */
enum class RestoreSessionResult {
    /** A restore key existed and the session was re-established — the user is now signed in. */
    RESTORED,

    /** No restore key is present on this device (fresh install, never signed in, or key was cleared). */
    NO_CREDENTIAL,

    /** This build or platform can't use restore credentials — nothing was attempted. */
    UNSUPPORTED,

    /** A restore key was found but exchanging it for a session failed (network, expired, RP rejected). */
    FAILED,
}

/**
 * Zero-tap account restoration on a new device (2026 Play "secure device migration" standard).
 *
 * On Android this bridges the platform Restore Credentials API (Credential Manager + Block Store,
 * passkey-shaped) to Supabase Auth's passkey relying party: a successful password sign-in mints a
 * system restore key, which a later install on another device consumes before the auth gate to
 * sign in without a single tap.
 *
 * Inert ([isActive] == false) on iOS/Desktop/Web and whenever `BuildConfig.RESTORE_CREDENTIALS_ENABLED`
 * is off — every method is then a cheap no-op so callers never need their own platform checks.
 */
interface RestoreCredentialGateway {

    /** True only when this build + platform can actually create and consume restore credentials. */
    val isActive: Boolean

    /**
     * Called right after a password sign-in/sign-up succeeds: registers a restore key for the
     * current session so the next device can restore it. Best-effort — never throws, and does
     * nothing if a key was already registered for this install.
     */
    suspend fun registerForCurrentSession()

    /**
     * Called on a fresh device before the auth gate: silently exchanges this device's restore key
     * (if any) for a live Supabase session. Never throws.
     */
    suspend fun tryRestoreSession(): RestoreSessionResult

    /** Called on sign-out: deletes this device's restore key so re-entry needs a real sign-in. */
    suspend fun clear()
}
