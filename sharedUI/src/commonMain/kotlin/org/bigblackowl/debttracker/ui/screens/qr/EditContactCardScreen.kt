package org.bigblackowl.debttracker.ui.screens.qr

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Devices.DESKTOP
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.bigblackowl.debttracker.core.i18n.LocalStrings
import org.bigblackowl.debttracker.domain.validation.isPhonePasteRelevant
import org.bigblackowl.debttracker.domain.validation.isValidEmail
import org.bigblackowl.debttracker.domain.validation.isValidFullName
import org.bigblackowl.debttracker.domain.validation.sanitizePhoneInput
import org.bigblackowl.debttracker.preview.DebtTrackerPreview
import org.bigblackowl.debttracker.theme.Dimens
import org.bigblackowl.debttracker.ui.components.SettingsDetailScaffold
import org.bigblackowl.debttracker.ui.components.UnsavedChangesGuard
import org.bigblackowl.debttracker.ui.components.button.Button
import org.bigblackowl.debttracker.ui.components.form.PasteableOutlinedTextField
import org.bigblackowl.debttracker.ui.components.form.UkrainianPhoneVisualTransformation
import org.bigblackowl.debttracker.ui.components.form.rememberClipboardText
import org.koin.compose.viewmodel.koinViewModel

/**
 * The locally-saved "my card" (name/phone/email) that [QrHubScreen] encodes as a QR — reached from
 * there via its Edit button, signed-out only (a signed-in card comes straight from the account,
 * nothing to edit here). Explicit Save button; leaving with a pending edit is guarded by
 * [UnsavedChangesGuard] instead of losing it silently.
 */
@Composable
fun EditContactCardScreen(
    onBack: () -> Unit,
    viewModel: EditContactCardViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val strings = LocalStrings.current
    val clipboardText by rememberClipboardText()

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                EditContactCardEffect.Saved -> onBack()
            }
        }
    }

    UnsavedChangesGuard(
        hasUnsavedChanges = state.hasUnsavedChanges,
        onSave = { viewModel.onIntent(EditContactCardIntent.Save) },
        onDiscard = onBack,
    )

    SettingsDetailScaffold(
        title = strings.accountInfoEdit,
        onBack = onBack,
        useImePadding = true,
        verticalSpacing = Dimens.Spacing.md,
    ) {
        PasteableOutlinedTextField(
            value = state.name,
            onValueChange = { viewModel.onIntent(EditContactCardIntent.NameChanged(it)) },
            label = strings.fullName,
            clipboardText = clipboardText,
            isPasteRelevant = ::isValidFullName,
        )
        PasteableOutlinedTextField(
            value = state.phone,
            onValueChange = { viewModel.onIntent(EditContactCardIntent.PhoneChanged(sanitizePhoneInput(it))) },
            label = strings.phone,
            clipboardText = clipboardText,
            isPasteRelevant = ::isPhonePasteRelevant,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            visualTransformation = remember { UkrainianPhoneVisualTransformation() },
        )
        PasteableOutlinedTextField(
            value = state.email,
            onValueChange = { viewModel.onIntent(EditContactCardIntent.EmailChanged(it)) },
            label = strings.email,
            clipboardText = clipboardText,
            isPasteRelevant = ::isValidEmail,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
        )
        Spacer(Modifier.height(Dimens.Spacing.sm))
        Button(
            onClick = { viewModel.onIntent(EditContactCardIntent.Save) },
            modifier = Modifier.fillMaxWidth(),
        ) { Text(strings.save) }
    }
}

@Preview
@Composable
private fun EditContactCardScreenLightPhonePreview() = DebtTrackerPreview(darkTheme = false) {
    EditContactCardScreen(onBack = {})
}

@Preview
@Composable
private fun EditContactCardScreenDarkPhonePreview() = DebtTrackerPreview(darkTheme = true) {
    EditContactCardScreen(onBack = {})
}

@Preview(device = DESKTOP)
@Composable
private fun EditContactCardScreenLightDesktopPreview() = DebtTrackerPreview(darkTheme = false) {
    EditContactCardScreen(onBack = {})
}

@Preview(device = DESKTOP)
@Composable
private fun EditContactCardScreenDarkDesktopPreview() = DebtTrackerPreview(darkTheme = true) {
    EditContactCardScreen(onBack = {})
}
