package org.bigblackowl.debttracker.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.tooling.preview.Devices.DESKTOP
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.SubcomposeAsyncImage
import org.bigblackowl.debttracker.BuildConfig
import org.bigblackowl.debttracker.core.i18n.LocalStrings
import org.bigblackowl.debttracker.core.platform.AppPlatform
import org.bigblackowl.debttracker.core.platform.currentPlatform
import org.bigblackowl.debttracker.core.settings.AppSettings
import org.bigblackowl.debttracker.domain.repository.AuthRepository
import org.bigblackowl.debttracker.domain.validation.formatUkrainianPhone
import org.bigblackowl.debttracker.preview.DebtTrackerPreview
import org.bigblackowl.debttracker.theme.Dimens
import org.bigblackowl.debttracker.ui.components.ConfirmDialog
import org.bigblackowl.debttracker.ui.components.PlaceholderScreen
import org.bigblackowl.debttracker.ui.components.SettingsRow
import org.bigblackowl.debttracker.ui.components.SettingsRowDivider
import org.bigblackowl.debttracker.ui.components.SettingsSection
import org.bigblackowl.debttracker.ui.components.SettingsSwitchRow
import org.bigblackowl.debttracker.ui.components.text.CaptionText
import org.bigblackowl.debttracker.ui.screens.settings.language.languageOptions
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

/**
 * SettingsScreen — a hub for Protection/Notifications/Data/About the app (separate screens), but
 * Preferences (theme/language/haptics/background work) stay right here, on the main page —
 * they're short enough not to justify yet another navigation hop.
 */
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenAuth: () -> Unit,
    onOpenAccountInfo: () -> Unit,
    onOpenProtection: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenLanguage: () -> Unit,
    onOpenData: () -> Unit,
    onOpenAbout: () -> Unit,
    viewModel: SettingsViewModel = koinViewModel(),
) {
    val settings = koinInject<AppSettings>()
    val authRepository = koinInject<AuthRepository>()
    val uriHandler = LocalUriHandler.current
    val isAuthenticated by authRepository.isAuthenticated.collectAsStateWithLifecycle()
    val avatarUrl by authRepository.avatarUrl.collectAsStateWithLifecycle()
    val accountEmail by authRepository.email.collectAsStateWithLifecycle()
    val accountName by authRepository.displayName.collectAsStateWithLifecycle()
    val accountPhone by authRepository.phone.collectAsStateWithLifecycle()

    SettingsContent(
        state = SettingsScreenState(
            isAuthenticated = isAuthenticated,
            avatarUrl = avatarUrl,
            accountEmail = accountEmail,
            accountName = accountName,
            accountPhone = accountPhone,
            notificationsEnabled = settings.notificationsEnabled,
            hapticEnabled = settings.hapticEnabled,
            runInBackground = settings.runInBackground,
            theme = settings.theme,
            locale = settings.locale,
        ),
        onBack = onBack,
        onOpenAuth = onOpenAuth,
        onOpenAccountInfo = onOpenAccountInfo,
        onOpenProtection = onOpenProtection,
        onOpenNotifications = onOpenNotifications,
        onOpenLanguage = onOpenLanguage,
        onOpenData = onOpenData,
        onOpenAbout = onOpenAbout,
        onSignOut = { viewModel.onIntent(SettingsIntent.SignOut) },
        onToggleHaptic = { settings.hapticEnabled = it },
        onToggleRunInBackground = { settings.runInBackground = it },
        onSetTheme = { settings.theme = it },
        onOpenUrl = { uriHandler.openUri(it) },
    )
}

/** UI-only bundling of the reactive values [SettingsContent] renders — not a real MVI state (this
 * screen has no dedicated ViewModel state; [SettingsViewModel] only carries the SignOut intent). */
private data class SettingsScreenState(
    val isAuthenticated: Boolean,
    val avatarUrl: String?,
    val accountEmail: String?,
    val accountName: String?,
    val accountPhone: String?,
    val notificationsEnabled: Boolean,
    val hapticEnabled: Boolean,
    val runInBackground: Boolean,
    val theme: String,
    val locale: String,
)

