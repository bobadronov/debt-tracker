package org.bigblackowl.debttracker.ui.screens.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Feedback
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Password
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.tooling.preview.Devices.DESKTOP
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.bigblackowl.debttracker.BuildConfig
import org.bigblackowl.debttracker.core.i18n.LocalStrings
import org.bigblackowl.debttracker.core.i18n.resolveFeedbackStrings
import org.bigblackowl.debttracker.core.notifications.NotificationPermissionRequester
import org.bigblackowl.debttracker.core.notifications.rememberNotificationPermissionRequester
import org.bigblackowl.debttracker.core.platform.AppPlatform
import org.bigblackowl.debttracker.core.platform.currentPlatform
import org.bigblackowl.debttracker.core.security.BiometricAuthenticator
import org.bigblackowl.debttracker.core.security.rememberBiometricAuthenticator
import org.bigblackowl.debttracker.core.settings.AppSettings
import org.bigblackowl.debttracker.core.update.InAppUpdateStatus
import org.bigblackowl.debttracker.core.update.appUpdateSupported
import org.bigblackowl.debttracker.core.update.inAppUpdateSupported
import org.bigblackowl.debttracker.core.update.rememberAppUpdateChecker
import org.bigblackowl.debttracker.core.update.rememberInAppUpdateLauncher
import org.bigblackowl.debttracker.domain.repository.AuthRepository
import org.bigblackowl.debttracker.preview.DebtTrackerPreview
import org.bigblackowl.debttracker.theme.Dimens
import org.bigblackowl.debttracker.theme.debtAccentColors
import org.bigblackowl.debttracker.ui.components.AccountAvatar
import org.bigblackowl.debttracker.ui.components.ConfirmDialog
import org.bigblackowl.debttracker.ui.components.PinSetupDialog
import org.bigblackowl.debttracker.ui.components.PlaceholderScreen
import org.bigblackowl.debttracker.ui.components.SettingsRow
import org.bigblackowl.debttracker.ui.components.SettingsRowDivider
import org.bigblackowl.debttracker.ui.components.SettingsSection
import org.bigblackowl.debttracker.ui.components.SettingsSwitchRow
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

/**
 * SettingsScreen: захист/біометрія (спек §6, п.7), тема, звук, експорт,
 * видалення всіх даних (подвійне підтвердження, спек §9.2), Local-only/
 * Account+Sync (спек §1.1, Фаза 6). All async/business logic lives in [SettingsViewModel] —
 * this screen creates the composition-scoped platform objects (biometric authenticator, update
 * checker/launcher — these need a live Activity/registration and can't be constructor-injected)
 * and forwards them into intents; simple synchronous [AppSettings] toggles (sound/haptic/theme/
 * language) stay direct reads/writes here, matching that class's own Compose-reactive design.
 *
 * The four groups are split into their own composables so each collects only the flows it renders:
 * the update-status flows tick rapidly during a check/download, and would otherwise recompose the
 * whole screen (avatar, preference list and all) on every emission.
 */
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onExport: () -> Unit,
    onOpenAuth: () -> Unit,
    onOpenAccountInfo: () -> Unit,
    onOpenLanguage: () -> Unit,
    viewModel: SettingsViewModel = koinViewModel(),
) {
    val settings = koinInject<AppSettings>()
    val authRepository = koinInject<AuthRepository>()
    val biometricAuthenticator = rememberBiometricAuthenticator()
    val notificationPermissionRequester = rememberNotificationPermissionRequester()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val strings = LocalStrings.current

    var showPinSetupDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm1 by remember { mutableStateOf(false) }
    var showDeleteConfirm2 by remember { mutableStateOf(false) }
    var showSignOutConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.onIntent(SettingsIntent.CheckBiometricHardware(biometricAuthenticator))
    }

    PlaceholderScreen(title = strings.settingsTitle, onBack = onBack) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier.width(Dimens.contentMaxWidth),
                verticalArrangement = Arrangement.spacedBy(Dimens.space24),
            ) {
                AccountSection(
                    authRepository = authRepository,
                    onOpenAuth = onOpenAuth,
                    onOpenAccountInfo = onOpenAccountInfo,
                    onSignOut = { showSignOutConfirm = true },
                )
                PreferencesSection(
                    settings = settings,
                    authRepository = authRepository,
                    biometricHardwareAvailable = state.biometricHardwareAvailable,
                    protectionConfirmError = state.protectionConfirmError,
                    notificationsPermissionBlocked = state.notificationsPermissionBlocked,
                    biometricAuthenticator = biometricAuthenticator,
                    notificationPermissionRequester = notificationPermissionRequester,
                    onIntent = viewModel::onIntent,
                    onRequestPinSetup = { showPinSetupDialog = true },
                    onOpenLanguage = onOpenLanguage,
                )
                DataSection(
                    deleteDone = state.deleteDone,
                    deleteError = state.deleteError,
                    onExport = onExport,
                    onRequestDelete = { showDeleteConfirm1 = true },
                )
                AboutSection(
                    updateState = state.updateState,
                    onIntent = viewModel::onIntent,
                )
            }
        }
    }

    if (showPinSetupDialog) {
        PinSetupDialog(
            onDismiss = { showPinSetupDialog = false },
            onConfirm = { pin ->
                showPinSetupDialog = false
                viewModel.onIntent(SettingsIntent.SetupPinAndEnableProtection(pin))
            },
        )
    }

    if (showSignOutConfirm) {
        ConfirmDialog(
            title = strings.settingsSignOutConfirmTitle,
            text = strings.settingsSignOutConfirmText,
            confirmLabel = strings.settingsSignOut,
            onConfirm = {
                showSignOutConfirm = false
                viewModel.onIntent(SettingsIntent.SignOut)
            },
            onDismiss = { showSignOutConfirm = false },
        )
    }

    if (showDeleteConfirm1) {
        ConfirmDialog(
            title = strings.settingsDeleteConfirm1Title,
            text = strings.settingsDeleteConfirm1Text,
            confirmLabel = strings.continueLabel,
            onConfirm = {
                showDeleteConfirm1 = false; showDeleteConfirm2 = true
            },
            onDismiss = { showDeleteConfirm1 = false },
        )
    }

    if (showDeleteConfirm2) {
        ConfirmDialog(
            title = strings.settingsDeleteConfirm2Title,
            text = strings.settingsDeleteConfirm2Text,
            confirmLabel = strings.deleteForever,
            onConfirm = {
                showDeleteConfirm2 = false
                viewModel.onIntent(SettingsIntent.DeleteAllData)
            },
            onDismiss = { showDeleteConfirm2 = false },
        )
    }
}

