package org.bigblackowl.debttracker.ui.screens.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Devices.DESKTOP
import androidx.compose.ui.tooling.preview.Preview
import org.bigblackowl.debttracker.core.i18n.LocalStrings
import org.bigblackowl.debttracker.preview.DebtTrackerPreview
import org.bigblackowl.debttracker.theme.Dimens
import org.bigblackowl.debttracker.ui.components.BackButton
import org.koin.compose.viewmodel.koinViewModel

/** Account+Sync (спек §1.1) — email/пароль через supabase-kt Auth. */
@Composable
fun AuthScreen(
    onBack: () -> Unit,
    onAuthenticated: () -> Unit,
    showBackButton: Boolean = true,
    viewModel: AuthViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val strings = LocalStrings.current
    var passwordVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                AuthEffect.Success -> onAuthenticated()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.isSignUpMode) strings.authTitleSignUp else strings.authTitleSignIn) },
                navigationIcon = {
                    if (showBackButton) BackButton(onClick = onBack)
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(Dimens.space16),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier = Modifier.width(Dimens.contentMaxWidth),
                verticalArrangement = Arrangement.spacedBy(Dimens.space12),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                OutlinedTextField(
                    value = state.email,
                    onValueChange = { viewModel.onIntent(AuthIntent.EmailChanged(it)) },
                    label = { Text(strings.authEmail) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = state.password,
                    onValueChange = { viewModel.onIntent(AuthIntent.PasswordChanged(it)) },
                    label = { Text(strings.authPassword) },
                    singleLine = true,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    isError = state.error != null,
                    supportingText = { state.error?.let { Text(it) } },
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (passwordVisible) strings.hidePassword else strings.showPassword,
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(Dimens.space30))
                Button(
                    onClick = { viewModel.onIntent(AuthIntent.Submit) },
                    enabled = !state.isLoading,
                    modifier = Modifier.fillMaxWidth(.8f),
                ) {
                    if (state.isLoading)
                        CircularWavyProgressIndicator(modifier = Modifier.padding(end = Dimens.space8))
                    else
                        Text(if (state.isSignUpMode) strings.authSubmitSignUp else strings.authSubmitSignIn)
                }
                TextButton(onClick = { viewModel.onIntent(AuthIntent.ToggleMode) }) {
                    Text(if (state.isSignUpMode) strings.authToggleToSignIn else strings.authToggleToSignUp)
                }
            }
        }
    }
}

@Preview
@Composable
private fun AuthScreenPreview() = DebtTrackerPreview {
    AuthScreen(onBack = {}, onAuthenticated = {})
}

@Preview(device = DESKTOP)
@Composable
private fun AuthScreenPreview2() = DebtTrackerPreview {
    AuthScreen(onBack = {}, onAuthenticated = {})
}