package org.bigblackowl.debttracker.ui.screens.authgate

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.text.input.ImeAction
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
import org.bigblackowl.debttracker.ui.components.PinCodeField
import org.bigblackowl.debttracker.ui.components.PlaceholderScreen
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

/**
 * Auth Gate (спек §6, п.2). Механізм розблокування залежить від того, що налаштовано
 * (SettingsScreen/ProtectionOnboardingScreen), а не лише від платформи: Android/iOS з
 * увімкненою біометрією — біометрія; Android/iOS без біометричного заліза (типово для
 * планшетів) чи Desktop (немає нативної біометрії) — PIN; Web — екран узагалі не
 * рендериться, бо вхід уже захищений обов'язковим email/паролем (Account+Sync-only на Web).
 * PIN/biometric verification lives in [AuthGateViewModel].
 */
@Composable
fun AuthGateScreen(onUnlocked: () -> Unit, viewModel: AuthGateViewModel = koinViewModel()) {
    val biometricAuthenticator = rememberBiometricAuthenticator()
    val settings = koinInject<AppSettings>()
    val strings = LocalStrings.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    val pinFocusRequester = remember { FocusRequester() }

    val isMobile = currentPlatform == AppPlatform.ANDROID || currentPlatform == AppPlatform.IOS
    val usesBiometric = isMobile && settings.biometricEnabled

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                AuthGateEffect.Unlocked -> onUnlocked()
            }
        }
    }

    LaunchedEffect(Unit) {
        when {
            currentPlatform == AppPlatform.WEB -> onUnlocked()
            usesBiometric -> viewModel.onIntent(AuthGateIntent.Authenticate(biometricAuthenticator))
            else -> Unit
        }
    }

    if (currentPlatform == AppPlatform.WEB) return

    PlaceholderScreen(title = strings.authGateTitle) {
        if (usesBiometric) {
            Text(if (state.biometricFailed) strings.authGateBiometricFailed else strings.authGateBiometricPrompt)
            Spacer(Modifier.height(Dimens.space16))
            Button(onClick = { viewModel.onIntent(AuthGateIntent.Authenticate(biometricAuthenticator)) }) {
                Text(strings.authGateRetry)
            }
        } else {
            LaunchedEffect(Unit) { pinFocusRequester.requestFocus() }

            Text(strings.authGateEnterPin)
            Spacer(Modifier.height(Dimens.space8))
            PinCodeField(
                value = state.pinInput,
                onValueChange = { viewModel.onIntent(AuthGateIntent.PinChanged(it)) },
                focusRequester = pinFocusRequester,
                imeAction = ImeAction.Done,
                keyboardActions = KeyboardActions(onDone = { viewModel.onIntent(AuthGateIntent.TryUnlockWithPin) }),
                modifier = Modifier.fillMaxWidth(),
            )
            state.error?.let {
                Spacer(Modifier.height(Dimens.space8))
                Text(it, color = MaterialTheme.debtAccentColors.debt)
            }
            Spacer(Modifier.height(Dimens.space16))
            Button(onClick = { viewModel.onIntent(AuthGateIntent.TryUnlockWithPin) }) { Text(strings.authGateUnlock) }
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
