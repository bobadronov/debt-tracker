package org.bigblackowl.debttracker.ui.screens

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
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
    var pinVisible by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var biometricFailed by remember { mutableStateOf(false) }

    val isMobile = currentPlatform == AppPlatform.ANDROID || currentPlatform == AppPlatform.IOS

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
            Text(strings.authGateEnterPin)
            Spacer(Modifier.height(Dimens.space8))
            OutlinedTextField(
                value = pinInput,
                onValueChange = { pinInput = it; error = null },
                label = { Text(strings.authGatePinLabel) },
                isError = error != null,
                supportingText = { error?.let { Text(it) } },
                singleLine = true,
                visualTransformation = if (pinVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                trailingIcon = {
                    IconButton(onClick = { pinVisible = !pinVisible }) {
                        Icon(
                            if (pinVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (pinVisible) strings.hidePin else strings.showPin,
                        )
                    }
                },
            )
            Spacer(Modifier.height(Dimens.space16))
            Button(onClick = {
                if (pinInput.isNotEmpty() && pinInput == settings.pinCode) {
                    onUnlocked()
                } else {
                    error = strings.authGateWrongPin
                }
            }) { Text(strings.authGateUnlock) }
        }
    }
}

@Preview(device = DESKTOP)
@Composable
private fun AuthGateScreenPreview() = DebtTrackerPreview {
    AuthGateScreen(onUnlocked = {})
}