// --- Account ---
// Tapping the card opens the read-only account detail screen (avatar/name/email/phone plus
// Active devices); editing itself lives one step further, on EditAccountScreen.
@Composable
private fun AccountSection(
    authRepository: AuthRepository,
    onOpenAuth: () -> Unit,
    onOpenAccountInfo: () -> Unit,
    onSignOut: () -> Unit,
) {
    val strings = LocalStrings.current
    val isAuthenticated by authRepository.isAuthenticated.collectAsStateWithLifecycle()
    val avatarUrl by authRepository.avatarUrl.collectAsStateWithLifecycle()
    val accountEmail by authRepository.email.collectAsStateWithLifecycle()
    val accountName by authRepository.displayName.collectAsStateWithLifecycle()
    val accountPhone by authRepository.phone.collectAsStateWithLifecycle()

    SettingsSection(strings.settingsAccount) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .let { if (isAuthenticated) it.clickable(onClick = onOpenAccountInfo) else it }
                .padding(Dimens.space16),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AccountAvatar(
                avatarUrl = avatarUrl,
                isUploading = false,
                onEditClick = onOpenAccountInfo,
            )
            Spacer(Modifier.width(Dimens.space16))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    if (isAuthenticated) strings.settingsAccountSynced(accountName ?: accountEmail.orEmpty()) else strings.settingsLocalOnly,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (isAuthenticated) {
                    accountEmail?.takeIf { it.isNotBlank() }?.let { ContactLine(Icons.Filled.Email, it) }
                    accountPhone?.takeIf { it.isNotBlank() }?.let { ContactLine(Icons.Filled.Phone, it) }
                }
            }
        }
        SettingsRowDivider()
        if (isAuthenticated) {
            SettingsRow(
                icon = Icons.AutoMirrored.Filled.Logout,
                title = strings.settingsSignOut,
                onClick = onSignOut,
            )
        } else {
            SettingsRow(
                icon = Icons.AutoMirrored.Filled.Login,
                title = strings.settingsSignIn,
                onClick = onOpenAuth,
            )
        }
    }
}

