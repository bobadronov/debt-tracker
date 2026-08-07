package org.bigblackowl.debttracker.ui.screens.creditors

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.tooling.preview.Devices.DESKTOP
import androidx.compose.ui.tooling.preview.Preview
import org.bigblackowl.debttracker.core.i18n.LocalStrings
import org.bigblackowl.debttracker.core.settings.AppSettings
import org.bigblackowl.debttracker.core.shortcuts.SearchFocusRequests
import org.bigblackowl.debttracker.domain.model.CreditorWithBalance
import org.bigblackowl.debttracker.domain.model.formatMoney
import org.bigblackowl.debttracker.domain.model.formatTotals
import org.bigblackowl.debttracker.preview.DebtTrackerPreview
import org.bigblackowl.debttracker.theme.Dimens
import org.bigblackowl.debttracker.theme.debtAccentColors
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

/** "Я винен" tab: searchable/filterable/sortable list of creditors with an overflow-menu delete and a running total. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreditorListScreen(
    onAddCreditor: () -> Unit,
    onOpenCreditor: (String) -> Unit,
    viewModel: CreditorListViewModel = koinViewModel(),
    searchFocusRequests: SearchFocusRequests = koinInject(),
) {
    val state by viewModel.state.collectAsState()
    val searchFocusRequester = remember { FocusRequester() }
    val strings = LocalStrings.current

    LaunchedEffect(Unit) {
        searchFocusRequests.events.collect { searchFocusRequester.requestFocus() }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            modifier = Modifier.width(Dimens.contentMaxWidth).fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(Dimens.space5),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(Dimens.space12),
                horizontalArrangement = Arrangement.spacedBy(Dimens.space8),
            ) {
                OutlinedTextField(
                    value = state.query,
                    onValueChange = { viewModel.onIntent(CreditorListIntent.Search(it)) },
                    modifier = Modifier.weight(1f).focusRequester(searchFocusRequester),
                    singleLine = true,
                    placeholder = { Text(strings.creditorListSearchPlaceholder) },
                )
                var sortMenuOpen by remember { mutableStateOf(false) }
                Box {
                    IconButton(onClick = {
                        sortMenuOpen = true
                    }) { Icon(Icons.AutoMirrored.Filled.Sort, null) }
                    DropdownMenu(
                        expanded = sortMenuOpen,
                        onDismissRequest = { sortMenuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text(strings.creditorListSortByName) },
                            onClick = {
                                viewModel.onIntent(
                                    CreditorListIntent.ChangeSort(
                                        CreditorSortOrder.NAME_ASC
                                    )
                                ); sortMenuOpen = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(strings.creditorListSortByBalance) },
                            onClick = {
                                viewModel.onIntent(
                                    CreditorListIntent.ChangeSort(
                                        CreditorSortOrder.BALANCE_DESC
                                    )
                                ); sortMenuOpen = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(strings.creditorListSortRecent) },
                            onClick = {
                                viewModel.onIntent(
                                    CreditorListIntent.ChangeSort(
                                        CreditorSortOrder.RECENT
                                    )
                                ); sortMenuOpen = false
                            }
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = Dimens.space12),
                horizontalArrangement = Arrangement.spacedBy(Dimens.space8),
            ) {
                FilterChip(
                    selected = state.statusFilter == CreditorStatusFilter.ACTIVE,
                    onClick = {
                        viewModel.onIntent(
                            CreditorListIntent.ChangeStatusFilter(
                                CreditorStatusFilter.ACTIVE
                            )
                        )
                    },
                    label = { Text(strings.creditorListFilterActive) },
                )
                FilterChip(
                    selected = state.statusFilter == CreditorStatusFilter.CLOSED,
                    onClick = {
                        viewModel.onIntent(
                            CreditorListIntent.ChangeStatusFilter(
                                CreditorStatusFilter.CLOSED
                            )
                        )
                    },
                    label = { Text(strings.creditorListFilterClosed) },
                )
                FilterChip(
                    selected = state.statusFilter == CreditorStatusFilter.ALL,
                    onClick = {
                        viewModel.onIntent(
                            CreditorListIntent.ChangeStatusFilter(
                                CreditorStatusFilter.ALL
                            )
                        )
                    },
                    label = { Text(strings.creditorListFilterAll) },
                )
            }

            LazyColumn(modifier = Modifier.weight(1f)) {
                items(state.creditors, key = { it.creditor.id }) { item ->
                    CreditorRow(
                        item = item,
                        onClick = { onOpenCreditor(item.creditor.id) },
                        onDelete = { viewModel.onIntent(CreditorListIntent.Delete(item.creditor.id)) },
                    )
                }
            }

            Card(modifier = Modifier.fillMaxWidth().padding(Dimens.space12)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(Dimens.space16),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(strings.creditorListTotal)
                        Text(
                            state.totalsByCurrency.formatTotals(),
                            color = MaterialTheme.debtAccentColors.debt
                        )
                    }
                    FloatingActionButton(
                        onClick = onAddCreditor,
                        modifier = Modifier.padding(start = Dimens.space12),
                    ) { Icon(Icons.Default.Add, null) }
                }
            }
        }
    }
}

@Preview
@Composable
private fun CreditorListScreenPreview() = DebtTrackerPreview {
    CreditorListScreen(onAddCreditor = {}, onOpenCreditor = {})
}

@Preview(device = DESKTOP)
@Composable
private fun CreditorListScreenPreview2() = DebtTrackerPreview {
    CreditorListScreen(onAddCreditor = {}, onOpenCreditor = {})
}


@Composable
private fun CreditorRow(
    item: CreditorWithBalance,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    val appSettings = koinInject<AppSettings>()
    val haptics = LocalHapticFeedback.current
    var menuOpen by remember { mutableStateOf(false) }

    OutlinedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(horizontal = Dimens.space12, vertical = Dimens.space4),
        border = BorderStroke(Dimens.space1, MaterialTheme.colorScheme.primary),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = Dimens.space16, top = Dimens.space16, bottom = Dimens.space16, end = Dimens.space4),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(item.creditor.fullName, style = MaterialTheme.typography.bodyLarge)
                item.creditor.phone?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    item.balance.formatMoney(item.creditor.currency),
                    color = MaterialTheme.debtAccentColors.debt
                )
                Box {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = null)
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text(LocalStrings.current.delete) },
                            leadingIcon = { Icon(Icons.Default.DeleteOutline, contentDescription = null) },
                            onClick = {
                                menuOpen = false
                                if (appSettings.hapticEnabled) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                onDelete()
                            },
                        )
                    }
                }
            }
        }
    }
}
