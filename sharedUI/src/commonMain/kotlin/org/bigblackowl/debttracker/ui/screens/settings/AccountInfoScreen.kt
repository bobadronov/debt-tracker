package org.bigblackowl.debttracker.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Devices.DESKTOP
import androidx.compose.ui.tooling.preview.Preview
import org.bigblackowl.debttracker.core.i18n.LocalStrings
import org.bigblackowl.debttracker.domain.repository.AuthRepository
import org.bigblackowl.debttracker.preview.DebtTrackerPreview
import org.bigblackowl.debttracker.theme.Dimens
import org.bigblackowl.debttracker.ui.components.AccountAvatar
import org.bigblackowl.debttracker.ui.components.BackButton
import org.bigblackowl.debttracker.ui.components.SettingsRow
import org.bigblackowl.debttracker.ui.components.SettingsRowDivider
import org.bigblackowl.debttracker.ui.components.SettingsSection
import org.koin.compose.koinInject

/**
 * Read-only account detail screen reached by tapping the account card in [SettingsScreen] —
 * shows avatar/name/email/phone plus the "Active devices" entry point (moved here from the
 * top-level Settings list). Actual field editing happens on [EditAccountScreen], reached from
 * here via the Edit button.
 */
@Composable
fun AccountInfoScreen(
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onOpenActiveSessions: () -> Unit,
    authRepository: AuthRepository = koinInject(),
) {
    val strings = LocalStrings.current
    val avatarUrl by authRepository.avatarUrl.collectAsState()
    val name by authRepository.displayName.collectAsState()
    val email by authRepository.email.collectAsState()
    val phone by authRepository.phone.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(strings.settingsAccount) },
                navigationIcon = { BackButton(onClick = onBack) },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier = Modifier.width(Dimens.contentMaxWidth).padding(Dimens.space16),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Dimens.space16),
            ) {
                Spacer(Modifier.height(Dimens.space8))
                AccountAvatar(avatarUrl = avatarUrl, isUploading = false, onEditClick = onEdit)
                Spacer(Modifier.height(Dimens.space8))

                SettingsSection(strings.settingsAccount) {
                    SettingsRow(icon = Icons.Filled.Person, title = strings.fullName, subtitle = name?.takeIf { it.isNotBlank() })
                    SettingsRowDivider()
                    SettingsRow(icon = Icons.Filled.Email, title = strings.email, subtitle = email?.takeIf { it.isNotBlank() })
                    SettingsRowDivider()
                    SettingsRow(icon = Icons.Filled.Phone, title = strings.phone, subtitle = phone?.takeIf { it.isNotBlank() })
                    SettingsRowDivider()
                    SettingsRow(
                        icon = Icons.Filled.Devices,
                        title = strings.settingsActiveSessions,
                        onClick = onOpenActiveSessions,
                    )
                }

                Button(onClick = onEdit, modifier = Modifier.fillMaxWidth()) {
                    Text(strings.accountInfoEdit)
                }
            }
        }
    }
}

@Preview
@Composable
private fun AccountInfoScreenLightPhonePreview() = DebtTrackerPreview(darkTheme = false) {
    AccountInfoScreen(onBack = {}, onEdit = {}, onOpenActiveSessions = {})
}

@Preview
@Composable
private fun AccountInfoScreenDarkPhonePreview() = DebtTrackerPreview(darkTheme = true) {
    AccountInfoScreen(onBack = {}, onEdit = {}, onOpenActiveSessions = {})
}

@Preview(device = DESKTOP)
@Composable
private fun AccountInfoScreenLightDesktopPreview() = DebtTrackerPreview(darkTheme = false) {
    AccountInfoScreen(onBack = {}, onEdit = {}, onOpenActiveSessions = {})
}

@Preview(device = DESKTOP)
@Composable
private fun AccountInfoScreenDarkDesktopPreview() = DebtTrackerPreview(darkTheme = true) {
    AccountInfoScreen(onBack = {}, onEdit = {}, onOpenActiveSessions = {})
}
