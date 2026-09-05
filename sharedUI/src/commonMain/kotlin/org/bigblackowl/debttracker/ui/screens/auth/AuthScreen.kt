package org.bigblackowl.debttracker.ui.screens.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Devices.DESKTOP
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import debt_tracker.sharedui.generated.resources.Res
import debt_tracker.sharedui.generated.resources.ic_google_logo
import org.bigblackowl.debttracker.BuildConfig
import org.bigblackowl.debttracker.core.i18n.LocalStrings
import org.bigblackowl.debttracker.core.media.rememberImagePicker
import org.bigblackowl.debttracker.domain.validation.isPhonePasteRelevant
import org.bigblackowl.debttracker.domain.validation.isValidEmail
import org.bigblackowl.debttracker.domain.validation.isValidFullName
import org.bigblackowl.debttracker.domain.validation.sanitizePhoneInput
import org.bigblackowl.debttracker.preview.DebtTrackerPreview
import org.bigblackowl.debttracker.theme.Dimens
import org.bigblackowl.debttracker.ui.components.AccountAvatar
import org.bigblackowl.debttracker.ui.components.BackTopAppBar
import org.bigblackowl.debttracker.ui.components.LoadingButton
import org.bigblackowl.debttracker.ui.components.PasteableOutlinedTextField
import org.bigblackowl.debttracker.ui.components.UkrainianPhoneVisualTransformation
import org.bigblackowl.debttracker.ui.components.rememberClipboardText
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel

