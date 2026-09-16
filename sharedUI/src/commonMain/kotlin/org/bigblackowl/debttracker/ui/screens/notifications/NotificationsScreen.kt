package org.bigblackowl.debttracker.ui.screens.notifications

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PriceChange
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Devices.DESKTOP
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ionspin.kotlin.bignum.decimal.BigDecimal
import org.bigblackowl.debttracker.core.i18n.LocalStrings
import org.bigblackowl.debttracker.core.notifications.formatBody
import org.bigblackowl.debttracker.domain.model.AppNotification
import org.bigblackowl.debttracker.domain.model.CorrectionReason
import org.bigblackowl.debttracker.domain.model.NotificationType
import org.bigblackowl.debttracker.domain.model.formatDateTime
import org.bigblackowl.debttracker.domain.validation.sanitizeAmountInput
import org.bigblackowl.debttracker.preview.DebtTrackerPreview
import org.bigblackowl.debttracker.theme.Dimens
import org.bigblackowl.debttracker.theme.debtAccentColors
import org.bigblackowl.debttracker.ui.components.FullScreenLoadingIndicator
import org.bigblackowl.debttracker.ui.components.button.IconButton
import org.bigblackowl.debttracker.ui.components.button.TextButton
import org.bigblackowl.debttracker.ui.components.form.PasteableOutlinedTextField
import org.bigblackowl.debttracker.ui.components.form.rememberClipboardText
import org.bigblackowl.debttracker.ui.components.settings.SettingsDetailScaffold
import org.bigblackowl.debttracker.ui.components.settings.SettingsRow
import org.bigblackowl.debttracker.ui.components.settings.SettingsRowDivider
import org.bigblackowl.debttracker.ui.components.settings.SettingsSection
import org.bigblackowl.debttracker.ui.components.text.BodyText
import org.bigblackowl.debttracker.ui.components.text.CaptionText
import org.koin.compose.viewmodel.koinViewModel

/** History of notifications about mirrored debts (spec §7) — available only in Account+Sync (badge in the Home top bar). */
@Composable
fun NotificationsScreen(
    onBack: () -> Unit,
    onNavigateToDebtor: (String) -> Unit,
    onNavigateToCreditor: (String) -> Unit,
    viewModel: NotificationsViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is NotificationsEffect.NavigateToDebtor -> onNavigateToDebtor(effect.debtorId)
                is NotificationsEffect.NavigateToCreditor -> onNavigateToCreditor(effect.creditorId)
                is NotificationsEffect.Error -> snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    NotificationsContent(
        state = state,
        snackbarHostState = snackbarHostState,
        onBack = onBack,
        onMarkAllRead = { viewModel.onIntent(NotificationsIntent.MarkAllRead) },
        onOpen = { viewModel.onIntent(NotificationsIntent.Open(it)) },
        onDelete = { viewModel.onIntent(NotificationsIntent.Delete(it)) },
        onApproveLinkRequest = { id, requestId -> viewModel.onIntent(NotificationsIntent.ApproveLinkRequest(id, requestId)) },
        onRejectLinkRequest = { id, requestId -> viewModel.onIntent(NotificationsIntent.RejectLinkRequest(id, requestId)) },
        onApproveCorrection = { id, correctionId -> viewModel.onIntent(NotificationsIntent.ApproveCorrection(id, correctionId)) },
        onRejectCorrection = { id, correctionId -> viewModel.onIntent(NotificationsIntent.RejectCorrection(id, correctionId)) },
        onOpenCorrectionDialog = { viewModel.onIntent(NotificationsIntent.OpenCorrectionDialog(it)) },
        onDismissCorrectionDialog = { viewModel.onIntent(NotificationsIntent.DismissCorrectionDialog) },
        onSubmitCorrection = { id, reason, amount -> viewModel.onIntent(NotificationsIntent.SubmitCorrection(id, reason, amount)) },
    )
}

