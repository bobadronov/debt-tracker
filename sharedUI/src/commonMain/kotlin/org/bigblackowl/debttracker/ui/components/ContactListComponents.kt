package org.bigblackowl.debttracker.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.SortByAlpha
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.tooling.preview.Devices.DESKTOP
import androidx.compose.ui.tooling.preview.Preview
import org.bigblackowl.debttracker.core.settings.AppSettings
import org.bigblackowl.debttracker.preview.DebtTrackerPreview
import org.bigblackowl.debttracker.theme.Dimens
import org.bigblackowl.debttracker.core.i18n.LocalStrings
import org.bigblackowl.debttracker.theme.debtAccentColors
import org.koin.compose.koinInject

/**
 * Shared building blocks for DebtorListScreen/CreditorListScreen (спек §6): the two
 * screens are the same contact list (search + sort/filter, row, running total) over
 * a different domain model, so the UI lives here once and each screen only supplies
 * its own state/intents/strings.
 */

/** One entry in a [ListSearchBar] sort/status dropdown. */
data class MenuOption<T>(val value: T, val label: String, val icon: ImageVector)

/**
 * Search field + combined sort/status dropdown (спек §6, п.2-3). Sort entries are
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
        modifier = modifier.fillMaxWidth().padding(Dimens.space12),
        horizontalArrangement = Arrangement.spacedBy(Dimens.space8),
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

/** One row of a contact list: avatar + name/phone, balance, and an overflow-menu delete. */
@Composable
fun ContactRow(
    id: String,
    name: String,
    phone: String?,
    avatarUrl: String?,
    balanceText: String,
    deleteLabel: String,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    val appSettings = koinInject<AppSettings>()
    val haptics = LocalHapticFeedback.current
    val strings = LocalStrings.current
    var menuOpen by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

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
            Row(verticalAlignment = Alignment.CenterVertically) {
                EntityAvatar(id = id, name = name, avatarUrl = avatarUrl)
                Spacer(Modifier.width(Dimens.space12))
                Column {
                    Text(name, style = MaterialTheme.typography.bodyLarge)
                    phone?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(balanceText, color = MaterialTheme.debtAccentColors.debt)
                Box {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = null)
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text(deleteLabel) },
                            leadingIcon = { Icon(Icons.Default.DeleteOutline, contentDescription = null) },
                            onClick = {
                                menuOpen = false
                                confirmDelete = true
                            },
                        )
                    }
                }
            }
        }
    }

    if (confirmDelete) {
        ConfirmDialog(
            title = strings.deleteContactConfirmTitle,
            text = strings.deleteContactConfirmText(name),
            confirmLabel = strings.delete,
            confirmColor = MaterialTheme.colorScheme.error,
            onConfirm = {
                confirmDelete = false
                if (appSettings.hapticEnabled) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                onDelete()
            },
            onDismiss = { confirmDelete = false },
        )
    }
}

/** Running total + add button, pinned under the list. */
@Composable
fun ListTotalBar(label: String, totalText: String, onAdd: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(Dimens.space12)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(Dimens.space16),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(label)
                Text(totalText, color = MaterialTheme.debtAccentColors.debt)
            }
            FloatingActionButton(
                onClick = onAdd,
                modifier = Modifier.padding(start = Dimens.space12),
            ) { Icon(Icons.Default.Add, null) }
        }
    }
}

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
            verticalArrangement = Arrangement.spacedBy(Dimens.space5),
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

private data class SampleContact(val id: String, val name: String, val phone: String?, val balanceText: String)

private val sampleContacts = listOf(
    SampleContact("1", "Олена Коваль", "+380 67 123 4567", "1 200 ₴"),
    SampleContact("2", "Іван Петренко", null, "450 ₴"),
    SampleContact("3", "Марія Бондар", "+380 50 987 6543", "3 000 ₴"),
)

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