/** Small icon + text line under the account name (email, phone). */
@Composable
private fun ContactLine(icon: ImageVector, value: String) {
    Spacer(Modifier.height(Dimens.space4))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(Dimens.space16),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(Dimens.space4))
        Text(
            value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// --- Preferences ---
@Composable
private fun PreferencesSection(
    settings: AppSettings,
    authRepository: AuthRepository,
    biometricHardwareAvailable: Boolean,
    protectionConfirmError: String?,
    notificationsPermissionBlocked: Boolean,
    biometricAuthenticator: BiometricAuthenticator,
    notificationPermissionRequester: NotificationPermissionRequester,
    onIntent: (SettingsIntent) -> Unit,
    onRequestPinSetup: () -> Unit,
    onOpenLanguage: () -> Unit,
) {
    val strings = LocalStrings.current

    // Сповіщення (§7) стосуються лише дзеркальних боргів у Supabase — без входу в акаунт
    // їх нема звідки отримувати, тож перемикач показуємо тільки залогіненим.
    val isAuthenticated by authRepository.isAuthenticated.collectAsStateWithLifecycle()

    // Web: захисту немає взагалі — вхід уже вимагає email/пароль (Account+Sync-only на Web).
    val showProtectionRow = currentPlatform != AppPlatform.WEB
    // Мобільні платформи без біометричного заліза (або незареєстрованою біометрією — типово
    // для планшетів) падають на той самий PIN-механізм, що й Desktop, замість ховати перемикач.
    val usesPinProtection = currentPlatform == AppPlatform.DESKTOP || !biometricHardwareAvailable
    val protectionIcon = if (usesPinProtection) Icons.Filled.Password else Icons.Filled.Fingerprint

    // Тільки Android/iOS мають реальний віброзвінок під керуванням LocalHapticFeedback —
    // на Desktop/Web це або no-op, або взагалі не підтримується, тож перемикач там ховаємо.
    // Only Android/iOS have a real vibration motor behind LocalHapticFeedback — on Desktop/Web
    // it's either a no-op or unsupported, so the toggle is hidden there.
    val showHapticRow = currentPlatform == AppPlatform.ANDROID || currentPlatform == AppPlatform.IOS

    SettingsSection(strings.settingsPreferences) {
        if (showProtectionRow) {
            SettingsRow(
                icon = protectionIcon,
                title = strings.settingsProtection,
                subtitle = protectionConfirmError,
                trailing = {
                    Switch(
                        checked = settings.protectionEnabled,
                        onCheckedChange = { checked ->
                            when {
                                usesPinProtection && checked && !settings.hasPinCode -> onRequestPinSetup()
                                usesPinProtection -> onIntent(SettingsIntent.TogglePinProtection(checked))

                                // Мобільні платформи з біометрією: увімкнення захисту потребує
                                // підтвердження відбитком/обличчям одразу — інакше можна ввімкнути
                                // перемикач, маючи чужий палець на сканері, і сам захист виявиться фікцією.
                                checked -> onIntent(SettingsIntent.EnableMobileProtection(biometricAuthenticator))
                                else -> onIntent(SettingsIntent.DisableMobileProtection)
                            }
                        },
                    )
                },
            )
            SettingsRowDivider()
        }

        if (isAuthenticated) {
            SettingsSwitchRow(
                icon = if (settings.notificationsEnabled) Icons.Filled.Notifications else Icons.Filled.NotificationsOff,
                title = strings.settingsNotifications,
                subtitle = if (settings.notificationsEnabled && notificationsPermissionBlocked) strings.settingsNotificationsBlocked else null,
                checked = settings.notificationsEnabled,
                onCheckedChange = { onIntent(SettingsIntent.ToggleNotifications(it, notificationPermissionRequester)) },
            )
            SettingsRowDivider()
        }

        if (BuildConfig.SOUND_ENABLED) {
            SettingsSwitchRow(
                icon = if (settings.soundEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                title = strings.settingsSound,
                checked = settings.soundEnabled,
                onCheckedChange = { settings.soundEnabled = it },
            )
            SettingsRowDivider()
        }

        if (showHapticRow) {
            SettingsSwitchRow(
                icon = Icons.Filled.Vibration,
                title = strings.settingsHaptic,
                checked = settings.hapticEnabled,
                onCheckedChange = { settings.hapticEnabled = it },
            )
            SettingsRowDivider()
        }

        if (currentPlatform == AppPlatform.DESKTOP) {
            SettingsSwitchRow(
                icon = Icons.Filled.Sync,
                title = strings.settingsRunInBackground,
                subtitle = strings.settingsRunInBackgroundSubtitle,
                checked = settings.runInBackground,
                onCheckedChange = { settings.runInBackground = it },
            )
            SettingsRowDivider()
        }

        // Один тап по рядку циклічно перемикає system → light → dark — іконка відображає поточний стан.
        val themeOptions = remember(strings) {
            listOf(
                "system" to strings.settingsThemeSystem,
                "light" to strings.settingsThemeLight,
                "dark" to strings.settingsThemeDark,
            )
        }
        val themeIndex = themeOptions.indexOfFirst { it.first == settings.theme }.coerceAtLeast(0)
        SettingsRow(
            icon = when (settings.theme) {
                "light" -> Icons.Filled.LightMode
                "dark" -> Icons.Filled.DarkMode
                else -> Icons.Filled.BrightnessAuto
            },
            title = strings.settingsTheme,
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
            title = strings.settingsLanguage,
            subtitle = languageLabel,
            onClick = onOpenLanguage,
        )
    }
}

// --- Data ---
@Composable
private fun DataSection(
    deleteDone: Boolean,
    deleteError: Boolean,
    onExport: () -> Unit,
    onRequestDelete: () -> Unit,
) {
    val strings = LocalStrings.current
    Column(verticalArrangement = Arrangement.spacedBy(Dimens.space8)) {
        SettingsSection(strings.settingsData) {
            SettingsRow(
                icon = Icons.Filled.Download,
                title = strings.settingsExportData,
                onClick = onExport,
            )
            SettingsRowDivider()
            SettingsRow(
                icon = Icons.Filled.DeleteForever,
                title = strings.settingsDeleteAllData,
                titleColor = MaterialTheme.debtAccentColors.debt,
                iconTint = MaterialTheme.debtAccentColors.debt,
                iconContainerColor = MaterialTheme.debtAccentColors.debt.copy(alpha = 0.12f),
                onClick = onRequestDelete,
            )
        }
        DataResultLine(
            visible = deleteDone,
            text = strings.settingsDeleteAllDataDone,
            color = MaterialTheme.debtAccentColors.repay,
        )
        DataResultLine(
            visible = deleteError,
            text = strings.settingsDeleteAllDataFailed,
            color = MaterialTheme.debtAccentColors.debt,
        )
    }
}

@Composable
private fun ColumnScope.DataResultLine(visible: Boolean, text: String, color: Color) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
    ) {
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = color,
            modifier = Modifier.padding(start = Dimens.space8),
        )
    }
}

