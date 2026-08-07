package org.bigblackowl.debttracker.ui.screens.debtors

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Money
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Devices.DESKTOP
import androidx.compose.ui.tooling.preview.Preview
import org.bigblackowl.debttracker.core.i18n.LocalStrings
import org.bigblackowl.debttracker.domain.model.Currency
import org.bigblackowl.debttracker.domain.model.PaymentMethod
import org.bigblackowl.debttracker.domain.validation.sanitizeAmountInput
import org.bigblackowl.debttracker.preview.DebtTrackerPreview
import org.bigblackowl.debttracker.theme.Dimens
import org.bigblackowl.debttracker.ui.components.BackButton
import org.bigblackowl.debttracker.ui.components.ProfileSuggestionCard
import org.bigblackowl.debttracker.ui.components.UkrainianPhoneVisualTransformation
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/** Create/edit form for a [org.bigblackowl.debttracker.domain.model.Debtor] — `debtorId == null` means "new". */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditDebtorScreen(
    debtorId: String?,
    onDone: () -> Unit,
    viewModel: AddEditDebtorViewModel = koinViewModel { parametersOf(debtorId) },
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val strings = LocalStrings.current
    var currencyMenuExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                AddEditDebtorEffect.Saved -> onDone()
                is AddEditDebtorEffect.Error -> snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.isEditing) strings.addEditDebtorTitleEdit else strings.addEditDebtorTitleNew) },
                navigationIcon = { BackButton(onClick = onDone) },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(Dimens.space16)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier.width(Dimens.contentMaxWidth),
                verticalArrangement = Arrangement.spacedBy(Dimens.space12),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                OutlinedTextField(
                    value = state.fullName,
                    onValueChange = { viewModel.onIntent(AddEditDebtorIntent.FullNameChanged(it)) },
                    label = { Text(strings.fullName) },
                    placeholder = { Text(strings.fullNamePlaceholder) },
                    isError = state.fullNameError != null,
                    supportingText = { state.fullNameError?.let { Text(it) } },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = state.phone,
                    onValueChange = {
                        viewModel.onIntent(AddEditDebtorIntent.PhoneChanged(it.filter { c -> c.isDigit() }.take(10)))
                    },
                    label = { Text(strings.phone) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    visualTransformation = remember { UkrainianPhoneVisualTransformation() },
                )
                OutlinedTextField(
                    value = state.email,
                    onValueChange = { viewModel.onIntent(AddEditDebtorIntent.EmailChanged(it)) },
                    label = { Text(strings.email) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                )
                state.profileSuggestion?.let { suggestion ->
                    ProfileSuggestionCard(
                        suggestion = suggestion,
                        onUse = { viewModel.onIntent(AddEditDebtorIntent.ApplyProfileSuggestion) },
                        onDismiss = { viewModel.onIntent(AddEditDebtorIntent.DismissProfileSuggestion) },
                    )
                }
                OutlinedTextField(
                    value = state.comment,
                    onValueChange = { viewModel.onIntent(AddEditDebtorIntent.CommentChanged(it)) },
                    label = { Text(strings.comment) },
                    modifier = Modifier.fillMaxWidth(),
                )

                if (!state.isEditing) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Dimens.space5)
                    ) {
                        OutlinedTextField(
                            value = state.initialAmountText,
                            onValueChange = {
                                viewModel.onIntent(
                                    AddEditDebtorIntent.InitialAmountChanged(
                                        sanitizeAmountInput(it)
                                    )
                                )
                            },
                            modifier = Modifier.weight(1f),
                            label = { Text(strings.addEditDebtorInitialAmount) },
                            isError = state.amountError != null,
                            supportingText = { state.amountError?.let { Text(it) } },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                        ExposedDropdownMenuBox(
                            expanded = currencyMenuExpanded,
                            onExpandedChange = { currencyMenuExpanded = it },
                            modifier = Modifier.widthIn(max = Dimens.space120),
                        ) {
                            OutlinedTextField(
                                value = state.currency.symbol,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text(strings.currency) },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = currencyMenuExpanded) },
                                modifier = Modifier
                                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                            )
                            ExposedDropdownMenu(
                                expanded = currencyMenuExpanded,
                                onDismissRequest = { currencyMenuExpanded = false },
                            ) {
                                Currency.entries.forEach { currency ->
                                    DropdownMenuItem(
                                        text = { Text(currency.symbol) },
                                        onClick = {
                                            viewModel.onIntent(
                                                AddEditDebtorIntent.CurrencyChanged(
                                                    currency
                                                )
                                            )
                                            currencyMenuExpanded = false
                                        },
                                    )
                                }
                            }
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(
                            Dimens.space8,
                            alignment = Alignment.CenterHorizontally
                        )
                    ) {

                        FilterChip(
                            selected = state.method == PaymentMethod.CASH,
                            onClick = {
                                viewModel.onIntent(
                                    AddEditDebtorIntent.MethodChanged(
                                        PaymentMethod.CASH
                                    )
                                )
                            },
                            label = { Text(strings.cash) },
                            trailingIcon = { Icon(Icons.Default.Money, null) }

                        )
                        FilterChip(
                            selected = state.method == PaymentMethod.CARD,
                            onClick = {
                                viewModel.onIntent(
                                    AddEditDebtorIntent.MethodChanged(
                                        PaymentMethod.CARD
                                    )
                                )
                            },
                            label = { Text(strings.card) },
                            trailingIcon = { Icon(Icons.Default.CreditCard, null) }

                        )
                    }
                    if (state.method == PaymentMethod.CARD) {
                        OutlinedTextField(
                            value = state.cardLastDigits,
                            onValueChange = {
                                viewModel.onIntent(
                                    AddEditDebtorIntent.CardLastDigitsChanged(
                                        it
                                    )
                                )
                            },
                            label = { Text(strings.cardLastDigits) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }

            }
            Button(
                onClick = { viewModel.onIntent(AddEditDebtorIntent.Save) },
                enabled = !state.isSaving && !state.isLoading,
                modifier = Modifier.widthIn(max = Dimens.contentMaxWidth),
            ) {
                if (state.isSaving)
                    CircularProgressIndicator(modifier = Modifier.padding(end = Dimens.space8))
                else
                    Text(strings.save)
            }
        }
    }
}

@Preview
@Composable
private fun AddEditDebtorScreenPreview() = DebtTrackerPreview {
    AddEditDebtorScreen(debtorId = null, onDone = {})
}
@Preview(device = DESKTOP)
@Composable
private fun AddEditDebtorScreenPreview1() = DebtTrackerPreview {
    AddEditDebtorScreen(debtorId = null, onDone = {})
}