package org.bigblackowl.debttracker.ui.components.contact

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.tooling.preview.Devices.DESKTOP
import androidx.compose.ui.tooling.preview.Preview
import org.bigblackowl.debttracker.preview.DebtTrackerPreview
import org.bigblackowl.debttracker.ui.components.FullScreenLoadingIndicator

/**
 * Full DebtorListScreen/CreditorListScreen body: loading guard + [ContactListScaffold] with
 * [ListSearchBar]/[ListTotalBar]/[ContactRow] wired up. Each screen only supplies its own state
 * fields, string resources, and per-item extractors — no UI logic differs between the two lists.
 */
@Composable
fun <T, S, F> ContactListContent(
    isLoading: Boolean,
    items: List<T>,
    itemKey: (T) -> Any,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    query: String,
    onQueryChange: (String) -> Unit,
    searchPlaceholder: String,
    clearSearchDescription: String,
    searchFocusRequester: FocusRequester,
    filterDescription: String,
    sortOptions: List<MenuOption<S>>,
    currentSort: S,
    sortAscending: Boolean,
    sortReverseDescription: String,
    onToggleSortDirection: () -> Unit,
    onChangeSort: (S) -> Unit,
    statusOptions: List<MenuOption<F>>,
    currentStatus: F,
    onChangeStatus: (F) -> Unit,
    totalLabel: String,
    totalText: String,
    onAdd: () -> Unit,
    deleteLabel: String,
    itemId: (T) -> String,
    itemName: (T) -> String,
    itemPhone: (T) -> String?,
    itemAvatarUrl: (T) -> String?,
    itemBalanceText: (T) -> String,
    onItemClick: (T) -> Unit,
    onItemDelete: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (isLoading) {
        FullScreenLoadingIndicator()
        return
    }

    ContactListScaffold(
        items = items,
        key = itemKey,
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier,
        searchBar = {
            ListSearchBar(
                query = query,
                onQueryChange = onQueryChange,
                searchPlaceholder = searchPlaceholder,
                clearSearchDescription = clearSearchDescription,
                searchFocusRequester = searchFocusRequester,
                filterDescription = filterDescription,
                sortOptions = sortOptions,
                currentSort = currentSort,
                sortAscending = sortAscending,
                sortReverseDescription = sortReverseDescription,
                onToggleSortDirection = onToggleSortDirection,
                onChangeSort = onChangeSort,
                statusOptions = statusOptions,
                currentStatus = currentStatus,
                onChangeStatus = onChangeStatus,
            )
        },
        totalBar = {
            ListTotalBar(label = totalLabel, totalText = totalText, onAdd = onAdd)
        },
    ) { item ->
        ContactRow(
            id = itemId(item),
            name = itemName(item),
            phone = itemPhone(item),
            avatarUrl = itemAvatarUrl(item),
            balanceText = itemBalanceText(item),
            deleteLabel = deleteLabel,
            onClick = { onItemClick(item) },
            onDelete = { onItemDelete(item) },
        )
    }
}

@Composable
private fun ContactListContentSample(isLoading: Boolean = false, items: List<SampleContact> = sampleContacts, isRefreshing: Boolean = false) {
    var query by remember { mutableStateOf("") }
    ContactListContent(
        isLoading = isLoading,
        items = items,
        itemKey = { it.id },
        isRefreshing = isRefreshing,
        onRefresh = {},
        query = query,
        onQueryChange = { query = it },
        searchPlaceholder = "Search",
        clearSearchDescription = "Clear",
        searchFocusRequester = remember { FocusRequester() },
        filterDescription = "Filter",
        sortOptions = listOf(
            MenuOption("name", "By name", Icons.Filled.SortByAlpha),
            MenuOption("balance", "By balance", Icons.Filled.Payments),
        ),
        currentSort = "name",
        sortAscending = true,
        sortReverseDescription = "Reverse",
        onToggleSortDirection = {},
        onChangeSort = {},
        statusOptions = listOf(
            MenuOption("active", "Active", Icons.Filled.HourglassEmpty),
            MenuOption("all", "All", Icons.AutoMirrored.Filled.List),
        ),
        currentStatus = "active",
        onChangeStatus = {},
        totalLabel = "Total",
        totalText = "4 650 ₴",
        onAdd = {},
        deleteLabel = "Delete",
        itemId = { it.id },
        itemName = { it.name },
        itemPhone = { it.phone },
        itemAvatarUrl = { null },
        itemBalanceText = { it.balanceText },
        onItemClick = {},
        onItemDelete = {},
    )
}

@Preview
@Composable
private fun ContactListContentLoadingPreview() = DebtTrackerPreview(darkTheme = false) { ContactListContentSample(isLoading = true) }

@Preview
@Composable
private fun ContactListContentEmptyPreview() = DebtTrackerPreview(darkTheme = false) { ContactListContentSample(items = emptyList()) }

@Preview
@Composable
private fun ContactListContentRefreshingPreview() = DebtTrackerPreview(darkTheme = false) { ContactListContentSample(isRefreshing = true) }

@Preview(device = DESKTOP)
@Composable
private fun ContactListContentDesktopPreview() = DebtTrackerPreview(darkTheme = false) { ContactListContentSample() }
