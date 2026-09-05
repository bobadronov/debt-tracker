package org.bigblackowl.debttracker.ui.screens.authgate

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.Button
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Devices.DESKTOP
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import org.bigblackowl.debttracker.core.i18n.LocalStrings
import org.bigblackowl.debttracker.core.platform.AppPlatform
import org.bigblackowl.debttracker.core.platform.currentPlatform
import org.bigblackowl.debttracker.core.security.rememberBiometricAuthenticator
import org.bigblackowl.debttracker.core.settings.AppSettings
import org.bigblackowl.debttracker.preview.DebtTrackerPreview
import org.bigblackowl.debttracker.theme.Dimens
import org.bigblackowl.debttracker.theme.debtAccentColors
import org.bigblackowl.debttracker.ui.components.PinCodeField
import org.bigblackowl.debttracker.ui.components.UnlockScaffold
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

/**
 * Auth Gate (спек §6, п.2). Механізм розблокування — від того, що налаштовано (SettingsScreen /
 * ProtectionOnboardingScreen): Android/iOS з увімкненою біометрією — біометрія з відкотом на
 * PIN-клавіатуру, якщо вона доступна; Desktop і планшети без біометрії — одразу PIN. Web — екран
 * не рендериться (там вхід уже захищено обов'язковим email/паролем). Уся перевірка та обробка
 * невдач — у [AuthGateViewModel]; цей екран лише малює [AuthGateState.mode].
 */
@Composable
fun AuthGateScreen(onUnlocked: () -> Unit, viewModel: AuthGateViewModel = koinViewModel()) {
    val biometricAuthenticator = rememberBiometricAuthenticator()
    val settings = koinInject<AppSettings>()
    val strings = LocalStrings.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    val pinFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                AuthGateEffect.Unlocked -> onUnlocked()
            }
        }
    }

    LaunchedEffect(Unit) {
        if (currentPlatform == AppPlatform.WEB) onUnlocked()
        else viewModel.onIntent(AuthGateIntent.Started(biometricAuthenticator))
    }

    if (currentPlatform == AppPlatform.WEB) return

    // Grab focus for the hidden PIN text field so its keyboard is ready immediately — the system
    // numeric keyboard on mobile, the physical keyboard on desktop. Keep retrying: the field isn't
    // always attached on the first frame (part of the old "PIN entry is unreliable" report).
    LaunchedEffect(state.mode) {
        if (state.mode == UnlockMode.PIN) {
            repeat(10) {
                runCatching { pinFocusRequester.requestFocus() }.onSuccess { return@LaunchedEffect }
                delay(50)
            }
        }
    }

    when (state.mode) {
        UnlockMode.BIOMETRIC -> UnlockScaffold(
            title = strings.authGate.title,
            subtitle = if (state.biometricDismissed) strings.authGate.biometricFailed else strings.authGate.biometricPrompt,
        ) {
            if (state.biometricRunning) {
                CircularWavyProgressIndicator()
            } else {
                Icon(
                    Icons.Filled.Fingerprint,
                    contentDescription = null,
                    modifier = Modifier.size(Dimens.space60),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(Dimens.space24))
                Button(onClick = { viewModel.onIntent(AuthGateIntent.RetryBiometric(biometricAuthenticator)) }) {
                    Text(strings.authGate.retry)
                }
                if (settings.hasPinCode) {
                    TextButton(onClick = { viewModel.onIntent(AuthGateIntent.SwitchToPin) }) {
                        Text(strings.authGate.usePinCode)
                    }
                }
            }
        }

        UnlockMode.PIN -> UnlockScaffold(
            title = strings.authGate.title,
            subtitle = strings.authGate.enterPin,
        ) {
            PinCodeField(
                value = state.pinInput,
                onValueChange = { viewModel.onIntent(AuthGateIntent.PinChanged(it)) },
                focusRequester = pinFocusRequester,
            )
            AnimatedVisibility(
                state.error != null,
                enter = slideInVertically { -it },
                exit = slideOutVertically { it },
            ) {
                Text(
                    state.error ?: "",
                    color = MaterialTheme.debtAccentColors.debt,
                    textAlign = TextAlign.Center,
                )
            }
            if (settings.biometricEnabled) {
                Spacer(Modifier.height(Dimens.space8))
                TextButton(onClick = { viewModel.onIntent(AuthGateIntent.RetryBiometric(biometricAuthenticator)) }) {
                    Text(strings.authGate.useBiometric)
                }
            }
        }
    }
}

@Preview
@Composable
private fun AuthGateScreenLightPhonePreview() = DebtTrackerPreview(darkTheme = false) {
    AuthGateScreen(onUnlocked = {})
}

@Preview
@Composable
private fun AuthGateScreenDarkPhonePreview() = DebtTrackerPreview(darkTheme = true) {
    AuthGateScreen(onUnlocked = {})
}

@Preview(device = DESKTOP)
@Composable
private fun AuthGateScreenLightDesktopPreview() = DebtTrackerPreview(darkTheme = false) {
    AuthGateScreen(onUnlocked = {})
}

@Preview(device = DESKTOP)
@Composable
private fun AuthGateScreenDarkDesktopPreview() = DebtTrackerPreview(darkTheme = true) {
    AuthGateScreen(onUnlocked = {})
}
