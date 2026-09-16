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

/** "I owe" tab: searchable/filterable/sortable list of creditors with an overflow-menu delete and a running total. */
@Composable
fun CreditorListScreen(
    onAddCreditor: () -> Unit,
    onOpenCreditor: (String) -> Unit,
    viewModel: CreditorListViewModel = koinViewModel(),
    searchFocusRequests: SearchFocusRequests = koinInject(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val searchFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        searchFocusRequests.events.collect { searchFocusRequester.requestFocus() }
    }

    CreditorListContent(
        state = state,
        searchFocusRequester = searchFocusRequester,
        onAddCreditor = onAddCreditor,
        onOpenCreditor = onOpenCreditor,
        onRefresh = { viewModel.onIntent(CreditorListIntent.Refresh) },
        onSearch = { viewModel.onIntent(CreditorListIntent.Search(it)) },
        onToggleSortDirection = { viewModel.onIntent(CreditorListIntent.ToggleSortDirection) },
        onChangeSort = { viewModel.onIntent(CreditorListIntent.ChangeSort(it)) },
        onChangeStatusFilter = { viewModel.onIntent(CreditorListIntent.ChangeStatusFilter(it)) },
        onDelete = { viewModel.onIntent(CreditorListIntent.Delete(it)) },
    )
}

@Composable
private fun CreditorListContent(
    state: CreditorListState,
    searchFocusRequester: FocusRequester,
    onAddCreditor: () -> Unit,
    onOpenCreditor: (String) -> Unit,
    onRefresh: () -> Unit,
    onSearch: (String) -> Unit,
    onToggleSortDirection: () -> Unit,
    onChangeSort: (CreditorSortOrder) -> Unit,
    onChangeStatusFilter: (CreditorStatusFilter) -> Unit,
    onDelete: (String) -> Unit,
) {
    val strings = LocalStrings.current

    ContactListContent(
        isLoading = state.isLoading,
        items = state.creditors,
        itemKey = { it.creditor.id },
        isRefreshing = state.isRefreshing,
        onRefresh = onRefresh,
        query = state.query,
        onQueryChange = onSearch,
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
        onToggleSortDirection = onToggleSortDirection,
        onChangeSort = onChangeSort,
        statusOptions = listOf(
            MenuOption(CreditorStatusFilter.ACTIVE, strings.creditorList.filterActive, Icons.Filled.HourglassEmpty),
            MenuOption(CreditorStatusFilter.CLOSED, strings.creditorList.filterClosed, Icons.Filled.CheckCircle),
            MenuOption(CreditorStatusFilter.ALL, strings.creditorList.filterAll, Icons.AutoMirrored.Filled.List),
        ),
        currentStatus = state.statusFilter,
        onChangeStatus = onChangeStatusFilter,
        totalLabel = strings.creditorList.total,
        totalText = state.totalsByCurrency.formatTotals(),
        onAdd = onAddCreditor,
        deleteLabel = strings.delete,
        itemId = { it.creditor.id },
        itemName = { it.creditor.fullName },
        itemPhone = { it.creditor.phone },
        itemAvatarUrl = { it.creditor.avatarUrl },
        itemBalanceText = { it.balance.formatMoney(it.creditor.currency) },
        onItemClick = { onOpenCreditor(it.creditor.id) },
        onItemDelete = { onDelete(it.creditor.id) },
    )
}

@Composable
private fun Preview(state: CreditorListState) = CreditorListContent(
    state = state,
    searchFocusRequester = remember { FocusRequester() },
    onAddCreditor = {},
    onOpenCreditor = {},
    onRefresh = {},
    onSearch = {},
    onToggleSortDirection = {},
    onChangeSort = {},
    onChangeStatusFilter = {},
    onDelete = {},
)

private val PREVIEW_NOW = kotlin.time.Instant.parse("2026-08-15T00:00:00Z")

private val PREVIEW_CREDITORS = listOf(
    org.bigblackowl.debttracker.domain.model.CreditorWithBalance(
        creditor = org.bigblackowl.debttracker.domain.model.Creditor(
            id = "c1", fullName = "Марія Шевченко", phone = "0671112233", email = null, avatarUrl = null,
            comment = null, createdAt = PREVIEW_NOW, updatedAt = PREVIEW_NOW,
            status = org.bigblackowl.debttracker.domain.model.DebtStatus.ACTIVE,
            syncStatus = org.bigblackowl.debttracker.domain.model.SyncStatus.SYNCED,
        ),
        balance = com.ionspin.kotlin.bignum.decimal.BigDecimal.parseString("2000"),
    ),
    org.bigblackowl.debttracker.domain.model.CreditorWithBalance(
        creditor = org.bigblackowl.debttracker.domain.model.Creditor(
            id = "c2", fullName = "Андрій Мельник", phone = null, email = "andriy@example.com", avatarUrl = null,
            comment = null, createdAt = PREVIEW_NOW, updatedAt = PREVIEW_NOW,
            status = org.bigblackowl.debttracker.domain.model.DebtStatus.ACTIVE,
            syncStatus = org.bigblackowl.debttracker.domain.model.SyncStatus.SYNCED,
        ),
        balance = com.ionspin.kotlin.bignum.decimal.BigDecimal.parseString("500"),
    ),
)

private val PREVIEW_STATE = CreditorListState(isLoading = false, creditors = PREVIEW_CREDITORS)

@Preview
@Composable
private fun CreditorListScreenLightPhonePreview() = DebtTrackerPreview(darkTheme = false) {
    Preview(PREVIEW_STATE)
}

@Preview
@Composable
private fun CreditorListScreenDarkPhonePreview() = DebtTrackerPreview(darkTheme = true) {
    Preview(PREVIEW_STATE)
}

@Preview(device = DESKTOP)
@Composable
private fun CreditorListScreenLightDesktopPreview() = DebtTrackerPreview(darkTheme = false) {
    Preview(PREVIEW_STATE)
}

@Preview(device = DESKTOP)
@Composable
private fun CreditorListScreenDarkDesktopPreview() = DebtTrackerPreview(darkTheme = true) {
    Preview(PREVIEW_STATE)
}

@Preview
@Composable
private fun CreditorListScreenLoadingPreview() = DebtTrackerPreview(darkTheme = false) {
    Preview(CreditorListState(isLoading = true))
}

@Preview
@Composable
private fun CreditorListScreenEmptyPreview() = DebtTrackerPreview(darkTheme = false) {
    Preview(CreditorListState(isLoading = false, creditors = emptyList()))
}

@Preview
@Composable
private fun CreditorListScreenRefreshingPreview() = DebtTrackerPreview(darkTheme = false) {
    Preview(PREVIEW_STATE.copy(isRefreshing = true))
}
