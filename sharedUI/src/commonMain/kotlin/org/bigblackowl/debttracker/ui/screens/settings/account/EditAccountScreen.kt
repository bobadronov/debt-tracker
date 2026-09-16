package org.bigblackowl.debttracker.ui.screens.settings.account

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Devices.DESKTOP
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.bigblackowl.debttracker.core.i18n.LocalStrings
import org.bigblackowl.debttracker.core.media.rememberImagePicker
import org.bigblackowl.debttracker.domain.validation.sanitizePhoneInput
import org.bigblackowl.debttracker.preview.DebtTrackerPreview
import org.bigblackowl.debttracker.theme.Dimens
import org.bigblackowl.debttracker.theme.debtAccentColors
import org.bigblackowl.debttracker.ui.components.AccountAvatar
import org.bigblackowl.debttracker.ui.components.UnsavedChangesGuard
import org.bigblackowl.debttracker.ui.components.button.LoadingButton
import org.bigblackowl.debttracker.ui.components.form.UkrainianPhoneVisualTransformation
import org.bigblackowl.debttracker.ui.components.settings.SettingsDetailScaffold
import org.bigblackowl.debttracker.ui.components.text.CaptionText
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

    EditAccountContent(
        state = state,
        snackbarHostState = snackbarHostState,
        onBack = onBack,
        onPickAvatar = {
            imagePicker.pickImage { picked ->
                if (picked == null) return@pickImage
                viewModel.onIntent(EditAccountIntent.AvatarPicked(picked))
            }
        },
        onFullNameChange = { viewModel.onIntent(EditAccountIntent.FullNameChanged(it)) },
        onPhoneChange = { viewModel.onIntent(EditAccountIntent.PhoneChanged(sanitizePhoneInput(it))) },
        onSave = { viewModel.onIntent(EditAccountIntent.Save) },
    )
}

@Composable
private fun EditAccountContent(
    state: EditAccountState,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onPickAvatar: () -> Unit,
    onFullNameChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit,
    onSave: () -> Unit,
) {
    val strings = LocalStrings.current

    UnsavedChangesGuard(
        hasUnsavedChanges = state.hasUnsavedChanges,
        onSave = onSave,
        onDiscard = onBack,
    )

    SettingsDetailScaffold(
        title = strings.editAccountTitle,
        onBack = onBack,
        snackbarHostState = snackbarHostState,
        useImePadding = true,
        verticalSpacing = Dimens.Spacing.md,
    ) {
        Spacer(Modifier.height(Dimens.Spacing.sm))
        AccountAvatar(
            avatarUrl = state.avatarUrl,
            isUploading = state.isUploadingAvatar,
            onEditClick = onPickAvatar,
        )
        AnimatedVisibility(
            visible = state.avatarError != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            CaptionText(
                state.avatarError.orEmpty(),
                color = MaterialTheme.debtAccentColors.debt,
            )
        }
        Spacer(Modifier.height(Dimens.Spacing.sm))

        OutlinedTextField(
            value = state.fullName,
            onValueChange = onFullNameChange,
            label = { Text(strings.fullName) },
            isError = state.fullNameError != null,
            supportingText = state.fullNameError?.let { { Text(it) } },
            leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
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

        OutlinedTextField(
            value = state.phone,
            onValueChange = onPhoneChange,
            label = { Text(strings.phone) },
            leadingIcon = { Icon(Icons.Filled.Phone, contentDescription = null) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            visualTransformation = remember { UkrainianPhoneVisualTransformation() },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(Dimens.Spacing.md))
        LoadingButton(
            onClick = onSave,
            isLoading = state.isSaving,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(strings.save) },
        )
    }
}

@Composable
private fun Preview(state: EditAccountState) = EditAccountContent(
    state = state,
    snackbarHostState = remember { SnackbarHostState() },
    onBack = {},
    onPickAvatar = {},
    onFullNameChange = {},
    onPhoneChange = {},
    onSave = {},
)

private val PREVIEW_STATE = EditAccountState(fullName = "Тарас Шевченко", email = "taras@example.com", phone = "0501234567")

@Preview
@Composable
private fun EditAccountScreenLightPhonePreview() = DebtTrackerPreview(darkTheme = false) {
    Preview(PREVIEW_STATE)
}

@Preview
@Composable
private fun EditAccountScreenDarkPhonePreview() = DebtTrackerPreview(darkTheme = true) {
    Preview(PREVIEW_STATE)
}

@Preview(device = DESKTOP)
@Composable
private fun EditAccountScreenLightDesktopPreview() = DebtTrackerPreview(darkTheme = false) {
    Preview(PREVIEW_STATE)
}

@Preview(device = DESKTOP)
@Composable
private fun EditAccountScreenDarkDesktopPreview() = DebtTrackerPreview(darkTheme = true) {
    Preview(PREVIEW_STATE)
}

@Preview
@Composable
private fun EditAccountScreenSavingPreview() = DebtTrackerPreview(darkTheme = false) {
    Preview(PREVIEW_STATE.copy(isSaving = true))
}

@Preview
@Composable
private fun EditAccountScreenUploadingAvatarPreview() = DebtTrackerPreview(darkTheme = false) {
    Preview(PREVIEW_STATE.copy(isUploadingAvatar = true))
}

@Preview
@Composable
private fun EditAccountScreenErrorPreview() = DebtTrackerPreview(darkTheme = false) {
    Preview(PREVIEW_STATE.copy(fullNameError = "Введіть ім'я", avatarError = "Не вдалося завантажити фото"))
}
