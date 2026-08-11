package org.bigblackowl.debttracker.ui.screens

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.text.input.ImeAction
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
import org.bigblackowl.debttracker.ui.components.PIN_LENGTH
import org.bigblackowl.debttracker.ui.components.PinCodeField
import org.bigblackowl.debttracker.ui.components.PlaceholderScreen
import org.koin.compose.koinInject

/**
 * Auth Gate (спек §6, п.2). Механізм розблокування залежить від платформи:
 * Android/iOS — лише біометрія, без PIN-фолбеку; Desktop — лише PIN (немає нативної
 * біометрії); Web — екран узагалі не рендериться, бо вхід уже захищений обов'язковим
 * email/паролем (Account+Sync-only на Web).
 */
@Composable
fun AuthGateScreen(onUnlocked: () -> Unit) {
    val settings = koinInject<AppSettings>()
    val biometricAuthenticator = rememberBiometricAuthenticator()
    val scope = rememberCoroutineScope()
    val strings = LocalStrings.current

    var pinInput by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var biometricFailed by remember { mutableStateOf(false) }
    val pinFocusRequester = remember { FocusRequester() }

    val isMobile = currentPlatform == AppPlatform.ANDROID || currentPlatform == AppPlatform.IOS

    fun tryUnlock() {
        if (pinInput.isNotEmpty() && settings.verifyPinCode(pinInput)) {
            onUnlocked()
        } else {
            error = strings.authGateWrongPin
        }
    }

    LaunchedEffect(Unit) {
        when (currentPlatform) {
            AppPlatform.WEB -> onUnlocked()
            AppPlatform.ANDROID, AppPlatform.IOS -> {
                val result = biometricAuthenticator.authenticate(strings.biometricUnlockReason)
                if (result == BiometricResult.SUCCESS) onUnlocked() else biometricFailed = true
            }
            AppPlatform.DESKTOP -> Unit
        }
    }

    if (currentPlatform == AppPlatform.WEB) return

    PlaceholderScreen(title = strings.authGateTitle) {
        if (isMobile) {
            Text(if (biometricFailed) strings.authGateBiometricFailed else strings.authGateBiometricPrompt)
            Spacer(Modifier.height(Dimens.space16))
            Button(onClick = {
                scope.launch {
                    when (biometricAuthenticator.authenticate(strings.biometricUnlockReason)) {
                        BiometricResult.SUCCESS -> onUnlocked()
                        else -> biometricFailed = true
                    }
                }
            }) { Text(strings.authGateRetry) }
        } else {
            LaunchedEffect(Unit) { pinFocusRequester.requestFocus() }

            Text(strings.authGateEnterPin)
            Spacer(Modifier.height(Dimens.space8))
            PinCodeField(
                value = pinInput,
                onValueChange = { new ->
                    pinInput = new
                    error = null
                    // Google-style OTP/PIN entry: submit the moment the PIN is complete, no extra tap needed.
                    if (new.length == PIN_LENGTH) tryUnlock()
                },
                focusRequester = pinFocusRequester,
                imeAction = ImeAction.Done,
                keyboardActions = KeyboardActions(onDone = { tryUnlock() }),
                modifier = Modifier.fillMaxWidth(),
            )
            error?.let {
                Spacer(Modifier.height(Dimens.space8))
                Text(it, color = MaterialTheme.debtAccentColors.debt)
            }
            Spacer(Modifier.height(Dimens.space16))
            Button(onClick = { tryUnlock() }) { Text(strings.authGateUnlock) }
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
