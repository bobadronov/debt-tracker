package org.bigblackowl.debttracker.ui.screens

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Password
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Devices.DESKTOP
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.launch
import org.bigblackowl.debttracker.core.i18n.LocalStrings
import org.bigblackowl.debttracker.core.platform.AppPlatform
import org.bigblackowl.debttracker.core.platform.currentPlatform
import org.bigblackowl.debttracker.core.security.BiometricResult
import org.bigblackowl.debttracker.core.security.rememberBiometricAuthenticator
import org.bigblackowl.debttracker.core.settings.AppSettings
import org.bigblackowl.debttracker.preview.DebtTrackerPreview
import org.bigblackowl.debttracker.theme.Dimens
import org.bigblackowl.debttracker.theme.debtAccentColors
import org.bigblackowl.debttracker.ui.components.PinSetupDialog
import org.bigblackowl.debttracker.ui.components.PlaceholderScreen
import org.koin.compose.koinInject

/**
 * First-launch-only screen explaining why to turn on app lock, shown once before [AuthGateScreen]/Home
 * become reachable (see [AppSettings.hasSeenProtectionOnboarding]). Not shown on Web — Web has no
 * local app lock at all (email/password sign-in already guards it).
 */
@Composable
fun ProtectionOnboardingScreen(onDone: () -> Unit) {
    val settings = koinInject<AppSettings>()
    val biometricAuthenticator = rememberBiometricAuthenticator()
    val scope = rememberCoroutineScope()
    val strings = LocalStrings.current

    var biometricAvailable by remember { mutableStateOf(false) }
    var showPinSetupDialog by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val isMobile = currentPlatform == AppPlatform.ANDROID || currentPlatform == AppPlatform.IOS

    LaunchedEffect(Unit) {
        if (isMobile) biometricAvailable = biometricAuthenticator.isAvailable()
    }

    fun finish() {
        settings.hasSeenProtectionOnboarding = true
        onDone()
    }

    PlaceholderScreen(title = strings.onboardingProtectionTitle) {
        Icon(
            if (currentPlatform == AppPlatform.DESKTOP) Icons.Filled.Password else Icons.Filled.Fingerprint,
            contentDescription = null,
            modifier = Modifier.size(Dimens.space60),
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(Dimens.space16))
        Text(strings.onboardingProtectionBody, textAlign = TextAlign.Center)
        error?.let {
            Spacer(Modifier.height(Dimens.space8))
            Text(it, color = MaterialTheme.debtAccentColors.debt, textAlign = TextAlign.Center)
        }
        Spacer(Modifier.height(Dimens.space24))

        if (currentPlatform == AppPlatform.DESKTOP) {
            Button(onClick = { showPinSetupDialog = true }) {
                Text(strings.onboardingProtectionEnablePin)
            }
        } else if (biometricAvailable) {
            Button(onClick = {
                scope.launch {
                    when (biometricAuthenticator.authenticate(strings.biometricEnableReason)) {
                        BiometricResult.SUCCESS -> {
                            settings.protectionEnabled = true
                            settings.biometricEnabled = true
                            finish()
                        }
                        else -> error = strings.onboardingProtectionConfirmFailed
                    }
                }
            }) { Text(strings.onboardingProtectionEnableBiometric) }
        }

        Spacer(Modifier.height(Dimens.space8))
        TextButton(onClick = { finish() }) { Text(strings.onboardingProtectionSkip) }
    }

    if (showPinSetupDialog) {
        PinSetupDialog(
            onDismiss = { showPinSetupDialog = false },
            onConfirm = { pin ->
                settings.pinCode = pin
                settings.protectionEnabled = true
                showPinSetupDialog = false
                finish()
            },
        )
    }
}

@Preview(device = DESKTOP)
@Composable
private fun ProtectionOnboardingScreenPreview() = DebtTrackerPreview {
    ProtectionOnboardingScreen(onDone = {})
}
