package org.bigblackowl.debttracker.ui.components.contact

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.tooling.preview.Devices.DESKTOP
import androidx.compose.ui.tooling.preview.Preview
import org.bigblackowl.debttracker.preview.DebtTrackerPreview
import org.bigblackowl.debttracker.theme.Dimens
import org.bigblackowl.debttracker.ui.components.button.IconButton

/**
 * Search field + combined sort/status dropdown (spec §6, items 2-3). Sort entries are
 * single-click toggles: tapping the already-active one flips [sortAscending] via
 * [onToggleSortDirection] instead of re-selecting it via [onChangeSort].
 */
@Composable
fun <S, F> ListSearchBar(
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
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(Dimens.Spacing.md),
        horizontalArrangement = Arrangement.spacedBy(Dimens.Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.weight(1f).focusRequester(searchFocusRequester),
            singleLine = true,
            placeholder = { Text(searchPlaceholder) },
            trailingIcon = if (query.isNotEmpty()) {
                {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Filled.Close, clearSearchDescription)
                    }
                }
            } else null,
        )

        var filterMenuOpen by remember { mutableStateOf(false) }
        Box {
            IconButton(onClick = { filterMenuOpen = true }) {
                Icon(Icons.Filled.FilterList, filterDescription)
            }
            DropdownMenu(
                expanded = filterMenuOpen,
                onDismissRequest = { filterMenuOpen = false },
            ) {
                sortOptions.forEach { option ->
                    val isActive = currentSort == option.value
                    DropdownMenuItem(
                        leadingIcon = { Icon(option.icon, null) },
                        text = { Text(option.label) },
                        trailingIcon = if (isActive) {
                            {
                                Icon(
                                    if (sortAscending) Icons.Filled.ArrowUpward else Icons.Filled.ArrowDownward,
                                    sortReverseDescription,
                                )
                            }
                        } else null,
                        onClick = {
                            if (isActive) onToggleSortDirection() else onChangeSort(option.value)
                            filterMenuOpen = false
                        },
                    )
                }
                HorizontalDivider()
                statusOptions.forEach { option ->
                    val isSelected = currentStatus == option.value
                    DropdownMenuItem(
                        leadingIcon = { Icon(option.icon, null) },
                        text = { Text(option.label) },
                        trailingIcon = if (isSelected) {
                            { Icon(Icons.Filled.Check, null) }
                        } else null,
                        onClick = {
                            onChangeStatus(option.value)
                            filterMenuOpen = false
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun ListSearchBarSample(query: String = "", currentSort: String = "name", sortAscending: Boolean = true) {
    ListSearchBar(
        query = query,
        onQueryChange = {},
        searchPlaceholder = "Search",
        clearSearchDescription = "Clear",
        searchFocusRequester = remember { FocusRequester() },
        filterDescription = "Filter",
        sortOptions = listOf(
            MenuOption("name", "By name", Icons.Filled.SortByAlpha),
            MenuOption("balance", "By balance", Icons.Filled.Payments),
        ),
        currentSort = currentSort,
        sortAscending = sortAscending,
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
}

@Preview
@Composable
private fun ListSearchBarEmptyPreview() = DebtTrackerPreview(darkTheme = false) { ListSearchBarSample() }

@Preview
@Composable
private fun ListSearchBarWithQueryPreview() = DebtTrackerPreview(darkTheme = false) { ListSearchBarSample(query = "Олена") }

@Preview
@Composable
private fun ListSearchBarSortDescendingPreview() = DebtTrackerPreview(darkTheme = false) { ListSearchBarSample(currentSort = "balance", sortAscending = false) }

@Preview
@Composable
private fun ListSearchBarDarkPreview() = DebtTrackerPreview(darkTheme = true) { ListSearchBarSample() }

@Preview(device = DESKTOP)
@Composable
private fun ListSearchBarDesktopPreview() = DebtTrackerPreview(darkTheme = false) { ListSearchBarSample() }