@Composable
private fun NotificationsContent(
    state: NotificationsState,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onMarkAllRead: () -> Unit,
    onOpen: (AppNotification) -> Unit,
    onDelete: (String) -> Unit,
    onApproveLinkRequest: (String, String) -> Unit,
    onRejectLinkRequest: (String, String) -> Unit,
    onApproveCorrection: (String, String) -> Unit,
    onRejectCorrection: (String, String) -> Unit,
    onOpenCorrectionDialog: (AppNotification) -> Unit,
    onDismissCorrectionDialog: () -> Unit,
    onSubmitCorrection: (String, CorrectionReason, BigDecimal?) -> Unit,
) {
    val strings = LocalStrings.current

    SettingsDetailScaffold(
        title = strings.notifications.title,
        onBack = onBack,
        snackbarHostState = snackbarHostState,
    ) {
        when {
            state.isLoading -> FullScreenLoadingIndicator()
            state.notifications.isEmpty() -> BodyText(
                strings.notifications.empty,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(Dimens.Spacing.xl),
            )
            else -> {
                if (state.notifications.any { !it.isRead }) {
                    TextButton(
                        onClick = onMarkAllRead,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Filled.DoneAll, contentDescription = null, modifier = Modifier.padding(end = Dimens.Spacing.sm))
                        Text(strings.notifications.markAllRead)
                    }
                }
                SettingsSection(strings.notifications.title) {
                    state.notifications.forEachIndexed { index, notification ->
                        NotificationRow(
                            notification = notification,
                            onOpen = { onOpen(notification) },
                            onDelete = { onDelete(notification.id) },
                            onApprove = notification.relatedLinkRequestId?.let { requestId ->
                                { onApproveLinkRequest(notification.id, requestId) }
                            },
                            onReject = notification.relatedLinkRequestId?.let { requestId ->
                                { onRejectLinkRequest(notification.id, requestId) }
                            },
                            onApproveCorrection = notification.relatedCorrectionId
                                ?.takeIf { notification.type == NotificationType.TRANSACTION_CORRECTION }
                                ?.let { correctionId ->
                                    { onApproveCorrection(notification.id, correctionId) }
                                },
                            onRejectCorrection = notification.relatedCorrectionId
                                ?.takeIf { notification.type == NotificationType.TRANSACTION_CORRECTION }
                                ?.let { correctionId ->
                                    { onRejectCorrection(notification.id, correctionId) }
                                },
                            onProposeCorrection = notification.relatedTransactionId
                                ?.takeIf { notification.type.isTransactionAdded() }
                                ?.let { { onOpenCorrectionDialog(notification) } },
                        )
                        if (index != state.notifications.lastIndex) SettingsRowDivider()
                    }
                }
            }
        }
    }

    state.correctionDialogFor?.let { notification ->
        CorrectionDialog(
            notification = notification,
            onDismiss = onDismissCorrectionDialog,
            onSubmit = { reason, amount ->
                onSubmitCorrection(notification.id, reason, amount)
            },
        )
    }
}

private fun NotificationType.isTransactionAdded() =
    this == NotificationType.DEBT_TRANSACTION_ADDED || this == NotificationType.CREDIT_TRANSACTION_ADDED

