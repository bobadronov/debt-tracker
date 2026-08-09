package org.bigblackowl.debttracker.ui.screens

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Devices.DESKTOP
import androidx.compose.ui.tooling.preview.Preview
import org.bigblackowl.debttracker.core.i18n.LocalStrings
import org.bigblackowl.debttracker.core.settings.AppSettings
import org.bigblackowl.debttracker.preview.DebtTrackerPreview
import org.bigblackowl.debttracker.theme.Dimens
import org.bigblackowl.debttracker.ui.components.PlaceholderScreen
import org.koin.compose.koinInject

/**
 * First-launch-only screen explaining Account+Sync, shown once after [ProtectionOnboardingScreen]/
 * [AuthGateScreen] and before Home (see [AppSettings.hasSeenAccountOnboarding]) — surfaces the
 * sign-in option up front instead of leaving it undiscoverable inside Settings. Not shown on Web,
 * which already forces sign-in before Home has no local-only mode to explain.
 */
@Composable
fun AccountOnboardingScreen(onSignIn: () -> Unit, onSkip: () -> Unit) {
    val settings = koinInject<AppSettings>()
    val strings = LocalStrings.current

    fun finish(then: () -> Unit) {
        settings.hasSeenAccountOnboarding = true
        then()
    }

    PlaceholderScreen(title = strings.onboardingAccountTitle) {
        Icon(
            Icons.AutoMirrored.Filled.Login,
            contentDescription = null,
            modifier = Modifier.size(Dimens.space60),
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(Dimens.space16))
        Text(strings.onboardingAccountBody, textAlign = TextAlign.Center)
        Spacer(Modifier.height(Dimens.space24))

        Button(onClick = { finish(onSignIn) }) { Text(strings.settingsSignIn) }
        Spacer(Modifier.height(Dimens.space8))
        TextButton(onClick = { finish(onSkip) }) { Text(strings.onboardingProtectionSkip) }
    }
}

@Preview
@Composable
private fun AccountOnboardingScreenLightPhonePreview() = DebtTrackerPreview(darkTheme = false) {
    AccountOnboardingScreen(onSignIn = {}, onSkip = {})
}

@Preview
@Composable
private fun AccountOnboardingScreenDarkPhonePreview() = DebtTrackerPreview(darkTheme = true) {
    AccountOnboardingScreen(onSignIn = {}, onSkip = {})
}

@Preview(device = DESKTOP)
@Composable
private fun AccountOnboardingScreenLightDesktopPreview() = DebtTrackerPreview(darkTheme = false) {
    AccountOnboardingScreen(onSignIn = {}, onSkip = {})
}

@Preview(device = DESKTOP)
@Composable
private fun AccountOnboardingScreenDarkDesktopPreview() = DebtTrackerPreview(darkTheme = true) {
    AccountOnboardingScreen(onSignIn = {}, onSkip = {})
}