// --- About ---
@Composable
private fun AboutSection(
    updateState: UpdateCheckState,
    onIntent: (SettingsIntent) -> Unit,
) {
    val strings = LocalStrings.current
    val settings = koinInject<AppSettings>()
    val uriHandler = LocalUriHandler.current
    val updateChecker = rememberAppUpdateChecker()
    val inAppUpdateLauncher = rememberInAppUpdateLauncher()
    val inAppUpdateReady by inAppUpdateLauncher.updateReadyToInstall.collectAsStateWithLifecycle()
    val inAppUpdateStatus by inAppUpdateLauncher.updateStatus.collectAsStateWithLifecycle()

    SettingsSection(strings.settingsAbout) {
        val versionLine = "${BuildConfig.APP_VERSION}-${BuildConfig.APP_VERSION_CODE}"
        val versionSubtitle = if (currentPlatform == AppPlatform.ANDROID) {
            when {
                inAppUpdateReady -> strings.updateReadyToInstall
                inAppUpdateStatus == InAppUpdateStatus.Checking -> "$versionLine · ${strings.settingsCheckingForUpdates}"
                inAppUpdateStatus == InAppUpdateStatus.UpToDate -> "$versionLine · ${strings.settingsUpToDate}"
                inAppUpdateStatus == InAppUpdateStatus.CheckFailed -> "$versionLine · ${strings.updateFailed}"
                inAppUpdateStatus == InAppUpdateStatus.Downloading -> strings.updateDownloading
                inAppUpdateStatus == InAppUpdateStatus.DownloadFailed -> strings.updateFailed
                else -> versionLine
            }
        } else {
            when (val s = updateState) {
                UpdateCheckState.Idle -> versionLine
                UpdateCheckState.Checking -> "$versionLine · ${strings.settingsCheckingForUpdates}"
                UpdateCheckState.UpToDate -> "$versionLine · ${strings.settingsUpToDate}"
                UpdateCheckState.CheckFailed -> "$versionLine · ${strings.updateFailed}"
                is UpdateCheckState.Available -> strings.updateAvailableMessage(s.info.version)
                is UpdateCheckState.Downloading -> strings.updateDownloading
                is UpdateCheckState.Failed -> strings.updateFailed
            }
        }
        SettingsRow(
            icon = Icons.Filled.Info,
            title = strings.settingsAboutVersion,
            subtitle = versionSubtitle,
            trailing = if (currentPlatform == AppPlatform.ANDROID && inAppUpdateSupported) {
                {
                    when {
                        inAppUpdateReady -> IconButton(onClick = { inAppUpdateLauncher.completeUpdate() }) {
                            Icon(Icons.Filled.Download, contentDescription = strings.updateRestartNow)
                        }

                        inAppUpdateStatus == InAppUpdateStatus.Checking || inAppUpdateStatus == InAppUpdateStatus.Downloading ->
                            CircularWavyProgressIndicator(modifier = Modifier.size(Dimens.space20))

                        else -> IconButton(onClick = { onIntent(SettingsIntent.CheckForInAppUpdate(inAppUpdateLauncher)) }) {
                            Icon(Icons.Filled.Refresh, contentDescription = strings.settingsCheckForUpdates)
                        }
                    }
                }
            } else if (appUpdateSupported) {
                {
                    when (val s = updateState) {
                        UpdateCheckState.Checking, is UpdateCheckState.Downloading ->
                            CircularWavyProgressIndicator(modifier = Modifier.size(Dimens.space20))

                        is UpdateCheckState.Available -> IconButton(onClick = { onIntent(SettingsIntent.DownloadUpdate(updateChecker, s.info)) }) {
                            Icon(Icons.Filled.Download, contentDescription = strings.updateDownloadInstall)
                        }

                        is UpdateCheckState.Failed -> IconButton(onClick = { onIntent(SettingsIntent.DownloadUpdate(updateChecker, s.info)) }) {
                            Icon(Icons.Filled.Refresh, contentDescription = strings.updateRetry)
                        }

                        UpdateCheckState.Idle, UpdateCheckState.UpToDate, UpdateCheckState.CheckFailed -> IconButton(onClick = { onIntent(SettingsIntent.CheckForUpdate(updateChecker)) }) {
                            Icon(Icons.Filled.Refresh, contentDescription = strings.settingsCheckForUpdates)
                        }
                    }
                }
            } else null,
        )
        SettingsRowDivider()
        SettingsRow(
            icon = Icons.Default.Person,
            title = strings.settingsAboutAuthor,
            subtitle = BuildConfig.APP_AUTHOR,
        )
        SettingsRowDivider()
        // Opens the web feedback form (legal/feedback.html on GitHub Pages) in a browser;
        // it POSTs to the submit-feedback Edge Function, which emails the maintainer.
        // Its label lives outside Strings — that constructor is at the JVM 255-param limit.
        val feedbackStrings = remember(settings.locale) { resolveFeedbackStrings(settings.locale) }
        SettingsRow(
            icon = Icons.Filled.Feedback,
            title = feedbackStrings.title,
            subtitle = feedbackStrings.subtitle,
            onClick = { uriHandler.openUri(feedbackUrl(settings.locale, settings.theme)) },
        )
    }
}

