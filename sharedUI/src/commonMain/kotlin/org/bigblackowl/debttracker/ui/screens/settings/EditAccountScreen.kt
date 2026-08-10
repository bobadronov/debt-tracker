package org.bigblackowl.debttracker.ui.screens.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Devices.DESKTOP
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.launch
import org.bigblackowl.debttracker.core.i18n.LocalStrings
import org.bigblackowl.debttracker.core.media.rememberImagePicker
import org.bigblackowl.debttracker.domain.repository.AuthRepository
import org.bigblackowl.debttracker.domain.validation.isValidFullName
import org.bigblackowl.debttracker.preview.DebtTrackerPreview
import org.bigblackowl.debttracker.theme.Dimens
import org.bigblackowl.debttracker.theme.debtAccentColors
import org.bigblackowl.debttracker.ui.components.AccountAvatar
import org.bigblackowl.debttracker.ui.components.BackButton
import org.bigblackowl.debttracker.ui.components.ClipboardPasteHint
import org.bigblackowl.debttracker.ui.components.UkrainianPhoneVisualTransformation
import org.bigblackowl.debttracker.ui.components.rememberClipboardText
import org.koin.compose.koinInject
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
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val strings = LocalStrings.current
    val clipboardText by rememberClipboardText()
    val authRepository = koinInject<AuthRepository>()
    val imagePicker = rememberImagePicker()
    val scope = rememberCoroutineScope()
    val avatarUrl by authRepository.avatarUrl.collectAsState()
    var isUploadingAvatar by remember { mutableStateOf(false) }
    var avatarError by remember { mutableStateOf<String?>(null) }

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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(strings.editAccountTitle) },
                navigationIcon = { BackButton(onClick = onBack) },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).imePadding()
                .verticalScroll(rememberScrollState()).padding(Dimens.space16),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier = Modifier.width(Dimens.contentMaxWidth),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Dimens.space12),
            ) {
                Spacer(Modifier.height(Dimens.space8))
                AccountAvatar(
                    avatarUrl = avatarUrl,
                    isUploading = isUploadingAvatar,
                    onEditClick = {
                        imagePicker.pickImage { picked ->
                            if (picked == null) return@pickImage
                            scope.launch {
                                isUploadingAvatar = true
                                avatarError = null
                                authRepository.updateAvatar(picked.bytes, picked.fileExtension)
                                    .onFailure { avatarError = strings.settingsAvatarUploadError }
                                isUploadingAvatar = false
                            }
                        }
                    },
                )
                AnimatedVisibility(
                    visible = avatarError != null,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Text(
                        avatarError.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.debtAccentColors.debt,
                    )
                }
                Spacer(Modifier.height(Dimens.space8))

                var fullNameFocused by remember { mutableStateOf(false) }
                OutlinedTextField(
                    value = state.fullName,
                    onValueChange = { viewModel.onIntent(EditAccountIntent.FullNameChanged(it)) },
                    label = { Text(strings.fullName) },
                    leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null) },
                    isError = state.fullNameError != null,
                    supportingText = { state.fullNameError?.let { Text(it) } },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().onFocusChanged { fullNameFocused = it.isFocused },
                )

                ClipboardPasteHint(
                    clipboardText = clipboardText,
                    fieldValue = state.fullName,
                    isFieldFocused = fullNameFocused,
                    isRelevant = ::isValidFullName,
                    onPaste = { viewModel.onIntent(EditAccountIntent.FullNameChanged(it)) },
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


                var phoneFocused by remember { mutableStateOf(false) }
                OutlinedTextField(
                    value = state.phone,
                    onValueChange = {
                        viewModel.onIntent(
                            EditAccountIntent.PhoneChanged(
                                it.filter(Char::isDigit).take(10)
                            )
                        )
                    },
                    label = { Text(strings.phone) },
                    leadingIcon = { Icon(Icons.Filled.Phone, contentDescription = null) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    visualTransformation = remember { UkrainianPhoneVisualTransformation() },
                    modifier = Modifier.fillMaxWidth().onFocusChanged { phoneFocused = it.isFocused },
                )
                ClipboardPasteHint(
                    clipboardText = clipboardText,
                    fieldValue = state.phone,
                    isFieldFocused = phoneFocused,
                    isRelevant = { it.filter(Char::isDigit).length >= 9 },
                    onPaste = {
                        viewModel.onIntent(
                            EditAccountIntent.PhoneChanged(
                                it.filter(Char::isDigit).take(10)
                            )
                        )
                    },
                )
                Spacer(Modifier.height(Dimens.space12))
                Button(
                    onClick = { viewModel.onIntent(EditAccountIntent.Save) },
                    enabled = !state.isSaving,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Crossfade(state.isSaving) {
                        if (state.isSaving) {
                            CircularWavyProgressIndicator(modifier = Modifier.padding(end = Dimens.space8))
                        } else {
                            Text(strings.save)
                        }
                    }
                }
            }
        }
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
