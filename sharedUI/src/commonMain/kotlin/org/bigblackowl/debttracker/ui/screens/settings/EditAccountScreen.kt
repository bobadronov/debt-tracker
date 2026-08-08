package org.bigblackowl.debttracker.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularWavyProgressIndicator
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Devices.DESKTOP
import androidx.compose.ui.tooling.preview.Preview
import org.bigblackowl.debttracker.core.i18n.LocalStrings
import org.bigblackowl.debttracker.domain.validation.isValidFullName
import org.bigblackowl.debttracker.preview.DebtTrackerPreview
import org.bigblackowl.debttracker.theme.Dimens
import org.bigblackowl.debttracker.ui.components.BackButton
import org.bigblackowl.debttracker.ui.components.ClipboardPasteHint
import org.bigblackowl.debttracker.ui.components.UkrainianPhoneVisualTransformation
import org.bigblackowl.debttracker.ui.components.rememberClipboardText
import org.koin.compose.viewmodel.koinViewModel

/** Edit-my-account form reached from [org.bigblackowl.debttracker.ui.screens.settings.SettingsScreen]'s Account section. */
@Composable
fun EditAccountScreen(
    onBack: () -> Unit,
    viewModel: EditAccountViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val strings = LocalStrings.current
    val clipboardText by rememberClipboardText()

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
            modifier = Modifier.fillMaxSize().padding(padding).padding(Dimens.space16),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier = Modifier.width(Dimens.contentMaxWidth),
                verticalArrangement = Arrangement.spacedBy(Dimens.space12),
            ) {
                OutlinedTextField(
                    value = state.email,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(strings.email) },
                    supportingText = { Text(strings.editAccountEmailReadOnly) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = state.fullName,
                    onValueChange = { viewModel.onIntent(EditAccountIntent.FullNameChanged(it)) },
                    label = { Text(strings.fullName) },
                    isError = state.fullNameError != null,
                    supportingText = { state.fullNameError?.let { Text(it) } },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                ClipboardPasteHint(
                    clipboardText = clipboardText,
                    fieldValue = state.fullName,
                    isRelevant = ::isValidFullName,
                    onPaste = { viewModel.onIntent(EditAccountIntent.FullNameChanged(it)) },
                )
                OutlinedTextField(
                    value = state.phone,
                    onValueChange = {
                        viewModel.onIntent(EditAccountIntent.PhoneChanged(it.filter(Char::isDigit).take(10)))
                    },
                    label = { Text(strings.phone) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    visualTransformation = remember { UkrainianPhoneVisualTransformation() },
                    modifier = Modifier.fillMaxWidth(),
                )
                ClipboardPasteHint(
                    clipboardText = clipboardText,
                    fieldValue = state.phone,
                    isRelevant = { it.filter(Char::isDigit).length >= 9 },
                    onPaste = {
                        viewModel.onIntent(EditAccountIntent.PhoneChanged(it.filter(Char::isDigit).take(10)))
                    },
                )

                Button(
                    onClick = { viewModel.onIntent(EditAccountIntent.Save) },
                    enabled = !state.isSaving,
                    modifier = Modifier.fillMaxWidth(),
                ) {
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

@Preview
@Composable
private fun EditAccountScreenPreview() = DebtTrackerPreview {
    EditAccountScreen(onBack = {})
}

@Preview(device = DESKTOP)
@Composable
private fun EditAccountScreenPreview2() = DebtTrackerPreview {
    EditAccountScreen(onBack = {})
}
