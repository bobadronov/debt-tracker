package org.bigblackowl.debttracker.ui.screens.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Devices.DESKTOP
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import org.bigblackowl.debttracker.core.i18n.LocalStrings
import org.bigblackowl.debttracker.core.i18n.Strings
import org.bigblackowl.debttracker.core.settings.AppSettings
import org.bigblackowl.debttracker.preview.DebtTrackerPreview
import org.bigblackowl.debttracker.theme.Dimens
import org.bigblackowl.debttracker.ui.components.PlaceholderScreen
import org.bigblackowl.debttracker.ui.components.SettingsRow
import org.bigblackowl.debttracker.ui.components.SettingsRowDivider
import org.bigblackowl.debttracker.ui.components.SettingsSection
import org.koin.compose.koinInject

/**
 * Full-screen language picker reached from [SettingsScreen]'s Language row — pulled out of an
 * inline dropdown because the option list (system/uk/en, more to come) doesn't fit a segmented
 * row or a small menu well long-term. Picking an option applies it and returns to Settings.
 */
@Composable
fun LanguageScreen(onBack: () -> Unit) {
    val settings = koinInject<AppSettings>()
    val strings = LocalStrings.current
    val languageOptions = remember(strings) { languageOptions(strings) }

    PlaceholderScreen(title = strings.settingsLanguage, onBack = onBack) {
        Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            Column(modifier = Modifier.width(Dimens.contentMaxWidth)) {
                SettingsSection(strings.settingsLanguage) {
                    languageOptions.forEachIndexed { index, option ->
                        val (value, label, flag) = option
                        val selected = settings.locale == value
                        val containerColor by animateColorAsState(
                            if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                        )
                        val tintColor by animateColorAsState(
                            if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        SettingsRow(
                            icon = Icons.Filled.Language,
                            title = label,
                            iconContainerColor = containerColor,
                            iconTint = tintColor,
                            leadingContent = flag?.let { { Text(it, fontSize = 20.sp) } },
                            trailing = {
                                AnimatedVisibility(
                                    visible = selected,
                                    enter = fadeIn() + scaleIn(),
                                    exit = fadeOut() + scaleOut(),
                                ) {
                                    Icon(Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                }
                            },
                            onClick = {
                                settings.locale = value
                                onBack()
                            },
                        )
                        if (index != languageOptions.lastIndex) SettingsRowDivider()
                    }
                }
            }
        }
    }
}

/** One row in the language picker: the [AppSettings.locale] value, its display label, and an optional flag emoji. */
internal data class LanguageOption(val value: String, val label: String, val flag: String? = null)

/** Shared with [SettingsScreen]'s language row so both stay in sync as languages are added. */
internal fun languageOptions(strings: Strings) = listOf(
    LanguageOption("system", strings.settingsLanguageSystem),
    LanguageOption("uk", "Українська", flag = "🇺🇦"),
    LanguageOption("en", "English", flag = "🇬🇧"),
    LanguageOption("pl", "Polski", flag = "🇵🇱"),
)

@Preview
@Composable
private fun LanguageScreenLightPhonePreview() = DebtTrackerPreview(darkTheme = false) {
    LanguageScreen(onBack = {})
}

@Preview
@Composable
private fun LanguageScreenDarkPhonePreview() = DebtTrackerPreview(darkTheme = true) {
    LanguageScreen(onBack = {})
}

@Preview(device = DESKTOP)
@Composable
private fun LanguageScreenLightDesktopPreview() = DebtTrackerPreview(darkTheme = false) {
    LanguageScreen(onBack = {})
}

@Preview(device = DESKTOP)
@Composable
private fun LanguageScreenDarkDesktopPreview() = DebtTrackerPreview(darkTheme = true) {
    LanguageScreen(onBack = {})
}
