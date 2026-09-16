package org.bigblackowl.debttracker.ui.components.contact

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Devices.DESKTOP
import androidx.compose.ui.tooling.preview.Preview
import org.bigblackowl.debttracker.core.i18n.LocalStrings
import org.bigblackowl.debttracker.domain.model.Currency
import org.bigblackowl.debttracker.domain.validation.formatUkrainianPhone
import org.bigblackowl.debttracker.preview.DebtTrackerPreview
import org.bigblackowl.debttracker.theme.Dimens
import org.bigblackowl.debttracker.theme.debtAccentColors
import org.bigblackowl.debttracker.ui.components.EntityAvatar
import org.bigblackowl.debttracker.ui.components.FullScreenLoadingIndicator
import org.bigblackowl.debttracker.ui.components.appbar.BackTopAppBar
import org.bigblackowl.debttracker.ui.components.button.Button
import org.bigblackowl.debttracker.ui.components.button.IconButton
import org.bigblackowl.debttracker.ui.components.button.OutlinedButton
import org.bigblackowl.debttracker.ui.components.card.ContentCard
import org.bigblackowl.debttracker.ui.components.text.BodyText
import org.bigblackowl.debttracker.ui.components.text.HeadingText

/**
 * Shared building blocks for DebtorDetailScreen/CreditorDetailScreen: same profile-card +
 * repay/borrow buttons + transaction history shell over a different domain model, mirroring
 * how [ContactListScaffold] does it for the list screens.
 */

/** Full screen shell: top bar with export action, profile card, primary/secondary action row, and a pull-to-refresh transaction list. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactDetailScaffold(
    id: String,
    title: String,
    avatarUrl: String?,
    onBack: () -> Unit,
    onExport: () -> Unit,
    snackbarHostState: SnackbarHostState,
    /** Non-null adds a pencil action to the top bar → opens the contact in the edit form. */
    onEdit: (() -> Unit)? = null,
    /** First-load: keep the top bar (title + back) so a desktop two-pane detail swap doesn't blank
     * the native title bar for a frame, but show only a spinner in place of the profile/history. */
    isLoading: Boolean = false,
    phone: String?,
    comment: String?,
    balanceText: String,
    primaryLabel: String,
    onPrimary: () -> Unit,
    secondaryLabel: String,
    onSecondary: () -> Unit,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    transactions: LazyListScope.() -> Unit,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            BackTopAppBar(
                title = title,
                onBack = onBack,
                actions = {
                    if (!isLoading) {
                        onEdit?.let {
                            IconButton(onClick = it) {
                                Icon(Icons.Filled.Edit, contentDescription = LocalStrings.current.edit)
                            }
                        }
                        IconButton(onClick = onExport) { Icon(Icons.Default.Share, contentDescription = null) }
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        if (isLoading) {
            FullScreenLoadingIndicator(Modifier.padding(padding))
            return@Scaffold
        }
        Column(
            modifier = Modifier.fillMaxSize().padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier = Modifier.width(Dimens.contentMaxWidth),
                verticalArrangement = Arrangement.spacedBy(Dimens.Spacing.md),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                ContentCard(modifier = Modifier.padding(Dimens.Spacing.lg)) {
                    Row(modifier = Modifier.padding(Dimens.Spacing.lg), verticalAlignment = Alignment.CenterVertically) {
                        EntityAvatar(id = id, name = title, avatarUrl = avatarUrl, size = Dimens.IconSize.lg)
                        Spacer(Modifier.width(Dimens.Spacing.md))
                        Column {
                            formatUkrainianPhone(phone)?.let { BodyText(it, style = MaterialTheme.typography.bodyLarge) }
                            comment?.let { BodyText(it, style = MaterialTheme.typography.bodyLarge) }
                            Spacer(Modifier.height(Dimens.Spacing.sm))
                            HeadingText(balanceText, color = MaterialTheme.debtAccentColors.debt)
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = Dimens.Spacing.lg),
                    horizontalArrangement = Arrangement.spacedBy(Dimens.Spacing.sm),
                ) {
                    Button(onClick = onPrimary, modifier = Modifier.weight(1f)) { Text(primaryLabel) }
                    OutlinedButton(onClick = onSecondary, modifier = Modifier.weight(1f)) { Text(secondaryLabel) }
                }

                Spacer(Modifier.height(Dimens.Spacing.sm))

                PullToRefreshBox(
                    isRefreshing = isRefreshing,
                    onRefresh = onRefresh,
                    modifier = Modifier.weight(1f),
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(horizontal = Dimens.Spacing.lg),
                        verticalArrangement = Arrangement.spacedBy(Dimens.Spacing.sm),
                        content = transactions,
                    )
                }
            }
        }
    }
}

@Composable
private fun ContactDetailComponentsSample() {
    val snackbarHostState = remember { SnackbarHostState() }
    ContactDetailScaffold(
        id = "1",
        title = "Олена Коваль",
        avatarUrl = null,
        onBack = {},
        onExport = {},
        snackbarHostState = snackbarHostState,
        phone = "+380 67 123 4567",
        comment = "Lent for repairs",
        balanceText = "Owes: 700 ₴",
        primaryLabel = "Repay",
        onPrimary = {},
        secondaryLabel = "Lend more",
        onSecondary = {},
        isRefreshing = false,
        onRefresh = {},
    ) {
        items(sampleTransactions, key = { it.id }) { tx ->
            TransactionRow(
                amount = tx.amount,
                method = tx.method,
                comment = tx.comment,
                date = tx.createdAt,
                currency = Currency.UAH,
                onEdit = {},
                onDelete = {},
            )
        }
    }
}

@Preview
@Composable
private fun ContactDetailComponentsLightPhonePreview() = DebtTrackerPreview(darkTheme = false) { ContactDetailComponentsSample() }

@Preview
@Composable
private fun ContactDetailComponentsDarkPhonePreview() = DebtTrackerPreview(darkTheme = true) { ContactDetailComponentsSample() }

@Preview(device = DESKTOP)
@Composable
private fun ContactDetailComponentsLightDesktopPreview() = DebtTrackerPreview(darkTheme = false) { ContactDetailComponentsSample() }

@Preview(device = DESKTOP)
@Composable
private fun ContactDetailComponentsDarkDesktopPreview() = DebtTrackerPreview(darkTheme = true) { ContactDetailComponentsSample() }