@Composable
private fun SettingsContent(
    state: SettingsScreenState,
    onBack: () -> Unit,
    onOpenAuth: () -> Unit,
    onOpenAccountInfo: () -> Unit,
    onOpenProtection: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenLanguage: () -> Unit,
    onOpenData: () -> Unit,
    onOpenAbout: () -> Unit,
    onSignOut: () -> Unit,
    onToggleHaptic: (Boolean) -> Unit,
    onToggleRunInBackground: (Boolean) -> Unit,
    onSetTheme: (String) -> Unit,
    onOpenUrl: (String) -> Unit,
) {
    val strings = LocalStrings.current
    var showSignOutConfirm by remember { mutableStateOf(false) }

    val showProtectionRow = currentPlatform != AppPlatform.WEB
    // Only Android/iOS have real haptics driven by LocalHapticFeedback —
    // on Desktop/Web it's either a no-op or unsupported entirely, so we hide the toggle there.
    val showHapticRow = currentPlatform == AppPlatform.ANDROID || currentPlatform == AppPlatform.IOS

    PlaceholderScreen(title = strings.settings.title, onBack = onBack) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier.width(Dimens.contentMaxWidth),
                verticalArrangement = Arrangement.spacedBy(Dimens.Spacing.xl),
            ) {
                AccountSection(
                    state = state,
                    onOpenAuth = onOpenAuth,
                    onOpenAccountInfo = onOpenAccountInfo,
                    onSignOut = { showSignOutConfirm = true },
                )
                SettingsSection(null) {
                    if (showProtectionRow) {
                        SettingsRow(
                            icon = Icons.Filled.Lock,
                            title = strings.settings.protection,
                            onClick = onOpenProtection,
                        )
                        SettingsRowDivider()
                    }
                    if (state.isAuthenticated) {
                        SettingsRow(
                            icon = if (state.notificationsEnabled) Icons.Filled.Notifications else Icons.Filled.NotificationsOff,
                            title = strings.settings.notifications,
                            onClick = onOpenNotifications,
                        )
                        SettingsRowDivider()
                    }
                    SettingsRow(
                        icon = Icons.Filled.Storage,
                        title = strings.settings.data,
                        onClick = onOpenData,
                    )
                    SettingsRowDivider()
                    SettingsRow(
                        icon = Icons.Filled.Info,
                        title = strings.settings.about,
                        subtitle = BuildConfig.APP_VERSION,
                        onClick = onOpenAbout,
                    )
                }
                SettingsSection(strings.settings.preferences) {
                    if (showHapticRow) {
                        SettingsSwitchRow(
                            icon = Icons.Filled.Vibration,
                            title = strings.settings.haptic,
                            checked = state.hapticEnabled,
                            onCheckedChange = onToggleHaptic,
                        )
                        SettingsRowDivider()
                    }

                    if (currentPlatform == AppPlatform.DESKTOP) {
                        SettingsSwitchRow(
                            icon = Icons.Filled.Sync,
                            title = strings.settings.runInBackground,
                            subtitle = strings.settings.runInBackgroundSubtitle,
                            checked = state.runInBackground,
                            onCheckedChange = onToggleRunInBackground,
                        )
                        SettingsRowDivider()
                    }

                    // One tap on the row cycles system → light → dark — the icon reflects the current state.
                    val themeOptions = remember(strings) {
                        listOf(
                            "system" to strings.settings.themeSystem,
                            "light" to strings.settings.themeLight,
                            "dark" to strings.settings.themeDark,
                        )
                    }
                    val themeIndex = themeOptions.indexOfFirst { it.first == state.theme }.coerceAtLeast(0)
                    SettingsRow(
                        icon = when (state.theme) {
                            "light" -> Icons.Filled.LightMode
                            "dark" -> Icons.Filled.DarkMode
                            else -> Icons.Filled.BrightnessAuto
                        },
                        title = strings.settings.theme,
                        subtitle = themeOptions[themeIndex].second,
                        onClick = { onSetTheme(themeOptions[(themeIndex + 1) % themeOptions.size].first) },
                    )
                    SettingsRowDivider()

                    // Full screen instead of a dropdown — the option list (system/uk/en, more to come)
                    // doesn't fit a small menu well long-term. See LanguageScreen.
                    val languageOptions = remember(strings) { languageOptions(strings) }
                    val languageLabel = languageOptions.firstOrNull { it.value == state.locale }?.label
                        ?: languageOptions.first().label
                    SettingsRow(
                        icon = Icons.Filled.Language,
                        title = strings.settings.language,
                        subtitle = languageLabel,
                        onClick = onOpenLanguage,
                    )
                }
                GetAppSection(onOpenUrl = onOpenUrl)
            }
        }
    }

    if (showSignOutConfirm) {
        ConfirmDialog(
            title = strings.settings.signOutConfirmTitle,
            text = strings.settings.signOutConfirmText,
            confirmLabel = strings.settings.signOut,
            onConfirm = {
                showSignOutConfirm = false
                onSignOut()
            },
            onDismiss = { showSignOutConfirm = false },
        )
    }
}

// --- Account ---
// Tapping the card opens the read-only account detail screen (avatar/name/email/phone plus
// Active devices); editing itself lives one step further, on EditAccountScreen.
@Composable
private fun AccountSection(
    state: SettingsScreenState,
    onOpenAuth: () -> Unit,
    onOpenAccountInfo: () -> Unit,
    onSignOut: () -> Unit,
) {
    val strings = LocalStrings.current

    SettingsSection(strings.settings.account) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .let { if (state.isAuthenticated) it.clickable(onClick = onOpenAccountInfo) else it }
                .padding(Dimens.Spacing.lg),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SubcomposeAsyncImage(
                model = state.avatarUrl,
                contentDescription = null,
                contentScale = ContentScale.Inside,
                modifier = Modifier.size(Dimens.IconSize.xxl).clip(CircleShape),
                loading = { CircularWavyProgressIndicator(modifier = Modifier.size(Dimens.IconSize.sm)) },
                error = {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant).padding(Dimens.Spacing.lg),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            )
            Spacer(Modifier.width(Dimens.Spacing.lg))
            Column(modifier = Modifier.weight(1f)) {
                CaptionText(
                    if (state.isAuthenticated) strings.settings.accountSynced(state.accountName ?: state.accountEmail.orEmpty()) else strings.settings.localOnly,
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (state.isAuthenticated) {
                    state.accountEmail?.takeIf { it.isNotBlank() }?.let { ContactLine(Icons.Filled.Email, it) }
                    formatUkrainianPhone(state.accountPhone)?.let { ContactLine(Icons.Filled.Phone, it) }
                }
            }
        }
        SettingsRowDivider()
        if (state.isAuthenticated) {
            SettingsRow(
                icon = Icons.AutoMirrored.Filled.Logout,
                title = strings.settings.signOut,
                onClick = onSignOut,
            )
        } else {
            SettingsRow(
                icon = Icons.AutoMirrored.Filled.Login,
                title = strings.settings.signIn,
                onClick = onOpenAuth,
            )
        }
    }
}

