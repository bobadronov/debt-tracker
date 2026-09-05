package org.bigblackowl.debttracker.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Password
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.bigblackowl.debttracker.core.i18n.LocalStrings
import org.bigblackowl.debttracker.core.platform.AppPlatform
import org.bigblackowl.debttracker.core.platform.currentPlatform
import org.bigblackowl.debttracker.core.security.rememberBiometricAuthenticator
import org.bigblackowl.debttracker.core.settings.AppSettings
import org.bigblackowl.debttracker.theme.Dimens
import org.bigblackowl.debttracker.ui.components.PinSetupDialog
import org.bigblackowl.debttracker.ui.components.PlaceholderScreen
import org.bigblackowl.debttracker.ui.components.SettingsRow
import org.bigblackowl.debttracker.ui.components.SettingsSection
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

/** Settings → Захист входу (PIN/біометрія) — виокремлено з колишнього єдиного SettingsScreen. */
@Composable
fun SettingsProtectionScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = koinViewModel(),
) {
    val settings = koinInject<AppSettings>()
    val biometricAuthenticator = rememberBiometricAuthenticator()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val strings = LocalStrings.current

    var showPinSetupDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.onIntent(SettingsIntent.CheckBiometricHardware(biometricAuthenticator))
    }

    // Мобільні платформи без біометричного заліза (або незареєстрованою біометрією — типово
    // для планшетів) падають на той самий PIN-механізм, що й Desktop, замість ховати перемикач.
    val usesPinProtection = currentPlatform == AppPlatform.DESKTOP || !state.biometricHardwareAvailable
    val protectionIcon = if (usesPinProtection) Icons.Filled.Password else Icons.Filled.Fingerprint

    PlaceholderScreen(title = strings.settings.protection, onBack = onBack) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier.width(Dimens.contentMaxWidth),
                verticalArrangement = Arrangement.spacedBy(Dimens.space24),
            ) {
                SettingsSection(strings.settings.protection) {
                    SettingsRow(
                        icon = protectionIcon,
                        title = strings.settings.protection,
                        subtitle = state.protectionConfirmError,
                        trailing = {
                            Switch(
                                checked = settings.protectionEnabled,
                                onCheckedChange = { checked ->
                                    when {
                                        usesPinProtection && checked && !settings.hasPinCode -> showPinSetupDialog = true
                                        usesPinProtection -> viewModel.onIntent(SettingsIntent.TogglePinProtection(checked))

                                        // Мобільні платформи з біометрією: увімкнення захисту потребує
                                        // підтвердження відбитком/обличчям одразу — інакше можна ввімкнути
                                        // перемикач, маючи чужий палець на сканері, і сам захист виявиться фікцією.
                                        checked -> viewModel.onIntent(SettingsIntent.EnableMobileProtection(biometricAuthenticator))
                                        else -> viewModel.onIntent(SettingsIntent.DisableMobileProtection)
                                    }
                                },
                            )
                        },
                    )
                }
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
}
