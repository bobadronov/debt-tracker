package org.bigblackowl.debttracker.core.settings

import androidx.compose.runtime.mutableStateOf
import com.russhwolf.settings.Settings
import org.bigblackowl.debttracker.core.security.PinHasher
import org.bigblackowl.debttracker.core.security.hexToByteArray
import org.bigblackowl.debttracker.core.security.toHex
import kotlin.reflect.KProperty
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Налаштування користувача (спек §9.1 `profiles`, локальний аналог). Поля,
 * що впливають на UI (protectionEnabled/biometricEnabled/soundEnabled/theme),
 * зроблені Compose-реактивними через [mutableStateOf], щоб SettingsScreen міг
 * змінювати їх, а решта застосунку (AppTheme, AuthGate) одразу бачила зміну —
 * без окремого шару DataStore/Flow.
 */
class AppSettings(private val settings: Settings) {

    var protectionEnabled: Boolean by SettingsBooleanState(settings, KEY_PROTECTION_ENABLED, false)
    var biometricEnabled: Boolean by SettingsBooleanState(settings, KEY_BIOMETRIC_ENABLED, false)
    /** Whether the first-launch "enable protection" onboarding screen has already been shown. */
    var hasSeenProtectionOnboarding: Boolean by SettingsBooleanState(settings, KEY_PROTECTION_ONBOARDING_SEEN, false)
    /** Whether the first-launch "sign in for Account+Sync" onboarding screen has already been shown. */
    var hasSeenAccountOnboarding: Boolean by SettingsBooleanState(settings, KEY_ACCOUNT_ONBOARDING_SEEN, false)
    var soundEnabled: Boolean by SettingsBooleanState(settings, KEY_SOUND_ENABLED, true)
    var hapticEnabled: Boolean by SettingsBooleanState(settings, KEY_HAPTIC_ENABLED, true)
    /** User's own on/off switch for system notifications from [org.bigblackowl.debttracker.core.notifications.NotificationsPoller] (Settings → Preferences) — independent of, and gated behind, the OS-level permission. On by default. */
    var notificationsEnabled: Boolean by SettingsBooleanState(settings, KEY_NOTIFICATIONS_ENABLED, true)
    /** Redacts amounts/names in the OS notification body ([org.bigblackowl.debttracker.core.notifications.NotificationText.formatBody]) — the in-app notification history always shows full detail regardless. Off by default. */
    var hideAmountsInNotifications: Boolean by SettingsBooleanState(settings, KEY_HIDE_AMOUNTS_IN_NOTIFICATIONS, false)
    var theme: String by SettingsStringState(settings, KEY_THEME, "system")
    /** "system" | "uk" | "en" — resolved to a [org.bigblackowl.debttracker.core.i18n.Strings] set via [org.bigblackowl.debttracker.core.i18n.resolveStrings]. */
    var locale: String by SettingsStringState(settings, KEY_LOCALE, "system")
    /** Desktop-only: closing the window hides it to the system tray instead of quitting (desktopApp's `main.kt`). Off by default so closing the window keeps its usual meaning. */
    var runInBackground: Boolean by SettingsBooleanState(settings, KEY_RUN_IN_BACKGROUND, false)

    /** Locally-saved "my contact card" for the QR share screen (org.bigblackowl.debttracker.ui.screens.qr) —
     * default source when signed out, persisted edit-override when signed in. */
    var myCardName: String by SettingsStringState(settings, KEY_MY_CARD_NAME, "")
    var myCardPhone: String by SettingsStringState(settings, KEY_MY_CARD_PHONE, "")
    var myCardEmail: String by SettingsStringState(settings, KEY_MY_CARD_EMAIL, "")

    /** Whether a desktop app-lock PIN has been set up. */
    val hasPinCode: Boolean
        get() = settings.getStringOrNull(KEY_PIN_HASH) != null

    /** Stable per-install id for this device's row in `user_sessions` (Settings → Active devices) — generated once, then persisted. */
    @OptIn(ExperimentalUuidApi::class)
    val deviceSessionId: String by lazy {
        settings.getStringOrNull(KEY_DEVICE_SESSION_ID) ?: Uuid.random().toString().also {
            settings.putString(KEY_DEVICE_SESSION_ID, it)
        }
    }

