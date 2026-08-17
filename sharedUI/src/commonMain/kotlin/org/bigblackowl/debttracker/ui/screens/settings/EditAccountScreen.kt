package org.bigblackowl.debttracker.ui.screens.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Devices.DESKTOP
import androidx.compose.ui.tooling.preview.Preview
import org.bigblackowl.debttracker.core.i18n.LocalStrings
import org.bigblackowl.debttracker.core.media.rememberImagePicker
import org.bigblackowl.debttracker.domain.validation.isPhonePasteRelevant
import org.bigblackowl.debttracker.domain.validation.isValidFullName
import org.bigblackowl.debttracker.domain.validation.sanitizePhoneInput
import org.bigblackowl.debttracker.preview.DebtTrackerPreview
import org.bigblackowl.debttracker.theme.Dimens
import org.bigblackowl.debttracker.theme.debtAccentColors
import org.bigblackowl.debttracker.ui.components.AccountAvatar
import org.bigblackowl.debttracker.ui.components.LoadingButton
import org.bigblackowl.debttracker.ui.components.PasteableOutlinedTextField
import org.bigblackowl.debttracker.ui.components.SettingsDetailScaffold
import org.bigblackowl.debttracker.ui.components.UkrainianPhoneVisualTransformation
import org.bigblackowl.debttracker.ui.components.rememberClipboardText
import org.koin.compose.viewmodel.koinViewModel

/**
 * Account detail screen reached by tapping the account card in
 * [org.bigblackowl.debttracker.ui.screens.settings.SettingsScreen]'s Account section — avatar
 * upload plus name/phone edit all live here now instead of a separate menu row.
 */
@Composable
fun EditAccountScreen(
    onBack: () -> Unit,
    viewModel: EditAccountViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val strings = LocalStrings.current
    val clipboardText by rememberClipboardText()
    val imagePicker = rememberImagePicker()

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                EditAccountEffect.Saved -> onBack()
            }
        }
    }

    LaunchedEffect(state.error) {
        state.error?.let { snackbarHostState.showSnackbar(it) }
    }

    SettingsDetailScaffold(
        title = strings.editAccountTitle,
        onBack = onBack,
        snackbarHostState = snackbarHostState,
        useImePadding = true,
        verticalSpacing = Dimens.space12,
    ) {
        Spacer(Modifier.height(Dimens.space8))
        AccountAvatar(
            avatarUrl = state.avatarUrl,
            isUploading = state.isUploadingAvatar,
            onEditClick = {
                imagePicker.pickImage { picked ->
                    if (picked == null) return@pickImage
                    viewModel.onIntent(EditAccountIntent.AvatarPicked(picked))
                }
            },
        )
        AnimatedVisibility(
            visible = state.avatarError != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Text(
                state.avatarError.orEmpty(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.debtAccentColors.debt,
            )
        }
        Spacer(Modifier.height(Dimens.space8))

        PasteableOutlinedTextField(
            value = state.fullName,
            onValueChange = { viewModel.onIntent(EditAccountIntent.FullNameChanged(it)) },
            label = strings.fullName,
            leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null) },
            isError = state.fullNameError != null,
            supportingText = state.fullNameError,
            clipboardText = clipboardText,
            isPasteRelevant = ::isValidFullName,
        )

        OutlinedTextField(
            value = state.email,
            onValueChange = {},
            readOnly = true,
            enabled = false,
            label = { Text(strings.email) },
            leadingIcon = { Icon(Icons.Filled.Email, contentDescription = null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        PasteableOutlinedTextField(
            value = state.phone,
            onValueChange = { viewModel.onIntent(EditAccountIntent.PhoneChanged(sanitizePhoneInput(it))) },
            label = strings.phone,
            leadingIcon = { Icon(Icons.Filled.Phone, contentDescription = null) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            visualTransformation = remember { UkrainianPhoneVisualTransformation() },
            clipboardText = clipboardText,
            isPasteRelevant = ::isPhonePasteRelevant,
        )
        Spacer(Modifier.height(Dimens.space12))
        LoadingButton(
            onClick = { viewModel.onIntent(EditAccountIntent.Save) },
            isLoading = state.isSaving,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(strings.save) },
        )
    }
}

@Preview
@Composable
private fun EditAccountScreenLightPhonePreview() = DebtTrackerPreview(darkTheme = false) {
    EditAccountScreen(onBack = {})
}

@Preview
@Composable
private fun EditAccountScreenDarkPhonePreview() = DebtTrackerPreview(darkTheme = true) {
    EditAccountScreen(onBack = {})
}

@Preview(device = DESKTOP)
@Composable
private fun EditAccountScreenLightDesktopPreview() = DebtTrackerPreview(darkTheme = false) {
    EditAccountScreen(onBack = {})
}

@Preview(device = DESKTOP)
@Composable
private fun EditAccountScreenDarkDesktopPreview() = DebtTrackerPreview(darkTheme = true) {
    EditAccountScreen(onBack = {})
}
