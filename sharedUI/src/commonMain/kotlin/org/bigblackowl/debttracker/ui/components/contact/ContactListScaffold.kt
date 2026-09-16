package org.bigblackowl.debttracker.ui.components.contact

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.tooling.preview.Devices.DESKTOP
import androidx.compose.ui.tooling.preview.Preview
import org.bigblackowl.debttracker.preview.DebtTrackerPreview
import org.bigblackowl.debttracker.theme.Dimens

/**
 * Full screen shell shared by DebtorListScreen/CreditorListScreen: centered content column,
 * search bar on top, pull-to-refresh list in the middle, total bar pinned to the bottom.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> ContactListScaffold(
    items: List<T>,
    key: (T) -> Any,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    searchBar: @Composable () -> Unit,
    totalBar: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    row: @Composable (T) -> Unit,
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            modifier = Modifier.width(Dimens.contentMaxWidth),
            verticalArrangement = Arrangement.spacedBy(Dimens.Spacing.xs),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            searchBar()

            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = onRefresh,
                modifier = Modifier.weight(1f),
            ) {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(items, key = key) { row(it) }
                }
            }

            totalBar()
        }
    }
}

@Composable
private fun ContactListComponentsSample() {
    var query by remember { mutableStateOf("") }
    val searchFocusRequester = remember { FocusRequester() }

    ContactListScaffold(
        items = sampleContacts,
        key = { it.id },
        isRefreshing = false,
        onRefresh = {},
        searchBar = {
            ListSearchBar(
                query = query,
                onQueryChange = { query = it },
                searchPlaceholder = "Search",
                clearSearchDescription = "Clear",
                searchFocusRequester = searchFocusRequester,
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
            )
        },
        totalBar = {
            ListTotalBar(label = "Total", totalText = "4 650 ₴", onAdd = {})
        },
    ) { item ->
        ContactRow(
            id = item.id,
            name = item.name,
            phone = item.phone,
            avatarUrl = null,
            balanceText = item.balanceText,
            deleteLabel = "Delete",
            onClick = {},
            onDelete = {},
        )
    }
}

@Preview
@Composable
private fun ContactListComponentsLightPhonePreview() = DebtTrackerPreview(darkTheme = false) { ContactListComponentsSample() }

@Preview
@Composable
private fun ContactListComponentsDarkPhonePreview() = DebtTrackerPreview(darkTheme = true) { ContactListComponentsSample() }

@Preview(device = DESKTOP)
@Composable
private fun ContactListComponentsLightDesktopPreview() = DebtTrackerPreview(darkTheme = false) { ContactListComponentsSample() }

@Preview(device = DESKTOP)
@Composable
private fun ContactListComponentsDarkDesktopPreview() = DebtTrackerPreview(darkTheme = true) { ContactListComponentsSample() }
