package org.bigblackowl.debttracker.ui.screens.settings.protection

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
import androidx.compose.ui.tooling.preview.Devices.DESKTOP
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.bigblackowl.debttracker.core.i18n.LocalStrings
import org.bigblackowl.debttracker.core.platform.AppPlatform
import org.bigblackowl.debttracker.core.platform.currentPlatform
import org.bigblackowl.debttracker.core.security.rememberBiometricAuthenticator
import org.bigblackowl.debttracker.core.settings.AppSettings
import org.bigblackowl.debttracker.preview.DebtTrackerPreview
import org.bigblackowl.debttracker.theme.Dimens
import org.bigblackowl.debttracker.ui.components.PlaceholderScreen
import org.bigblackowl.debttracker.ui.components.settings.SettingsRow
import org.bigblackowl.debttracker.ui.components.settings.SettingsSection
import org.bigblackowl.debttracker.ui.components.unlock.PinSetupDialog
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

/** Settings → Login protection (PIN/biometrics) — split out from the former single SettingsScreen. */
@Composable
fun SettingsProtectionScreen(
    onBack: () -> Unit,
    viewModel: SettingsProtectionViewModel = koinViewModel(),
) {
    val settings = koinInject<AppSettings>()
    val biometricAuthenticator = rememberBiometricAuthenticator()
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.onIntent(SettingsProtectionIntent.CheckBiometricHardware(biometricAuthenticator))
    }

    SettingsProtectionContent(
        state = state,
        protectionEnabled = settings.protectionEnabled,
        hasPinCode = settings.hasPinCode,
        onBack = onBack,
        onTogglePinProtection = { viewModel.onIntent(SettingsProtectionIntent.TogglePinProtection(it)) },
        onSetupPinAndEnableProtection = { viewModel.onIntent(SettingsProtectionIntent.SetupPinAndEnableProtection(it)) },
        onEnableMobileProtection = { viewModel.onIntent(SettingsProtectionIntent.EnableMobileProtection(biometricAuthenticator)) },
        onDisableMobileProtection = { viewModel.onIntent(SettingsProtectionIntent.DisableMobileProtection) },
    )
}

@Composable
private fun SettingsProtectionContent(
    state: SettingsProtectionState,
    protectionEnabled: Boolean,
    hasPinCode: Boolean,
    onBack: () -> Unit,
    onTogglePinProtection: (Boolean) -> Unit,
    onSetupPinAndEnableProtection: (String) -> Unit,
    onEnableMobileProtection: () -> Unit,
    onDisableMobileProtection: () -> Unit,
) {
    val strings = LocalStrings.current
    var showPinSetupDialog by remember { mutableStateOf(false) }

    // Mobile platforms without biometric hardware (or with no biometrics enrolled — typical
    // for tablets) fall back to the same PIN mechanism as Desktop instead of hiding the toggle.
    val usesPinProtection = currentPlatform == AppPlatform.DESKTOP || !state.biometricHardwareAvailable
    val protectionIcon = if (usesPinProtection) Icons.Filled.Password else Icons.Filled.Fingerprint

    PlaceholderScreen(title = strings.settings.protection, onBack = onBack) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier.width(Dimens.contentMaxWidth),
                verticalArrangement = Arrangement.spacedBy(Dimens.Spacing.xl),
            ) {
                SettingsSection(strings.settings.protection) {
                    SettingsRow(
                        icon = protectionIcon,
                        title = strings.settings.protection,
                        subtitle = state.protectionConfirmError,
                        trailing = {
                            Switch(
                                checked = protectionEnabled,
                                onCheckedChange = { checked ->
                                    when {
                                        usesPinProtection && checked && !hasPinCode -> showPinSetupDialog = true
                                        usesPinProtection -> onTogglePinProtection(checked)

                                        // Mobile platforms with biometrics: enabling protection requires
                                        // immediate fingerprint/face confirmation — otherwise the toggle
                                        // could be flipped with someone else's finger on the sensor,
                                        // making the protection itself a fiction.
                                        checked -> onEnableMobileProtection()
                                        else -> onDisableMobileProtection()
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
                onSetupPinAndEnableProtection(pin)
            },
        )
    }
}

@Composable
private fun Preview(state: SettingsProtectionState, protectionEnabled: Boolean = false, hasPinCode: Boolean = false) =
    SettingsProtectionContent(
        state = state,
        protectionEnabled = protectionEnabled,
        hasPinCode = hasPinCode,
        onBack = {},
        onTogglePinProtection = {},
        onSetupPinAndEnableProtection = {},
        onEnableMobileProtection = {},
        onDisableMobileProtection = {},
    )

@Preview
@Composable
private fun SettingsProtectionScreenLightPhonePreview() = DebtTrackerPreview(darkTheme = false) { Preview(SettingsProtectionState()) }

@Preview
@Composable
private fun SettingsProtectionScreenDarkPhonePreview() = DebtTrackerPreview(darkTheme = true) { Preview(SettingsProtectionState()) }

@Preview(device = DESKTOP)
@Composable
private fun SettingsProtectionScreenLightDesktopPreview() = DebtTrackerPreview(darkTheme = false) { Preview(SettingsProtectionState()) }

@Preview(device = DESKTOP)
@Composable
private fun SettingsProtectionScreenDarkDesktopPreview() = DebtTrackerPreview(darkTheme = true) { Preview(SettingsProtectionState()) }

@Preview
@Composable
private fun SettingsProtectionScreenBiometricOnPreview() = DebtTrackerPreview(darkTheme = false) {
    Preview(SettingsProtectionState(biometricHardwareAvailable = true), protectionEnabled = true)
}

@Preview
@Composable
private fun SettingsProtectionScreenPinOnPreview() = DebtTrackerPreview(darkTheme = false) {
    Preview(SettingsProtectionState(), protectionEnabled = true, hasPinCode = true)
}

@Preview
@Composable
private fun SettingsProtectionScreenErrorPreview() = DebtTrackerPreview(darkTheme = false) {
    Preview(SettingsProtectionState(protectionConfirmError = "Не вдалося підтвердити"), protectionEnabled = true)
}