/** Account+Sync (спек §1.1) — email/пароль через supabase-kt Auth; sign up also collects name/optional avatar+phone. */
@Composable
fun AuthScreen(
    onBack: () -> Unit,
    onAuthenticated: () -> Unit,
    showBackButton: Boolean = true,
    viewModel: AuthViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val strings = LocalStrings.current
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    val imagePicker = rememberImagePicker()

    val fullNameFocusRequester = remember { FocusRequester() }
    val emailFocusRequester = remember { FocusRequester() }
    val passwordFocusRequester = remember { FocusRequester() }
    val confirmPasswordFocusRequester = remember { FocusRequester() }
    val phoneFocusRequester = remember { FocusRequester() }
    val clipboardText by rememberClipboardText()

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                AuthEffect.Success -> onAuthenticated()
            }
        }
    }

    Scaffold(
        topBar = {
            BackTopAppBar(
                title = if (state.isSignUpMode) strings.auth.titleSignUp else strings.auth.titleSignIn,
                onBack = if (showBackButton) onBack else null,
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).imePadding()
                .verticalScroll(rememberScrollState()).padding(Dimens.space16),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier = Modifier.width(Dimens.contentMaxWidth),
                verticalArrangement = Arrangement.spacedBy(Dimens.space12),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {

                SignUpOnlyFields(visible = state.isSignUpMode) {
                    AccountAvatar(
                        localImageBytes = state.avatarPicked?.bytes,
                        onEditClick = {
                            imagePicker.pickImage { picked ->
                                if (picked == null) return@pickImage
                                viewModel.onIntent(AuthIntent.AvatarPicked(picked))
                            }
                        },
                    )

                    PasteableOutlinedTextField(
                        value = state.fullName,
                        onValueChange = { viewModel.onIntent(AuthIntent.FullNameChanged(it)) },
                        label = strings.fullName,
                        clipboardText = clipboardText,
                        isPasteRelevant = ::isValidFullName,
                        modifier = Modifier.focusRequester(fullNameFocusRequester),
                        isError = state.fullNameError != null,
                        supportingText = state.fullNameError,
                        leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { emailFocusRequester.requestFocus() }),
                    )
                }

                PasteableOutlinedTextField(
                    value = state.email,
                    onValueChange = {
                        viewModel.onIntent(AuthIntent.EmailChanged(it))
                    },
                    label = strings.auth.email,
                    clipboardText = clipboardText,
                    isPasteRelevant = ::isValidEmail,
                    modifier = Modifier
                        .focusRequester(emailFocusRequester)
                        .semantics { contentType = ContentType.Username },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next,
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { passwordFocusRequester.requestFocus() },
                    ),
                )

                OutlinedTextField(
                    value = state.password,
                    onValueChange = {
                        viewModel.onIntent(AuthIntent.PasswordChanged(it))
                    },
                    label = { Text(strings.auth.password) },
                    singleLine = true,
                    visualTransformation = if (passwordVisible) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = if (state.isSignUpMode) ImeAction.Next else ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { confirmPasswordFocusRequester.requestFocus() },
                        onDone = {
                            if (!state.isLoading) {
                                viewModel.onIntent(AuthIntent.Submit)
                            }
                        },
                    ),
                    isError = state.error != null || state.passwordError != null,
                    supportingText = {
                        (state.passwordError ?: state.error)?.let { Text(it) }
                    },
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                passwordVisible = !passwordVisible
                            },
                        ) {
                            Icon(
                                imageVector = if (passwordVisible) {
                                    Icons.Default.VisibilityOff
                                } else {
                                    Icons.Default.Visibility
                                },
                                contentDescription = if (passwordVisible) {
                                    strings.auth.hidePassword
                                } else {
                                    strings.auth.showPassword
                                },
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(passwordFocusRequester)
                        .semantics { contentType = if (state.isSignUpMode) ContentType.NewPassword else ContentType.Password },
                )

                // After a failed sign-in — Supabase Auth can't tell an unknown email from a wrong
                // password (anti-enumeration), so this covers both by inviting the user to register.
                AnimatedVisibility(visible = state.offerRegistration && !state.isSignUpMode) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(strings.authExtra.offerSignUpPrompt)
                        TextButton(onClick = { viewModel.onIntent(AuthIntent.SwitchToSignUp) }) {
                            Text(strings.authExtra.offerSignUpAction)
                        }
                    }
                }

                SignUpOnlyFields(visible = state.isSignUpMode) {
                    OutlinedTextField(
                        value = state.confirmPassword,
                        onValueChange = { viewModel.onIntent(AuthIntent.ConfirmPasswordChanged(it)) },
                        label = { Text(strings.auth.confirmPassword) },
                        singleLine = true,
                        visualTransformation = if (confirmPasswordVisible) {
                            VisualTransformation.None
                        } else {
                            PasswordVisualTransformation()
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Next,
                        ),
                        keyboardActions = KeyboardActions(onNext = { phoneFocusRequester.requestFocus() }),
                        isError = state.confirmPasswordError != null,
                        supportingText = { state.confirmPasswordError?.let { Text(it) } },
                        trailingIcon = {
                            IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                                Icon(
                                    imageVector = if (confirmPasswordVisible) {
                                        Icons.Default.VisibilityOff
                                    } else {
                                        Icons.Default.Visibility
                                    },
                                    contentDescription = if (confirmPasswordVisible) {
                                        strings.auth.hidePassword
                                    } else {
                                        strings.auth.showPassword
                                    },
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(confirmPasswordFocusRequester)
                            .semantics { contentType = ContentType.NewPassword },
                    )

                    PasteableOutlinedTextField(
                        value = state.phone,
                        onValueChange = { viewModel.onIntent(AuthIntent.PhoneChanged(sanitizePhoneInput(it))) },
                        label = strings.phone,
                        clipboardText = clipboardText,
                        isPasteRelevant = ::isPhonePasteRelevant,
                        modifier = Modifier.focusRequester(phoneFocusRequester).semantics { contentType = ContentType.PhoneNumber },
                        leadingIcon = { Icon(Icons.Filled.Phone, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Phone,
                            imeAction = ImeAction.Done,
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                if (!state.isLoading) {
                                    viewModel.onIntent(AuthIntent.Submit)
                                }
                            },
                        ),
                        visualTransformation = remember { UkrainianPhoneVisualTransformation() },
                    )
                }

                Spacer(Modifier.height(Dimens.space30))
                LoadingButton(
                    onClick = { viewModel.onIntent(AuthIntent.Submit) },
                    isLoading = state.isLoading,
                    enabled = !state.isGoogleLoading,
                    modifier = Modifier.fillMaxWidth(.8f),
                    label = { Text(if (state.isSignUpMode) strings.auth.submitSignUp else strings.auth.submitSignIn) },
                )

                if (BuildConfig.GOOGLE_SIGN_IN_ENABLED) {
                    Row(
                        modifier = Modifier.fillMaxWidth(.8f).padding(vertical = Dimens.space8),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Dimens.space12),
                    ) {
                        HorizontalDivider(modifier = Modifier.weight(1f))
                        Text(strings.authExtra.divider)
                        HorizontalDivider(modifier = Modifier.weight(1f))
                    }
                    OutlinedButton(
                        onClick = { viewModel.onIntent(AuthIntent.GoogleSignIn) },
                        enabled = !state.isLoading && !state.isGoogleLoading,
                        modifier = Modifier.fillMaxWidth(.8f),
                    ) {
                        if (state.isGoogleLoading) {
                            CircularWavyProgressIndicator(
                                modifier = Modifier.size(Dimens.space20),
                            )
                        } else {
                            Image(
                                painter = painterResource(Res.drawable.ic_google_logo),
                                contentDescription = null,
                                modifier = Modifier.size(Dimens.space20),
                            )
                            Spacer(Modifier.width(Dimens.space12))
                            Text(strings.authExtra.continueWithGoogle)
                        }
                    }

                    val googleError = state.googleError
                    AnimatedVisibility(visible = googleError != null) {
                        Text(
                            text = googleError.orEmpty(),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.fillMaxWidth(.8f).padding(top = Dimens.space8),
                        )
                    }
                }

                TextButton(onClick = { viewModel.onIntent(AuthIntent.ToggleMode) }) {
                    Text(if (state.isSignUpMode) strings.auth.toggleToSignIn else strings.auth.toggleToSignUp)
                }
            }
        }
    }
}

/** Wraps the sign-up-only fields shared by both halves of the form (around the always-visible password field). */
@Composable
private fun SignUpOnlyFields(visible: Boolean, content: @Composable ColumnScope.() -> Unit) {
    AnimatedVisibility(visible = visible) {
        Column(
            verticalArrangement = Arrangement.spacedBy(Dimens.space12),
            horizontalAlignment = Alignment.CenterHorizontally,
            content = content,
        )
    }
}

@Preview
@Composable
private fun AuthScreenLightPhonePreview() = DebtTrackerPreview(darkTheme = false) {
    AuthScreen(onBack = {}, onAuthenticated = {})
}

@Preview
@Composable
private fun AuthScreenDarkPhonePreview() = DebtTrackerPreview(darkTheme = true) {
    AuthScreen(onBack = {}, onAuthenticated = {})
}

@Preview(device = DESKTOP)
@Composable
private fun AuthScreenLightDesktopPreview() = DebtTrackerPreview(darkTheme = false) {
    AuthScreen(onBack = {}, onAuthenticated = {})
}

@Preview(device = DESKTOP)
@Composable
private fun AuthScreenDarkDesktopPreview() = DebtTrackerPreview(darkTheme = true) {
    AuthScreen(onBack = {}, onAuthenticated = {})
}