@Composable
private fun NotificationRow(
    notification: AppNotification,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
    onApprove: (() -> Unit)? = null,
    onReject: (() -> Unit)? = null,
    onApproveCorrection: (() -> Unit)? = null,
    onRejectCorrection: (() -> Unit)? = null,
    onProposeCorrection: (() -> Unit)? = null,
) {
    val strings = LocalStrings.current
    val accent = when (notification.type) {
        NotificationType.DEBTOR_LINKED, NotificationType.DEBT_TRANSACTION_ADDED -> MaterialTheme.debtAccentColors.debt
        NotificationType.CREDITOR_LINKED, NotificationType.CREDIT_TRANSACTION_ADDED -> MaterialTheme.debtAccentColors.repay
        NotificationType.LINK_REQUEST, NotificationType.LINK_REQUEST_APPROVED -> MaterialTheme.debtAccentColors.repay
        NotificationType.TRANSACTION_CORRECTION,
        NotificationType.TRANSACTION_CORRECTION_APPROVED,
        NotificationType.TRANSACTION_CORRECTION_REJECTED,
            -> MaterialTheme.debtAccentColors.debt
    }
    SettingsRow(
        icon = notification.type.icon(),
        title = notification.formatBody(strings),
        titleColor = if (notification.isRead) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
        subtitle = notification.createdAt.formatDateTime(),
        iconTint = accent,
        iconContainerColor = accent.copy(alpha = 0.14f),
        onClick = onOpen,
        trailing = {
            when {
                notification.type == NotificationType.LINK_REQUEST && onApprove != null && onReject != null -> Row {
                    IconButton(onClick = onApprove) {
                        Icon(Icons.Filled.Check, contentDescription = strings.notificationBody.approveAction)
                    }
                    IconButton(onClick = onReject) {
                        Icon(Icons.Filled.Close, contentDescription = strings.notificationBody.rejectAction)
                    }
                }

                notification.type == NotificationType.TRANSACTION_CORRECTION && onApproveCorrection != null && onRejectCorrection != null -> Row {
                    IconButton(onClick = onApproveCorrection) {
                        Icon(Icons.Filled.Check, contentDescription = strings.notificationBody.approveAction)
                    }
                    IconButton(onClick = onRejectCorrection) {
                        Icon(Icons.Filled.Close, contentDescription = strings.notificationBody.rejectAction)
                    }
                }

                onProposeCorrection != null -> Row {
                    IconButton(onClick = onProposeCorrection) {
                        Icon(Icons.Filled.PriceChange, contentDescription = strings.notifications.correction.rowAction)
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Filled.Close, contentDescription = null)
                    }
                }

                else -> IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Close, contentDescription = null)
                }
            }
        },
    )
}

/** "Reject transaction" dialog on a *_TRANSACTION_ADDED row — reason + (for "wrong amount") a new amount (0014). */
@Composable
private fun CorrectionDialog(
    notification: AppNotification,
    onDismiss: () -> Unit,
    onSubmit: (CorrectionReason, BigDecimal?) -> Unit,
) {
    val strings = LocalStrings.current
    val clipboardText by rememberClipboardText()
    var reason by remember { mutableStateOf(CorrectionReason.WRONG_AMOUNT) }
    var amountText by remember { mutableStateOf("") }

    val parsedAmount = runCatching { BigDecimal.parseString(amountText.trim()) }.getOrNull()
        ?.takeIf { it > BigDecimal.ZERO }
    val canSend = reason == CorrectionReason.NOT_HAPPENED || parsedAmount != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.notifications.correction.title) },
        text = {
            Column {
                ReasonRow(
                    selected = reason == CorrectionReason.WRONG_AMOUNT,
                    label = strings.notifications.correction.reasonWrongAmount,
                    onSelect = { reason = CorrectionReason.WRONG_AMOUNT },
                )
                if (reason == CorrectionReason.WRONG_AMOUNT) {
                    PasteableOutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = sanitizeAmountInput(it) },
                        label = strings.notifications.correction.amountLabel(notification.currency?.symbol ?: ""),
                        clipboardText = clipboardText,
                        isPasteRelevant = { text ->
                            val s = sanitizeAmountInput(text)
                            s.isNotBlank() && runCatching { BigDecimal.parseString(s) }.getOrNull()?.let { it > BigDecimal.ZERO } == true
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.padding(start = Dimens.Spacing.xxl, top = Dimens.Spacing.xs, bottom = Dimens.Spacing.sm),
                    ) { amountText = sanitizeAmountInput(it) }
                }
                ReasonRow(
                    selected = reason == CorrectionReason.NOT_HAPPENED,
                    label = strings.notifications.correction.reasonNotHappened,
                    onSelect = { reason = CorrectionReason.NOT_HAPPENED },
                )
                if (reason == CorrectionReason.NOT_HAPPENED) {
                    CaptionText(
                        strings.notifications.correction.notHappenedHint,
                        modifier = Modifier.padding(start = Dimens.Spacing.xxl, top = Dimens.Spacing.xs),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSubmit(reason, parsedAmount?.takeIf { reason == CorrectionReason.WRONG_AMOUNT }) },
                enabled = canSend,
            ) { Text(strings.notifications.correction.send) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(strings.cancel) }
        },
    )
}

