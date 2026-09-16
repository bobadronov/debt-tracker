package org.bigblackowl.debttracker.ui.screens.protectiononboarding

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Password
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
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
import org.bigblackowl.debttracker.theme.debtAccentColors
import org.bigblackowl.debttracker.ui.components.button.Button
import org.bigblackowl.debttracker.ui.components.button.TextButton
import org.bigblackowl.debttracker.ui.components.text.BodyText
import org.bigblackowl.debttracker.ui.components.unlock.PinSetupDialog
import org.bigblackowl.debttracker.ui.components.unlock.UnlockScaffold
import org.koin.compose.viewmodel.koinViewModel

/**
 * First-launch-only screen explaining why to turn on app lock — the second onboarding step, shown
 * once after [org.bigblackowl.debttracker.ui.screens.accountonboarding.AccountOnboardingScreen] and
 * before Home (see [AppSettings.hasSeenProtectionOnboarding]). Not shown on Web — Web has no
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
    val state by viewModel.state.collectAsStateWithLifecycle()

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

    ProtectionOnboardingContent(
        state = state,
        onEnableBiometric = { viewModel.onIntent(ProtectionOnboardingIntent.EnableBiometric(biometricAuthenticator)) },
        onEnablePin = { viewModel.onIntent(ProtectionOnboardingIntent.EnablePin(it)) },
        onSkip = { viewModel.onIntent(ProtectionOnboardingIntent.Skip) },
    )
}

@Composable
private fun ProtectionOnboardingContent(
    state: ProtectionOnboardingState,
    onEnableBiometric: () -> Unit,
    onEnablePin: (String) -> Unit,
    onSkip: () -> Unit,
) {
    val strings = LocalStrings.current
    var showPinSetupDialog by remember { mutableStateOf(false) }

    UnlockScaffold(
        title = strings.onboardingProtection.title,
        subtitle = strings.onboardingProtection.body,
    ) {
        Icon(
            if (state.biometricAvailable) Icons.Filled.Fingerprint else Icons.Filled.Password,
            contentDescription = null,
            modifier = Modifier.size(Dimens.IconSize.lg),
            tint = MaterialTheme.colorScheme.primary,
        )
        state.error?.let {
            Spacer(Modifier.height(Dimens.Spacing.sm))
            BodyText(it, color = MaterialTheme.debtAccentColors.debt, textAlign = TextAlign.Center)
        }
        Spacer(Modifier.height(Dimens.Spacing.xl))

        // Desktop has no biometric at all; mobile devices without biometric hardware/enrollment
        // (common on tablets) fall back to the same PIN setup instead of only offering Skip.
        if (state.biometricAvailable) {
            Button(onClick = onEnableBiometric) {
                Text(strings.onboardingProtection.enableBiometric)
            }
        } else {
            Button(onClick = { showPinSetupDialog = true }) {
                Text(strings.onboardingProtection.enablePin)
            }
        }

        Spacer(Modifier.height(Dimens.Spacing.sm))
        TextButton(onClick = onSkip) { Text(strings.onboardingProtection.skip) }
    }

    if (showPinSetupDialog) {
        PinSetupDialog(
            onDismiss = { showPinSetupDialog = false },
            onConfirm = { pin ->
                showPinSetupDialog = false
                onEnablePin(pin)
            },
        )
    }
}

@Composable
private fun Preview(state: ProtectionOnboardingState) = ProtectionOnboardingContent(
    state = state,
    onEnableBiometric = {},
    onEnablePin = {},
    onSkip = {},
)

@Preview
@Composable
private fun ProtectionOnboardingScreenLightPhonePreview() = DebtTrackerPreview(darkTheme = false) {
    Preview(ProtectionOnboardingState(biometricAvailable = true))
}

@Preview
@Composable
private fun ProtectionOnboardingScreenDarkPhonePreview() = DebtTrackerPreview(darkTheme = true) {
    Preview(ProtectionOnboardingState())
}

@Preview(device = DESKTOP)
@Composable
private fun ProtectionOnboardingScreenLightDesktopPreview() = DebtTrackerPreview(darkTheme = false) {
    Preview(ProtectionOnboardingState())
}

@Preview(device = DESKTOP)
@Composable
private fun ProtectionOnboardingScreenDarkDesktopPreview() = DebtTrackerPreview(darkTheme = true) {
    Preview(ProtectionOnboardingState())
}