/** Small icon + text line under the account name (email, phone). */
@Composable
private fun ContactLine(icon: ImageVector, value: String) {
    Spacer(Modifier.height(Dimens.Spacing.xs))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(Dimens.IconSize.sm),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(Dimens.Spacing.xs))
        CaptionText(value)
    }
}

// --- Get the app ---
// Points to the other platforms' builds and the marketing site — hides only the platform the user
// is already running, since Desktop covers Windows/macOS/Linux as one link (currentPlatform can't
// tell them apart) and there's no iOS row yet (no App Store listing).
private const val WEBSITE_URL = "https://bobadronov.github.io/debt-tracker/"
private const val PLAY_STORE_URL = "https://play.google.com/store/apps/details?id=org.bigblackowl.debttracker.androidApp"
private const val RELEASES_URL = "https://github.com/bobadronov/debt-tracker/releases/latest"

@Composable
private fun GetAppSection(onOpenUrl: (String) -> Unit) {
    val strings = LocalStrings.current
    SettingsSection(strings.settings.getAppTitle) {
        if (currentPlatform != AppPlatform.WEB) {
            SettingsRow(
                icon = Icons.Filled.Public,
                title = strings.settings.getAppWebsite,
                subtitle = WEBSITE_URL,
                onClick = { onOpenUrl(WEBSITE_URL) },
            )
            SettingsRowDivider()
        }
        if (currentPlatform != AppPlatform.ANDROID) {
            SettingsRow(
                icon = Icons.Filled.Android,
                title = strings.settings.getAppAndroid,
                subtitle = "Google Play",
                onClick = { onOpenUrl(PLAY_STORE_URL) },
            )
        }
        if (currentPlatform != AppPlatform.DESKTOP) {
            if (currentPlatform != AppPlatform.ANDROID) SettingsRowDivider()
            SettingsRow(
                icon = Icons.Filled.Computer,
                title = strings.settings.getAppDesktop,
                subtitle = "Windows · macOS · Linux",
                onClick = { onOpenUrl(RELEASES_URL) },
            )
        }
    }
}

@Composable
private fun Preview(state: SettingsScreenState) = SettingsContent(
    state = state,
    onBack = {},
    onOpenAuth = {},
    onOpenAccountInfo = {},
    onOpenProtection = {},
    onOpenNotifications = {},
    onOpenLanguage = {},
    onOpenData = {},
    onOpenAbout = {},
    onSignOut = {},
    onToggleHaptic = {},
    onToggleRunInBackground = {},
    onSetTheme = {},
    onOpenUrl = {},
)

private val PREVIEW_STATE_SIGNED_IN = SettingsScreenState(
    isAuthenticated = true,
    avatarUrl = null,
    accountEmail = "taras@example.com",
    accountName = "Тарас Шевченко",
    accountPhone = "0501234567",
    notificationsEnabled = true,
    hapticEnabled = true,
    runInBackground = true,
    theme = "system",
    locale = "uk",
)

private val PREVIEW_STATE_SIGNED_OUT = SettingsScreenState(
    isAuthenticated = false,
    avatarUrl = null,
    accountEmail = null,
    accountName = null,
    accountPhone = null,
    notificationsEnabled = false,
    hapticEnabled = true,
    runInBackground = false,
    theme = "system",
    locale = "uk",
)

@Preview
@Composable
private fun SettingsScreenLightPhonePreview() = DebtTrackerPreview(darkTheme = false) { Preview(PREVIEW_STATE_SIGNED_IN) }

@Preview
@Composable
private fun SettingsScreenDarkPhonePreview() = DebtTrackerPreview(darkTheme = true) { Preview(PREVIEW_STATE_SIGNED_OUT) }

@Preview(device = DESKTOP)
@Composable
private fun SettingsScreenLightDesktopPreview() = DebtTrackerPreview(darkTheme = false) { Preview(PREVIEW_STATE_SIGNED_IN) }

@Preview(device = DESKTOP)
@Composable
private fun SettingsScreenDarkDesktopPreview() = DebtTrackerPreview(darkTheme = true) { Preview(PREVIEW_STATE_SIGNED_OUT) }
