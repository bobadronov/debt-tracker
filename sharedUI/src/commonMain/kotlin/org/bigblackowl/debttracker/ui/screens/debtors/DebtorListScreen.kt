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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.tooling.preview.Devices.DESKTOP
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.bigblackowl.debttracker.core.i18n.LocalStrings
import org.bigblackowl.debttracker.core.shortcuts.SearchFocusRequests
import org.bigblackowl.debttracker.domain.model.formatMoney
import org.bigblackowl.debttracker.domain.model.formatTotals
import org.bigblackowl.debttracker.preview.DebtTrackerPreview
import org.bigblackowl.debttracker.ui.components.contact.ContactListContent
import org.bigblackowl.debttracker.ui.components.contact.MenuOption
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

/** "Owed to me" tab: searchable/filterable/sortable list of debtors with an overflow-menu delete and a running total. */
@Composable
fun DebtorListScreen(
    onAddDebtor: () -> Unit,
    onOpenDebtor: (String) -> Unit,
    viewModel: DebtorListViewModel = koinViewModel(),
    searchFocusRequests: SearchFocusRequests = koinInject(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val searchFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        searchFocusRequests.events.collect { searchFocusRequester.requestFocus() }
    }

    DebtorListContent(
        state = state,
        searchFocusRequester = searchFocusRequester,
        onAddDebtor = onAddDebtor,
        onOpenDebtor = onOpenDebtor,
        onRefresh = { viewModel.onIntent(DebtorListIntent.Refresh) },
        onSearch = { viewModel.onIntent(DebtorListIntent.Search(it)) },
        onToggleSortDirection = { viewModel.onIntent(DebtorListIntent.ToggleSortDirection) },
        onChangeSort = { viewModel.onIntent(DebtorListIntent.ChangeSort(it)) },
        onChangeStatusFilter = { viewModel.onIntent(DebtorListIntent.ChangeStatusFilter(it)) },
        onDelete = { viewModel.onIntent(DebtorListIntent.Delete(it)) },
    )
}

@Composable
private fun DebtorListContent(
    state: DebtorListState,
    searchFocusRequester: FocusRequester,
    onAddDebtor: () -> Unit,
    onOpenDebtor: (String) -> Unit,
    onRefresh: () -> Unit,
    onSearch: (String) -> Unit,
    onToggleSortDirection: () -> Unit,
    onChangeSort: (DebtorSortOrder) -> Unit,
    onChangeStatusFilter: (DebtorStatusFilter) -> Unit,
    onDelete: (String) -> Unit,
) {
    val strings = LocalStrings.current

    ContactListContent(
        isLoading = state.isLoading,
        items = state.debtors,
        itemKey = { it.debtor.id },
        isRefreshing = state.isRefreshing,
        onRefresh = onRefresh,
        query = state.query,
        onQueryChange = onSearch,
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
        onToggleSortDirection = onToggleSortDirection,
        onChangeSort = onChangeSort,
        statusOptions = listOf(
            MenuOption(DebtorStatusFilter.ACTIVE, strings.debtorList.filterActive, Icons.Filled.HourglassEmpty),
            MenuOption(DebtorStatusFilter.CLOSED, strings.debtorList.filterClosed, Icons.Filled.CheckCircle),
            MenuOption(DebtorStatusFilter.ALL, strings.debtorList.filterAll, Icons.AutoMirrored.Filled.List),
        ),
        currentStatus = state.statusFilter,
        onChangeStatus = onChangeStatusFilter,
        totalLabel = strings.debtorList.total,
        totalText = state.totalsByCurrency.formatTotals(),
        onAdd = onAddDebtor,
        deleteLabel = strings.delete,
        itemId = { it.debtor.id },
        itemName = { it.debtor.fullName },
        itemPhone = { it.debtor.phone },
        itemAvatarUrl = { it.debtor.avatarUrl },
        itemBalanceText = { it.balance.formatMoney(it.debtor.currency) },
        onItemClick = { onOpenDebtor(it.debtor.id) },
        onItemDelete = { onDelete(it.debtor.id) },
    )
}

@Composable
private fun Preview(state: DebtorListState) = DebtorListContent(
    state = state,
    searchFocusRequester = remember { FocusRequester() },
    onAddDebtor = {},
    onOpenDebtor = {},
    onRefresh = {},
    onSearch = {},
    onToggleSortDirection = {},
    onChangeSort = {},
    onChangeStatusFilter = {},
    onDelete = {},
)

private val PREVIEW_NOW = kotlin.time.Instant.parse("2026-08-15T00:00:00Z")

private val PREVIEW_DEBTORS = listOf(
    org.bigblackowl.debttracker.domain.model.DebtorWithBalance(
        debtor = org.bigblackowl.debttracker.domain.model.Debtor(
            id = "d1", fullName = "Тарас Шевченко", phone = "0501234567", email = null, avatarUrl = null,
            comment = null, createdAt = PREVIEW_NOW, updatedAt = PREVIEW_NOW,
            status = org.bigblackowl.debttracker.domain.model.DebtStatus.ACTIVE,
            syncStatus = org.bigblackowl.debttracker.domain.model.SyncStatus.SYNCED,
        ),
        balance = com.ionspin.kotlin.bignum.decimal.BigDecimal.parseString("700"),
    ),
    org.bigblackowl.debttracker.domain.model.DebtorWithBalance(
        debtor = org.bigblackowl.debttracker.domain.model.Debtor(
            id = "d2", fullName = "Леся Українка", phone = null, email = "lesya@example.com", avatarUrl = null,
            comment = null, createdAt = PREVIEW_NOW, updatedAt = PREVIEW_NOW,
            status = org.bigblackowl.debttracker.domain.model.DebtStatus.ACTIVE,
            syncStatus = org.bigblackowl.debttracker.domain.model.SyncStatus.PENDING,
        ),
        balance = com.ionspin.kotlin.bignum.decimal.BigDecimal.parseString("1200"),
    ),
)

private val PREVIEW_STATE = DebtorListState(isLoading = false, debtors = PREVIEW_DEBTORS)

@Preview
@Composable
private fun DebtorListScreenLightPhonePreview() = DebtTrackerPreview(darkTheme = false) {
    Preview(PREVIEW_STATE)
}

@Preview
@Composable
private fun DebtorListScreenDarkPhonePreview() = DebtTrackerPreview(darkTheme = true) {
    Preview(PREVIEW_STATE)
}

@Preview(device = DESKTOP)
@Composable
private fun DebtorListScreenLightDesktopPreview() = DebtTrackerPreview(darkTheme = false) {
    Preview(PREVIEW_STATE)
}

@Preview(device = DESKTOP)
@Composable
private fun DebtorListScreenDarkDesktopPreview() = DebtTrackerPreview(darkTheme = true) {
    Preview(PREVIEW_STATE)
}

@Preview
@Composable
private fun DebtorListScreenLoadingPreview() = DebtTrackerPreview(darkTheme = false) {
    Preview(DebtorListState(isLoading = true))
}

@Preview
@Composable
private fun DebtorListScreenEmptyPreview() = DebtTrackerPreview(darkTheme = false) {
    Preview(DebtorListState(isLoading = false, debtors = emptyList()))
}

@Preview
@Composable
private fun DebtorListScreenRefreshingPreview() = DebtTrackerPreview(darkTheme = false) {
    Preview(PREVIEW_STATE.copy(isRefreshing = true))
}
