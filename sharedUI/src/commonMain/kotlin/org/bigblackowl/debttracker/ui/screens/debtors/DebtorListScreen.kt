package org.bigblackowl.debttracker.ui.screens.debtors

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
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
import org.bigblackowl.debttracker.domain.model.DebtorWithBalance
import org.bigblackowl.debttracker.domain.model.formatMoney
import org.bigblackowl.debttracker.domain.model.formatTotals
import org.bigblackowl.debttracker.preview.DebtTrackerPreview
import org.bigblackowl.debttracker.theme.Dimens
import org.bigblackowl.debttracker.theme.debtAccentColors
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

/** "Мені винні" tab: searchable/filterable/sortable list of debtors with an overflow-menu delete and a running total. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebtorListScreen(
    onAddDebtor: () -> Unit,
    onOpenDebtor: (String) -> Unit,
    viewModel: DebtorListViewModel = koinViewModel(),
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
            modifier = Modifier.width(Dimens.contentMaxWidth),
            verticalArrangement = Arrangement.spacedBy(Dimens.space5),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(Dimens.space12),
                horizontalArrangement = Arrangement.spacedBy(Dimens.space8),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = state.query,
                    onValueChange = { viewModel.onIntent(DebtorListIntent.Search(it)) },
                    modifier = Modifier.weight(1f).focusRequester(searchFocusRequester),
                    singleLine = true,
                    placeholder = { Text(strings.debtorListSearchPlaceholder) },
                    trailingIcon = if (state.query.isNotEmpty()) {
                        {
                            IconButton(onClick = { viewModel.onIntent(DebtorListIntent.Search("")) }) {
                                Icon(Icons.Filled.Close, strings.clearSearch)
                            }
                        }
                    } else null,
                )

                // All sort + status options live in one menu instead of separate chip rows.
                // Sort items are single-click toggles: tapping the active one flips its direction
                // (shown by the trailing arrow) instead of just re-selecting it.
                var filterMenuOpen by remember { mutableStateOf(false) }
                Box {
                    IconButton(onClick = { filterMenuOpen = true }) {
                        Icon(Icons.Filled.FilterList, strings.debtorListSort)
                    }
                    DropdownMenu(
                        expanded = filterMenuOpen,
                        onDismissRequest = { filterMenuOpen = false },
                    ) {
                        val sortOptions = listOf(
                            Triple(DebtorSortOrder.NAME_ASC, strings.debtorListSortByName, Icons.Filled.SortByAlpha),
                            Triple(DebtorSortOrder.BALANCE_DESC, strings.debtorListSortByBalance, Icons.Filled.Payments),
                            Triple(DebtorSortOrder.RECENT, strings.debtorListSortRecent, Icons.Filled.History),
                        )
                        sortOptions.forEach { (order, label, icon) ->
                            val isActive = state.sortOrder == order
                            DropdownMenuItem(
                                leadingIcon = { Icon(icon, null) },
                                text = { Text(label) },
                                trailingIcon = if (isActive) {
                                    {
                                        Icon(
                                            if (state.sortAscending) Icons.Filled.ArrowUpward else Icons.Filled.ArrowDownward,
                                            strings.debtorListSortReverse,
                                        )
                                    }
                                } else null,
                                onClick = {
                                    viewModel.onIntent(
                                        if (isActive) DebtorListIntent.ToggleSortDirection
                                        else DebtorListIntent.ChangeSort(order)
                                    )
                                    filterMenuOpen = false
                                },
                            )
                        }
                        HorizontalDivider()
                        val statusOptions = listOf(
                            Triple(DebtorStatusFilter.ACTIVE, strings.debtorListFilterActive, Icons.Filled.HourglassEmpty),
                            Triple(DebtorStatusFilter.CLOSED, strings.debtorListFilterClosed, Icons.Filled.CheckCircle),
                            Triple(DebtorStatusFilter.ALL, strings.debtorListFilterAll, Icons.AutoMirrored.Filled.List),
                        )
                        statusOptions.forEach { (filter, label, icon) ->
                            val isSelected = state.statusFilter == filter
                            DropdownMenuItem(
                                leadingIcon = { Icon(icon, null) },
                                text = { Text(label) },
                                trailingIcon = if (isSelected) {
                                    { Icon(Icons.Filled.Check, null) }
                                } else null,
                                onClick = {
                                    viewModel.onIntent(DebtorListIntent.ChangeStatusFilter(filter))
                                    filterMenuOpen = false
                                },
                            )
                        }
                    }
                }
            }

            PullToRefreshBox(
                isRefreshing = state.isRefreshing,
                onRefresh = { viewModel.onIntent(DebtorListIntent.Refresh) },
                modifier = Modifier.weight(1f),
            ) {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(state.debtors, key = { it.debtor.id }) { item ->
                        DebtorRow(
                            item = item,
                            onClick = { onOpenDebtor(item.debtor.id) },
                            onDelete = { viewModel.onIntent(DebtorListIntent.Delete(item.debtor.id)) },
                        )
                    }
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
                        Text(strings.debtorListTotal)
                        Text(
                            state.totalsByCurrency.formatTotals(),
                            color = MaterialTheme.debtAccentColors.debt
                        )
                    }
                    FloatingActionButton(
                        onClick = onAddDebtor,
                        modifier = Modifier.padding(start = Dimens.space12),
                    ) { Icon(Icons.Default.Add, null) }
                }
            }
        }
    }
}

@Preview
@Composable
private fun DebtorListScreenLightPhonePreview() = DebtTrackerPreview(darkTheme = false) {
    DebtorListScreen(onAddDebtor = {}, onOpenDebtor = {})
}

@Preview
@Composable
private fun DebtorListScreenDarkPhonePreview() = DebtTrackerPreview(darkTheme = true) {
    DebtorListScreen(onAddDebtor = {}, onOpenDebtor = {})
}

@Preview(device = DESKTOP)
@Composable
private fun DebtorListScreenLightDesktopPreview() = DebtTrackerPreview(darkTheme = false) {
    DebtorListScreen(onAddDebtor = {}, onOpenDebtor = {})
}

@Preview(device = DESKTOP)
@Composable
private fun DebtorListScreenDarkDesktopPreview() = DebtTrackerPreview(darkTheme = true) {
    DebtorListScreen(onAddDebtor = {}, onOpenDebtor = {})
}

@Composable
private fun DebtorRow(
    item: DebtorWithBalance,
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
                Text(item.debtor.fullName, style = MaterialTheme.typography.bodyLarge)
                item.debtor.phone?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    item.balance.formatMoney(item.debtor.currency),
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
