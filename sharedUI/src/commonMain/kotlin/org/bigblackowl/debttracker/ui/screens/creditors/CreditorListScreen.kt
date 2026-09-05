package org.bigblackowl.debttracker.ui.screens.creditors

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.tooling.preview.Devices.DESKTOP
import androidx.compose.ui.tooling.preview.Preview
import org.bigblackowl.debttracker.core.i18n.LocalStrings
import org.bigblackowl.debttracker.core.shortcuts.SearchFocusRequests
import org.bigblackowl.debttracker.domain.model.formatMoney
import org.bigblackowl.debttracker.domain.model.formatTotals
import org.bigblackowl.debttracker.preview.DebtTrackerPreview
import org.bigblackowl.debttracker.ui.components.ContactListScaffold
import org.bigblackowl.debttracker.ui.components.ContactRow
import org.bigblackowl.debttracker.ui.components.FullScreenLoadingIndicator
import org.bigblackowl.debttracker.ui.components.ListSearchBar
import org.bigblackowl.debttracker.ui.components.ListTotalBar
import org.bigblackowl.debttracker.ui.components.MenuOption
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

/** "Я винен" tab: searchable/filterable/sortable list of creditors with an overflow-menu delete and a running total. */
@Composable
fun CreditorListScreen(
    onAddCreditor: () -> Unit,
    onOpenCreditor: (String) -> Unit,
    viewModel: CreditorListViewModel = koinViewModel(),
    searchFocusRequests: SearchFocusRequests = koinInject(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val searchFocusRequester = remember { FocusRequester() }
    val strings = LocalStrings.current

    LaunchedEffect(Unit) {
        searchFocusRequests.events.collect { searchFocusRequester.requestFocus() }
    }

    if (state.isLoading) {
        FullScreenLoadingIndicator()
        return
    }

    ContactListScaffold(
        items = state.creditors,
        key = { it.creditor.id },
        isRefreshing = state.isRefreshing,
        onRefresh = { viewModel.onIntent(CreditorListIntent.Refresh) },
        searchBar = {
            ListSearchBar(
                query = state.query,
                onQueryChange = { viewModel.onIntent(CreditorListIntent.Search(it)) },
                searchPlaceholder = strings.creditorList.searchPlaceholder,
                clearSearchDescription = strings.clearSearch,
                searchFocusRequester = searchFocusRequester,
                filterDescription = strings.creditorList.sort,
                sortOptions = listOf(
                    MenuOption(CreditorSortOrder.NAME_ASC, strings.creditorList.sortByName, Icons.Filled.SortByAlpha),
                    MenuOption(CreditorSortOrder.BALANCE_DESC, strings.creditorList.sortByBalance, Icons.Filled.Payments),
                    MenuOption(CreditorSortOrder.RECENT, strings.creditorList.sortRecent, Icons.Filled.History),
                ),
                currentSort = state.sortOrder,
                sortAscending = state.sortAscending,
                sortReverseDescription = strings.creditorList.sortReverse,
                onToggleSortDirection = { viewModel.onIntent(CreditorListIntent.ToggleSortDirection) },
                onChangeSort = { viewModel.onIntent(CreditorListIntent.ChangeSort(it)) },
                statusOptions = listOf(
                    MenuOption(CreditorStatusFilter.ACTIVE, strings.creditorList.filterActive, Icons.Filled.HourglassEmpty),
                    MenuOption(CreditorStatusFilter.CLOSED, strings.creditorList.filterClosed, Icons.Filled.CheckCircle),
                    MenuOption(CreditorStatusFilter.ALL, strings.creditorList.filterAll, Icons.AutoMirrored.Filled.List),
                ),
                currentStatus = state.statusFilter,
                onChangeStatus = { viewModel.onIntent(CreditorListIntent.ChangeStatusFilter(it)) },
            )
        },
        totalBar = {
            ListTotalBar(
                label = strings.creditorList.total,
                totalText = state.totalsByCurrency.formatTotals(),
                onAdd = onAddCreditor,
            )
        },
    ) { item ->
        ContactRow(
            id = item.creditor.id,
            name = item.creditor.fullName,
            phone = item.creditor.phone,
            avatarUrl = item.creditor.avatarUrl,
            balanceText = item.balance.formatMoney(item.creditor.currency),
            deleteLabel = strings.delete,
            onClick = { onOpenCreditor(item.creditor.id) },
            onDelete = { viewModel.onIntent(CreditorListIntent.Delete(item.creditor.id)) },
        )
    }
}

@Preview
@Composable
private fun CreditorListScreenLightPhonePreview() = DebtTrackerPreview(darkTheme = false) {
    CreditorListScreen(onAddCreditor = {}, onOpenCreditor = {})
}

@Preview
@Composable
private fun CreditorListScreenDarkPhonePreview() = DebtTrackerPreview(darkTheme = true) {
    CreditorListScreen(onAddCreditor = {}, onOpenCreditor = {})
}

@Preview(device = DESKTOP)
@Composable
private fun CreditorListScreenLightDesktopPreview() = DebtTrackerPreview(darkTheme = false) {
    CreditorListScreen(onAddCreditor = {}, onOpenCreditor = {})
}

@Preview(device = DESKTOP)
@Composable
private fun CreditorListScreenDarkDesktopPreview() = DebtTrackerPreview(darkTheme = true) {
    CreditorListScreen(onAddCreditor = {}, onOpenCreditor = {})
}