private const val FEEDBACK_BASE_URL = "https://bobadronov.github.io/debt-tracker/feedback.html"

/**
 * Feedback-form URL carrying the app version/build/platform as query params, plus — each only when
 * it isn't left to the OS — the UI language (`lang`) and the light/dark theme (`theme`), so the web
 * page opens matching the app.
 */
private fun feedbackUrl(locale: String, theme: String): String = buildString {
    append(FEEDBACK_BASE_URL)
    append("?v=").append(BuildConfig.APP_VERSION)
    append("&build=").append(BuildConfig.APP_VERSION_CODE)
    append("&platform=").append(currentPlatform.name.lowercase())
    if (locale != "system") append("&lang=").append(locale)
    if (theme == "light" || theme == "dark") append("&theme=").append(theme)
}

// The @Preview functions render this rather than SettingsScreen directly: the extra hop keeps the
// koinViewModel() call out of the previewed function's own body (the tooling only flags a ViewModel
// one call deep), matching HomeScreen/NotificationsScreen/QrHubScreen. The screen still renders
// through SettingsViewModel, backed by the fakes in preview/PreviewModule.kt — no real I/O.
@Composable
private fun SettingsScreenPreviewContent() {
    SettingsScreen(onBack = {}, onExport = {}, onOpenAuth = {}, onOpenAccountInfo = {}, onOpenLanguage = {})
}

@Preview
@Composable
private fun SettingsScreenLightPhonePreview() = DebtTrackerPreview(darkTheme = false) { SettingsScreenPreviewContent() }

@Preview
@Composable
private fun SettingsScreenDarkPhonePreview() = DebtTrackerPreview(darkTheme = true) { SettingsScreenPreviewContent() }

@Preview(device = DESKTOP)
@Composable
private fun SettingsScreenLightDesktopPreview() = DebtTrackerPreview(darkTheme = false) { SettingsScreenPreviewContent() }

@Preview(device = DESKTOP)
@Composable
private fun SettingsScreenDarkDesktopPreview() = DebtTrackerPreview(darkTheme = true) { SettingsScreenPreviewContent() }