    /**
     * ISO-8601 (`kotlin.time.Instant.toString()`) курсор для [org.bigblackowl.debttracker.core.notifications.NotificationsPoller] —
     * `created_at` останнього вже показаного системного сповіщення на цьому пристрої. `null` —
     * ще жодного не показано. Не Compose-реактивне (не UI-стан), тож звичайний get/set поверх Settings.
     */
    var lastSeenNotificationAt: String?
        get() = settings.getStringOrNull(KEY_LAST_SEEN_NOTIFICATION_AT)
        set(value) {
            if (value == null) settings.remove(KEY_LAST_SEEN_NOTIFICATION_AT)
            else settings.putString(KEY_LAST_SEEN_NOTIFICATION_AT, value)
        }

    /** Whether the app has already asked the OS for notification permission (Android 13+/iOS/Web) — asked once, not on every launch. */
    var notificationsPermissionRequested: Boolean by SettingsBooleanState(settings, KEY_NOTIFICATIONS_PERMISSION_REQUESTED, false)

    /**
     * Supabase `user_id` of whoever's data currently occupies the local Room cache — `null` means
     * the cache has never been synced to any account (pure local-only, or freshly cleared).
     * [org.bigblackowl.debttracker.data.sync.SyncCoordinator] uses this to tell "first sign-in from
     * local-only" (migrate the existing local rows into the new account, as onboarding promises)
     * apart from "signing into a DIFFERENT account on a device that still has a prior account's
     * cached data because it was never explicitly signed out of" (wipe the stale rows first, so
     * they don't render mixed into the new account's list or get pushed under the wrong user_id).
     * Not UI state.
     */
    var lastSyncedUserId: String?
        get() = settings.getStringOrNull(KEY_LAST_SYNCED_USER_ID)
        set(value) {
            if (value == null) settings.remove(KEY_LAST_SYNCED_USER_ID)
            else settings.putString(KEY_LAST_SYNCED_USER_ID, value)
        }

    /**
     * Останній вдалий зріз курсів валют, серіалізований [org.bigblackowl.debttracker.data.remote.HttpExchangeRatesRepository]
     * (одна JSON-мапа `джерело -> зріз`). `null` — ще жодного разу не вантажилось. Не UI-стан.
     */
    var exchangeRatesCache: String?
        get() = settings.getStringOrNull(KEY_EXCHANGE_RATES_CACHE)
        set(value) {
            if (value == null) settings.remove(KEY_EXCHANGE_RATES_CACHE)
            else settings.putString(KEY_EXCHANGE_RATES_CACHE, value)
        }

    /** Останнє обране джерело курсів ([org.bigblackowl.debttracker.domain.model.RateSource] name); `null` — дефолт. Не UI-стан. */
    var exchangeRatesSource: String?
        get() = settings.getStringOrNull(KEY_EXCHANGE_RATES_SOURCE)
        set(value) {
            if (value == null) settings.remove(KEY_EXCHANGE_RATES_SOURCE)
            else settings.putString(KEY_EXCHANGE_RATES_SOURCE, value)
        }

    /** Остання обрана базова валюта курсів (ISO-код) для джерел із довільною базою; `null` — дефолт. Не UI-стан. */
    var exchangeRatesBase: String?
        get() = settings.getStringOrNull(KEY_EXCHANGE_RATES_BASE)
        set(value) {
            if (value == null) settings.remove(KEY_EXCHANGE_RATES_BASE)
            else settings.putString(KEY_EXCHANGE_RATES_BASE, value)
        }

    /** Закріплені користувачем валюти на екрані курсів — CSV ISO-кодів. Порожній рядок — жодної. Не UI-стан. */
    var exchangeRatesPinnedCsv: String
        get() = settings.getString(KEY_EXCHANGE_RATES_PINNED, "")
        set(value) = settings.putString(KEY_EXCHANGE_RATES_PINNED, value)

    /**
     * Whether an OS restore credential (zero-tap sign-in) has already been registered for the
     * account signed in on this install — a local guard so [org.bigblackowl.debttracker.data.remote.RestoreCredentialCoordinator]
     * doesn't re-run the register round trip on every sign-in. Cleared on sign-out. Not UI state.
     */
    var restoreCredentialRegistered: Boolean
        get() = settings.getBoolean(KEY_RESTORE_CREDENTIAL_REGISTERED, false)
        set(value) = settings.putBoolean(KEY_RESTORE_CREDENTIAL_REGISTERED, value)

