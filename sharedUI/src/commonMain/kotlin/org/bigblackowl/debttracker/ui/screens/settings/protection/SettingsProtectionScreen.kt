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
import org.bigblackowl.debttracker.ui.components.SettingsRow
import org.bigblackowl.debttracker.ui.components.SettingsSection
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
    val strings = LocalStrings.current

    var showPinSetupDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.onIntent(SettingsProtectionIntent.CheckBiometricHardware(biometricAuthenticator))
    }

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
                                checked = settings.protectionEnabled,
                                onCheckedChange = { checked ->
                                    when {
                                        usesPinProtection && checked && !settings.hasPinCode -> showPinSetupDialog = true
                                        usesPinProtection -> viewModel.onIntent(SettingsProtectionIntent.TogglePinProtection(checked))

                                        // Mobile platforms with biometrics: enabling protection requires
                                        // immediate fingerprint/face confirmation — otherwise the toggle
                                        // could be flipped with someone else's finger on the sensor,
                                        // making the protection itself a fiction.
                                        checked -> viewModel.onIntent(SettingsProtectionIntent.EnableMobileProtection(biometricAuthenticator))
                                        else -> viewModel.onIntent(SettingsProtectionIntent.DisableMobileProtection)
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
                viewModel.onIntent(SettingsProtectionIntent.SetupPinAndEnableProtection(pin))
            },
        )
    }
}

// The @Preview functions render this rather than SettingsProtectionScreen directly: the extra hop
// keeps the koinViewModel() call out of the previewed function's own body (matching SettingsScreen).
// The screen renders through SettingsProtectionViewModel, backed by the fakes in preview/PreviewModule.kt.
@Composable
private fun SettingsProtectionScreenPreviewContent() {
    SettingsProtectionScreen(onBack = {})
}

@Preview
@Composable
private fun SettingsProtectionScreenLightPhonePreview() = DebtTrackerPreview(darkTheme = false) { SettingsProtectionScreenPreviewContent() }

@Preview
@Composable
private fun SettingsProtectionScreenDarkPhonePreview() = DebtTrackerPreview(darkTheme = true) { SettingsProtectionScreenPreviewContent() }

@Preview(device = DESKTOP)
@Composable
private fun SettingsProtectionScreenLightDesktopPreview() = DebtTrackerPreview(darkTheme = false) { SettingsProtectionScreenPreviewContent() }

@Preview(device = DESKTOP)
@Composable
private fun SettingsProtectionScreenDarkDesktopPreview() = DebtTrackerPreview(darkTheme = true) { SettingsProtectionScreenPreviewContent() }
