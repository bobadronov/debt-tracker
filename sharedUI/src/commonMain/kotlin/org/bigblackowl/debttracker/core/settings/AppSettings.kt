package org.bigblackowl.debttracker.core.settings

import androidx.compose.runtime.mutableStateOf
import com.russhwolf.settings.Settings
import kotlin.reflect.KProperty

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
    var soundEnabled: Boolean by SettingsBooleanState(settings, KEY_SOUND_ENABLED, true)
    var hapticEnabled: Boolean by SettingsBooleanState(settings, KEY_HAPTIC_ENABLED, true)
    var theme: String by SettingsStringState(settings, KEY_THEME, "system")
    /** "system" | "uk" | "en" — resolved to a [org.bigblackowl.debttracker.core.i18n.Strings] set via [org.bigblackowl.debttracker.core.i18n.resolveStrings]. */
    var locale: String by SettingsStringState(settings, KEY_LOCALE, "system")

    var pinCode: String?
        get() = settings.getStringOrNull(KEY_PIN_CODE)
        set(value) {
            if (value == null) settings.remove(KEY_PIN_CODE) else settings.putString(KEY_PIN_CODE, value)
        }

    private companion object {
        const val KEY_PROTECTION_ENABLED = "protection_enabled"
        const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
        const val KEY_PROTECTION_ONBOARDING_SEEN = "protection_onboarding_seen"
        const val KEY_PIN_CODE = "pin_code"
        const val KEY_SOUND_ENABLED = "sound_enabled"
        const val KEY_HAPTIC_ENABLED = "haptic_enabled"
        const val KEY_THEME = "theme"
        const val KEY_LOCALE = "locale"
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
