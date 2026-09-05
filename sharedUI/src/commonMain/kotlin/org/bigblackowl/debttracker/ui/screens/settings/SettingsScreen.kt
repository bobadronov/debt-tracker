package org.bigblackowl.debttracker.ui.screens.settings

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Password
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Devices.DESKTOP
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.bigblackowl.debttracker.BuildConfig
import org.bigblackowl.debttracker.core.i18n.LocalStrings
import org.bigblackowl.debttracker.core.platform.AppPlatform
import org.bigblackowl.debttracker.core.platform.currentPlatform
import org.bigblackowl.debttracker.core.settings.AppSettings
import org.bigblackowl.debttracker.domain.repository.AuthRepository
import org.bigblackowl.debttracker.preview.DebtTrackerPreview
import org.bigblackowl.debttracker.theme.Dimens
import org.bigblackowl.debttracker.ui.components.AccountAvatar
import org.bigblackowl.debttracker.ui.components.ConfirmDialog
import org.bigblackowl.debttracker.ui.components.PlaceholderScreen
import org.bigblackowl.debttracker.ui.components.SettingsRow
import org.bigblackowl.debttracker.ui.components.SettingsRowDivider
import org.bigblackowl.debttracker.ui.components.SettingsSection
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

/**
 * SettingsScreen — тепер лише хаб (було: один довгий скрол на все, поки перемикачів не стало
 * забагато): обліковий запис зверху (найважливіше — видно одразу й керується прямо тут через
 * [SettingsViewModel]), нижче — навігаційні рядки до п'яти окремих екранів (Захист / Сповіщення /
 * Параметри / Дані / Про застосунок). Кожен підрозділ тепер сам відповідає за свій
 * [SettingsViewModel]/[AppSettings]-стан — той самий підхід, що вже був у окремій LanguageScreen.
 */
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenAuth: () -> Unit,
    onOpenAccountInfo: () -> Unit,
    onOpenProtection: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenPreferences: () -> Unit,
    onOpenData: () -> Unit,
    onOpenAbout: () -> Unit,
    viewModel: SettingsViewModel = koinViewModel(),
) {
    val settings = koinInject<AppSettings>()
    val authRepository = koinInject<AuthRepository>()
    val strings = LocalStrings.current
    val isAuthenticated by authRepository.isAuthenticated.collectAsStateWithLifecycle()
    var showSignOutConfirm by remember { mutableStateOf(false) }

    val usesPinProtection = currentPlatform == AppPlatform.DESKTOP
    val protectionIcon = if (usesPinProtection) Icons.Filled.Password else Icons.Filled.Fingerprint
    val showProtectionRow = currentPlatform != AppPlatform.WEB

    PlaceholderScreen(title = strings.settings.title, onBack = onBack) {
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
                SettingsSection(null) {
                    if (showProtectionRow) {
                        SettingsRow(
                            icon = protectionIcon,
                            title = strings.settings.protection,
                            onClick = onOpenProtection,
                        )
                        SettingsRowDivider()
                    }
                    if (isAuthenticated) {
                        SettingsRow(
                            icon = if (settings.notificationsEnabled) Icons.Filled.Notifications else Icons.Filled.NotificationsOff,
                            title = strings.settings.notifications,
                            onClick = onOpenNotifications,
                        )
                        SettingsRowDivider()
                    }
                    SettingsRow(
                        icon = Icons.Filled.Tune,
                        title = strings.settings.preferences,
                        onClick = onOpenPreferences,
                    )
                    SettingsRowDivider()
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
                viewModel.onIntent(SettingsIntent.SignOut)
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

    SettingsSection(strings.settings.account) {
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
                    if (isAuthenticated) strings.settings.accountSynced(accountName ?: accountEmail.orEmpty()) else strings.settings.localOnly,
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

// The @Preview functions render this rather than SettingsScreen directly: the extra hop keeps the
// koinViewModel() call out of the previewed function's own body (the tooling only flags a ViewModel
// one call deep), matching HomeScreen/NotificationsScreen/QrHubScreen. The screen still renders
// through SettingsViewModel, backed by the fakes in preview/PreviewModule.kt — no real I/O.
@Composable
private fun SettingsScreenPreviewContent() {
    SettingsScreen(
        onBack = {},
        onOpenAuth = {},
        onOpenAccountInfo = {},
        onOpenProtection = {},
        onOpenNotifications = {},
        onOpenPreferences = {},
        onOpenData = {},
        onOpenAbout = {},
    )
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
