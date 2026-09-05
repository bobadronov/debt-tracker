package org.bigblackowl.debttracker.ui.screens.debtors

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

/** "Мені винні" tab: searchable/filterable/sortable list of debtors with an overflow-menu delete and a running total. */
@Composable
fun DebtorListScreen(
    onAddDebtor: () -> Unit,
    onOpenDebtor: (String) -> Unit,
    viewModel: DebtorListViewModel = koinViewModel(),
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
        items = state.debtors,
        key = { it.debtor.id },
        isRefreshing = state.isRefreshing,
        onRefresh = { viewModel.onIntent(DebtorListIntent.Refresh) },
        searchBar = {
            ListSearchBar(
                query = state.query,
                onQueryChange = { viewModel.onIntent(DebtorListIntent.Search(it)) },
                searchPlaceholder = strings.debtorList.searchPlaceholder,
                clearSearchDescription = strings.clearSearch,
                searchFocusRequester = searchFocusRequester,
                filterDescription = strings.debtorList.sort,
                sortOptions = listOf(
                    MenuOption(DebtorSortOrder.NAME_ASC, strings.debtorList.sortByName, Icons.Filled.SortByAlpha),
                    MenuOption(DebtorSortOrder.BALANCE_DESC, strings.debtorList.sortByBalance, Icons.Filled.Payments),
                    MenuOption(DebtorSortOrder.RECENT, strings.debtorList.sortRecent, Icons.Filled.History),
                ),
                currentSort = state.sortOrder,
                sortAscending = state.sortAscending,
                sortReverseDescription = strings.debtorList.sortReverse,
                onToggleSortDirection = { viewModel.onIntent(DebtorListIntent.ToggleSortDirection) },
                onChangeSort = { viewModel.onIntent(DebtorListIntent.ChangeSort(it)) },
                statusOptions = listOf(
                    MenuOption(DebtorStatusFilter.ACTIVE, strings.debtorList.filterActive, Icons.Filled.HourglassEmpty),
                    MenuOption(DebtorStatusFilter.CLOSED, strings.debtorList.filterClosed, Icons.Filled.CheckCircle),
                    MenuOption(DebtorStatusFilter.ALL, strings.debtorList.filterAll, Icons.AutoMirrored.Filled.List),
                ),
                currentStatus = state.statusFilter,
                onChangeStatus = { viewModel.onIntent(DebtorListIntent.ChangeStatusFilter(it)) },
            )
        },
        totalBar = {
            ListTotalBar(
                label = strings.debtorList.total,
                totalText = state.totalsByCurrency.formatTotals(),
                onAdd = onAddDebtor,
            )
        },
    ) { item ->
        ContactRow(
            id = item.debtor.id,
            name = item.debtor.fullName,
            phone = item.debtor.phone,
            avatarUrl = item.debtor.avatarUrl,
            balanceText = item.balance.formatMoney(item.debtor.currency),
            deleteLabel = strings.delete,
            onClick = { onOpenDebtor(item.debtor.id) },
            onDelete = { viewModel.onIntent(DebtorListIntent.Delete(item.debtor.id)) },
        )
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