    /** Salts+hashes [pin] before persisting it (see [PinHasher]) — the raw PIN is never stored. */
    fun setPinCode(pin: String) {
        val salt = PinHasher.newSalt()
        settings.putString(KEY_PIN_SALT, salt.toHex())
        settings.putString(KEY_PIN_HASH, PinHasher.hash(pin, salt).toHex())
    }

    fun verifyPinCode(pin: String): Boolean {
        val saltHex = settings.getStringOrNull(KEY_PIN_SALT) ?: return false
        val hashHex = settings.getStringOrNull(KEY_PIN_HASH) ?: return false
        return PinHasher.matches(pin, saltHex.hexToByteArray(), hashHex.hexToByteArray())
    }

    /**
     * Wipes the app-lock PIN itself, not just the [protectionEnabled]/[biometricEnabled] flags —
     * called on sign-out ([org.bigblackowl.debttracker.domain.usecase.ClearLocalCacheUseCase]) so
     * the PIN this account's user chose can't outlive the account on this device: without this,
     * [hasPinCode] would still be true after sign-out, and the *next* person to sign in here would
     * either inherit the previous user's PIN gate or (if they also disable/re-enable protection)
     * have their new PIN silently coexist with the stale hash. Local-only — never touches Supabase.
     */
    fun clearPinCode() {
        settings.remove(KEY_PIN_SALT)
        settings.remove(KEY_PIN_HASH)
    }

    init {
        // One-time migration off the pre-hashing plaintext `pin_code` key.
        settings.getStringOrNull(KEY_PIN_CODE_LEGACY)?.let { legacyPin ->
            setPinCode(legacyPin)
            settings.remove(KEY_PIN_CODE_LEGACY)
        }
    }

    private companion object {
        const val KEY_PROTECTION_ENABLED = "protection_enabled"
        const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
        const val KEY_PROTECTION_ONBOARDING_SEEN = "protection_onboarding_seen"
        const val KEY_ACCOUNT_ONBOARDING_SEEN = "account_onboarding_seen"
        const val KEY_PIN_CODE_LEGACY = "pin_code"
        const val KEY_PIN_SALT = "pin_salt"
        const val KEY_PIN_HASH = "pin_hash"
        const val KEY_SOUND_ENABLED = "sound_enabled"
        const val KEY_HAPTIC_ENABLED = "haptic_enabled"
        const val KEY_NOTIFICATIONS_ENABLED = "notifications_enabled"
        const val KEY_HIDE_AMOUNTS_IN_NOTIFICATIONS = "hide_amounts_in_notifications"
        const val KEY_THEME = "theme"
        const val KEY_LOCALE = "locale"
        const val KEY_DEVICE_SESSION_ID = "device_session_id"
        const val KEY_RUN_IN_BACKGROUND = "run_in_background"
        const val KEY_MY_CARD_NAME = "my_card_name"
        const val KEY_MY_CARD_PHONE = "my_card_phone"
        const val KEY_MY_CARD_EMAIL = "my_card_email"
        const val KEY_LAST_SEEN_NOTIFICATION_AT = "last_seen_notification_at"
        const val KEY_NOTIFICATIONS_PERMISSION_REQUESTED = "notifications_permission_requested"
        const val KEY_LAST_SYNCED_USER_ID = "last_synced_user_id"
        const val KEY_RESTORE_CREDENTIAL_REGISTERED = "restore_credential_registered"
        const val KEY_EXCHANGE_RATES_CACHE = "exchange_rates_cache"
        const val KEY_EXCHANGE_RATES_SOURCE = "exchange_rates_source"
        const val KEY_EXCHANGE_RATES_BASE = "exchange_rates_base"
        const val KEY_EXCHANGE_RATES_PINNED = "exchange_rates_pinned"
    }
}

private class SettingsBooleanState(
    private val settings: Settings,
    private val key: String,
    default: Boolean,
) {
    private val state = mutableStateOf(settings.getBoolean(key, default))
    operator fun getValue(thisRef: Any?, property: KProperty<*>): Boolean = state.value
    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: Boolean) {
        state.value = value
        settings.putBoolean(key, value)
    }
}

private class SettingsStringState(
    private val settings: Settings,
    private val key: String,
    default: String,
) {
    private val state = mutableStateOf(settings.getString(key, default))
    operator fun getValue(thisRef: Any?, property: KProperty<*>): String = state.value
    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: String) {
        state.value = value
        settings.putString(key, value)
    }
}