@Composable
private fun ReasonRow(selected: Boolean, label: String, onSelect: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().selectable(selected = selected, onClick = onSelect).padding(vertical = Dimens.Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onSelect)
        BodyText(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(start = Dimens.Spacing.sm))
    }
}

private fun NotificationType.icon(): ImageVector = when (this) {
    NotificationType.DEBTOR_LINKED, NotificationType.CREDITOR_LINKED,
    NotificationType.LINK_REQUEST, NotificationType.LINK_REQUEST_APPROVED,
        -> Icons.Filled.Link
    NotificationType.DEBT_TRANSACTION_ADDED, NotificationType.CREDIT_TRANSACTION_ADDED -> Icons.Filled.SwapHoriz
    NotificationType.TRANSACTION_CORRECTION,
    NotificationType.TRANSACTION_CORRECTION_APPROVED,
    NotificationType.TRANSACTION_CORRECTION_REJECTED,
        -> Icons.AutoMirrored.Filled.ReceiptLong
}

private val PREVIEW_NOW = kotlin.time.Instant.parse("2026-09-16T08:00:00Z")

private val PREVIEW_NOTIFICATIONS = listOf(
    AppNotification(
        id = "n1", type = NotificationType.DEBT_TRANSACTION_ADDED, actorDisplayName = "Тарас Шевченко",
        relatedDebtorId = "d1", relatedCreditorId = null, relatedLinkRequestId = null,
        relatedTransactionId = "t1", relatedCorrectionId = null,
        amount = BigDecimal.parseString("500"), currency = org.bigblackowl.debttracker.domain.model.Currency.UAH,
        isRead = false, createdAt = PREVIEW_NOW,
    ),
    AppNotification(
        id = "n2", type = NotificationType.LINK_REQUEST, actorDisplayName = "Леся Українка",
        relatedDebtorId = null, relatedCreditorId = null, relatedLinkRequestId = "lr1",
        relatedTransactionId = null, relatedCorrectionId = null,
        amount = null, currency = null,
        isRead = true, createdAt = PREVIEW_NOW,
    ),
)

@Composable
private fun Preview(state: NotificationsState) = NotificationsContent(
    state = state,
    snackbarHostState = remember { SnackbarHostState() },
    onBack = {},
    onMarkAllRead = {},
    onOpen = {},
    onDelete = {},
    onApproveLinkRequest = { _, _ -> },
    onRejectLinkRequest = { _, _ -> },
    onApproveCorrection = { _, _ -> },
    onRejectCorrection = { _, _ -> },
    onOpenCorrectionDialog = {},
    onDismissCorrectionDialog = {},
    onSubmitCorrection = { _, _, _ -> },
)

@Preview
@Composable
private fun NotificationsScreenLightPhonePreview() = DebtTrackerPreview(darkTheme = false) {
    Preview(NotificationsState(isLoading = false, notifications = PREVIEW_NOTIFICATIONS))
}

@Preview
@Composable
private fun NotificationsScreenDarkPhonePreview() = DebtTrackerPreview(darkTheme = true) {
    Preview(NotificationsState(isLoading = false, notifications = PREVIEW_NOTIFICATIONS))
}

@Preview(device = DESKTOP)
@Composable
private fun NotificationsScreenLightDesktopPreview() = DebtTrackerPreview(darkTheme = false) {
    Preview(NotificationsState(isLoading = false, notifications = PREVIEW_NOTIFICATIONS))
}

@Preview
@Composable
private fun NotificationsScreenLoadingPreview() = DebtTrackerPreview(darkTheme = false) {
    Preview(NotificationsState(isLoading = true))
}

@Preview
@Composable
private fun NotificationsScreenEmptyPreview() = DebtTrackerPreview(darkTheme = false) {
    Preview(NotificationsState(isLoading = false, notifications = emptyList()))
}

@Preview
@Composable
private fun NotificationsScreenCorrectionDialogPreview() = DebtTrackerPreview(darkTheme = false) {
    Preview(NotificationsState(isLoading = false, notifications = PREVIEW_NOTIFICATIONS, correctionDialogFor = PREVIEW_NOTIFICATIONS.first()))
}
