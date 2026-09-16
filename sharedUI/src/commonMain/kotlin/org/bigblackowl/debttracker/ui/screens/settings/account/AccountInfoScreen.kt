package org.bigblackowl.debttracker.ui.screens.settings.account

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Devices.DESKTOP
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.bigblackowl.debttracker.core.i18n.LocalStrings
import org.bigblackowl.debttracker.domain.repository.AuthRepository
import org.bigblackowl.debttracker.domain.validation.formatUkrainianPhone
import org.bigblackowl.debttracker.preview.DebtTrackerPreview
import org.bigblackowl.debttracker.theme.Dimens
import org.bigblackowl.debttracker.ui.components.AccountAvatar
import org.bigblackowl.debttracker.ui.components.SettingsDetailScaffold
import org.bigblackowl.debttracker.ui.components.SettingsRow
import org.bigblackowl.debttracker.ui.components.SettingsRowDivider
import org.bigblackowl.debttracker.ui.components.SettingsSection
import org.bigblackowl.debttracker.ui.components.button.Button
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
    val avatarUrl by authRepository.avatarUrl.collectAsStateWithLifecycle()
    val name by authRepository.displayName.collectAsStateWithLifecycle()
    val email by authRepository.email.collectAsStateWithLifecycle()
    val phone by authRepository.phone.collectAsStateWithLifecycle()

    AccountInfoContent(
        state = AccountInfoState(avatarUrl = avatarUrl, name = name, email = email, phone = phone),
        onBack = onBack,
        onEdit = onEdit,
        onOpenActiveSessions = onOpenActiveSessions,
    )
}

data class AccountInfoState(
    val avatarUrl: String? = null,
    val name: String? = null,
    val email: String? = null,
    val phone: String? = null,
)

@Composable
private fun AccountInfoContent(
    state: AccountInfoState,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onOpenActiveSessions: () -> Unit,
) {
    val strings = LocalStrings.current
    SettingsDetailScaffold(title = strings.settings.account, onBack = onBack) {
        Spacer(Modifier.height(Dimens.Spacing.sm))
        AccountAvatar(avatarUrl = state.avatarUrl, isUploading = false, onEditClick = onEdit)
        Spacer(Modifier.height(Dimens.Spacing.sm))

        SettingsSection(strings.settings.account) {
            SettingsRow(icon = Icons.Filled.Person, title = strings.fullName, subtitle = state.name?.takeIf { it.isNotBlank() })
            SettingsRowDivider()
            SettingsRow(icon = Icons.Filled.Email, title = strings.email, subtitle = state.email?.takeIf { it.isNotBlank() })
            SettingsRowDivider()
            SettingsRow(icon = Icons.Filled.Phone, title = strings.phone, subtitle = formatUkrainianPhone(state.phone))
            SettingsRowDivider()
            SettingsRow(
                icon = Icons.Filled.Devices,
                title = strings.settings.activeSessions,
                onClick = onOpenActiveSessions,
            )
        }

        Button(onClick = onEdit, modifier = Modifier.fillMaxWidth()) {
            Text(strings.accountInfoEdit)
        }
    }
}

@Composable
private fun Preview(state: AccountInfoState) = AccountInfoContent(
    state = state,
    onBack = {},
    onEdit = {},
    onOpenActiveSessions = {},
)

@Preview
@Composable
private fun AccountInfoScreenLightPhonePreview() = DebtTrackerPreview(darkTheme = false) {
    Preview(AccountInfoState(name = "Тарас Шевченко", email = "taras@example.com", phone = "+380501234567"))
}

@Preview
@Composable
private fun AccountInfoScreenDarkPhonePreview() = DebtTrackerPreview(darkTheme = true) {
    Preview(AccountInfoState(name = "Тарас Шевченко", email = "taras@example.com", phone = "+380501234567"))
}

@Preview(device = DESKTOP)
@Composable
private fun AccountInfoScreenLightDesktopPreview() = DebtTrackerPreview(darkTheme = false) {
    Preview(AccountInfoState(name = "Тарас Шевченко", email = "taras@example.com", phone = "+380501234567"))
}

@Preview(device = DESKTOP)
@Composable
private fun AccountInfoScreenDarkDesktopPreview() = DebtTrackerPreview(darkTheme = true) {
    Preview(AccountInfoState(name = "Тарас Шевченко", email = "taras@example.com", phone = "+380501234567"))
}
