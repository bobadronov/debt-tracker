package org.bigblackowl.debttracker.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.bigblackowl.debttracker.BuildConfig
import org.bigblackowl.debttracker.core.i18n.LocalStrings
import org.bigblackowl.debttracker.core.platform.AppPlatform
import org.bigblackowl.debttracker.core.platform.currentPlatform
import org.bigblackowl.debttracker.core.settings.AppSettings
import org.bigblackowl.debttracker.theme.Dimens
import org.bigblackowl.debttracker.ui.components.PlaceholderScreen
import org.bigblackowl.debttracker.ui.components.SettingsRow
import org.bigblackowl.debttracker.ui.components.SettingsRowDivider
import org.bigblackowl.debttracker.ui.components.SettingsSection
import org.bigblackowl.debttracker.ui.components.SettingsSwitchRow
import org.koin.compose.koinInject

/**
 * Settings → Параметри (тема/мова/звук/віброзвінок/фонова робота) — виокремлено з колишнього
 * єдиного SettingsScreen. Прямі read/write поверх [AppSettings] без ViewModel — той клас сам
 * Compose-реактивний саме для цього.
 */
@Composable
fun SettingsPreferencesScreen(
    onBack: () -> Unit,
    onOpenLanguage: () -> Unit,
) {
    val settings = koinInject<AppSettings>()
    val strings = LocalStrings.current

    // Тільки Android/iOS мають реальний віброзвінок під керуванням LocalHapticFeedback —
    // на Desktop/Web це або no-op, або взагалі не підтримується, тож перемикач там ховаємо.
    val showHapticRow = currentPlatform == AppPlatform.ANDROID || currentPlatform == AppPlatform.IOS

    PlaceholderScreen(title = strings.settings.preferences, onBack = onBack) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier.width(Dimens.contentMaxWidth),
                verticalArrangement = Arrangement.spacedBy(Dimens.space24),
            ) {
                SettingsSection(strings.settings.preferences) {
                    if (BuildConfig.SOUND_ENABLED) {
                        SettingsSwitchRow(
                            icon = if (settings.soundEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                            title = strings.settings.sound,
                            checked = settings.soundEnabled,
                            onCheckedChange = { settings.soundEnabled = it },
                        )
                        SettingsRowDivider()
                    }

                    if (showHapticRow) {
                        SettingsSwitchRow(
                            icon = Icons.Filled.Vibration,
                            title = strings.settings.haptic,
                            checked = settings.hapticEnabled,
                            onCheckedChange = { settings.hapticEnabled = it },
                        )
                        SettingsRowDivider()
                    }

                    if (currentPlatform == AppPlatform.DESKTOP) {
                        SettingsSwitchRow(
                            icon = Icons.Filled.Sync,
                            title = strings.settings.runInBackground,
                            subtitle = strings.settings.runInBackgroundSubtitle,
                            checked = settings.runInBackground,
                            onCheckedChange = { settings.runInBackground = it },
                        )
                        SettingsRowDivider()
                    }

                    // Один тап по рядку циклічно перемикає system → light → dark — іконка відображає поточний стан.
                    val themeOptions = remember(strings) {
                        listOf(
                            "system" to strings.settings.themeSystem,
                            "light" to strings.settings.themeLight,
                            "dark" to strings.settings.themeDark,
                        )
                    }
                    val themeIndex = themeOptions.indexOfFirst { it.first == settings.theme }.coerceAtLeast(0)
                    SettingsRow(
                        icon = when (settings.theme) {
                            "light" -> Icons.Filled.LightMode
                            "dark" -> Icons.Filled.DarkMode
                            else -> Icons.Filled.BrightnessAuto
                        },
                        title = strings.settings.theme,
                        subtitle = themeOptions[themeIndex].second,
                        onClick = { settings.theme = themeOptions[(themeIndex + 1) % themeOptions.size].first },
                    )
                    SettingsRowDivider()

                    // Full screen instead of a dropdown — the option list (system/uk/en, more to come)
                    // doesn't fit a small menu well long-term. See LanguageScreen.
                    val languageOptions = remember(strings) { languageOptions(strings) }
                    val languageLabel = languageOptions.firstOrNull { it.value == settings.locale }?.label
                        ?: languageOptions.first().label
                    SettingsRow(
                        icon = Icons.Filled.Language,
                        title = strings.settings.language,
                        subtitle = languageLabel,
                        onClick = onOpenLanguage,
                    )
                }
            }
        }
    }
}
