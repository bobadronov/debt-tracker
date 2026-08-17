package org.bigblackowl.debttracker.ui.screens.protectiononboarding

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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Devices.DESKTOP
import androidx.compose.ui.tooling.preview.Preview
import org.bigblackowl.debttracker.core.i18n.LocalStrings
import org.bigblackowl.debttracker.core.platform.AppPlatform
import org.bigblackowl.debttracker.core.platform.currentPlatform
import org.bigblackowl.debttracker.core.security.rememberBiometricAuthenticator
import org.bigblackowl.debttracker.core.settings.AppSettings
import org.bigblackowl.debttracker.preview.DebtTrackerPreview
import org.bigblackowl.debttracker.theme.Dimens
import org.bigblackowl.debttracker.theme.debtAccentColors
import org.bigblackowl.debttracker.ui.components.PinSetupDialog
import org.bigblackowl.debttracker.ui.components.PlaceholderScreen
import org.koin.compose.viewmodel.koinViewModel

/**
 * First-launch-only screen explaining why to turn on app lock, shown once before [org.bigblackowl.debttracker.ui.screens.authgate.AuthGateScreen]/Home
 * become reachable (see [AppSettings.hasSeenProtectionOnboarding]). Not shown on Web — Web has no
 * local app lock at all (email/password sign-in already guards it). Logic lives in [ProtectionOnboardingViewModel];
 * this screen only creates the composition-scoped [rememberBiometricAuthenticator] object (Android's
 * BiometricPrompt needs a live Activity, so it can't be constructor-injected into the ViewModel) and forwards it.
 */
@Composable
fun ProtectionOnboardingScreen(
    onDone: () -> Unit,
    viewModel: ProtectionOnboardingViewModel = koinViewModel(),
) {
    val biometricAuthenticator = rememberBiometricAuthenticator()
    val strings = LocalStrings.current
    val state by viewModel.state.collectAsStateWithLifecycle()

    var showPinSetupDialog by remember { mutableStateOf(false) }

    val isMobile = currentPlatform == AppPlatform.ANDROID || currentPlatform == AppPlatform.IOS

    LaunchedEffect(Unit) {
        if (isMobile) {
            viewModel.onIntent(ProtectionOnboardingIntent.CheckBiometricAvailability(biometricAuthenticator))
        }
    }

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                ProtectionOnboardingEffect.Done -> onDone()
            }
        }
    }

    PlaceholderScreen(title = strings.onboardingProtectionTitle) {
        Icon(
            if (state.biometricAvailable) Icons.Filled.Fingerprint else Icons.Filled.Password,
            contentDescription = null,
            modifier = Modifier.size(Dimens.space60),
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(Dimens.space16))
        Text(strings.onboardingProtectionBody, textAlign = TextAlign.Center)
        state.error?.let {
            Spacer(Modifier.height(Dimens.space8))
            Text(it, color = MaterialTheme.debtAccentColors.debt, textAlign = TextAlign.Center)
        }
        Spacer(Modifier.height(Dimens.space24))

        // Desktop has no biometric at all; mobile devices without biometric hardware/enrollment
        // (common on tablets) fall back to the same PIN setup instead of only offering Skip.
        if (state.biometricAvailable) {
            Button(onClick = { viewModel.onIntent(ProtectionOnboardingIntent.EnableBiometric(biometricAuthenticator)) }) {
                Text(strings.onboardingProtectionEnableBiometric)
            }
        } else {
            Button(onClick = { showPinSetupDialog = true }) {
                Text(strings.onboardingProtectionEnablePin)
            }
        }

        Spacer(Modifier.height(Dimens.space8))
        TextButton(onClick = { viewModel.onIntent(ProtectionOnboardingIntent.Skip) }) { Text(strings.onboardingProtectionSkip) }
    }

    if (showPinSetupDialog) {
        PinSetupDialog(
            onDismiss = { showPinSetupDialog = false },
            onConfirm = { pin ->
                showPinSetupDialog = false
                viewModel.onIntent(ProtectionOnboardingIntent.EnablePin(pin))
            },
        )
    }
}

@Preview
@Composable
private fun ProtectionOnboardingScreenLightPhonePreview() = DebtTrackerPreview(darkTheme = false) {
    ProtectionOnboardingScreen(onDone = {})
}

@Preview
@Composable
private fun ProtectionOnboardingScreenDarkPhonePreview() = DebtTrackerPreview(darkTheme = true) {
    ProtectionOnboardingScreen(onDone = {})
}

@Preview(device = DESKTOP)
@Composable
private fun ProtectionOnboardingScreenLightDesktopPreview() = DebtTrackerPreview(darkTheme = false) {
    ProtectionOnboardingScreen(onDone = {})
}

@Preview(device = DESKTOP)
@Composable
private fun ProtectionOnboardingScreenDarkDesktopPreview() = DebtTrackerPreview(darkTheme = true) {
    ProtectionOnboardingScreen(onDone = {})
}
