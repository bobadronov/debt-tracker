package org.bigblackowl.debttracker.ui.components.contact

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Devices.DESKTOP
import androidx.compose.ui.tooling.preview.Preview
import com.ionspin.kotlin.bignum.decimal.BigDecimal
import org.bigblackowl.debttracker.core.i18n.LocalStrings
import org.bigblackowl.debttracker.domain.model.Currency
import org.bigblackowl.debttracker.domain.model.PaymentMethod
import org.bigblackowl.debttracker.domain.model.formatDueDate
import org.bigblackowl.debttracker.domain.model.formatMoney
import org.bigblackowl.debttracker.preview.DebtTrackerPreview
import org.bigblackowl.debttracker.theme.Dimens
import org.bigblackowl.debttracker.theme.debtAccentColors
import org.bigblackowl.debttracker.ui.components.button.IconButton
import org.bigblackowl.debttracker.ui.components.card.SemanticOutlinedCard
import org.bigblackowl.debttracker.ui.components.text.BodyText
import org.bigblackowl.debttracker.ui.components.text.TitleText
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * One transaction row: signed amount, optional comment, method + date — shared by debtor/creditor
 * histories. Passing [onEdit]/[onDelete] adds a ⋮ overflow menu for editing or removing the row.
 */
@Composable
fun TransactionRow(
    amount: BigDecimal,
    method: PaymentMethod,
    comment: String?,
    date: Instant,
    currency: Currency,
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
) {
    val color = if (amount.signum() > 0) {
        MaterialTheme.debtAccentColors.repay
    } else {
        MaterialTheme.debtAccentColors.debt
    }
    val strings = LocalStrings.current
    var menuOpen by remember { mutableStateOf(false) }

    SemanticOutlinedCard(
        borderColor = color.copy(alpha = .4f),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f).padding(Dimens.Spacing.lg)) {
                TitleText(amount.formatMoney(currency), color = color)
                comment?.let { BodyText(it, style = MaterialTheme.typography.bodyLarge) }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    BodyText(method.name, style = MaterialTheme.typography.bodyLarge)
                    BodyText(date.formatDueDate(), style = MaterialTheme.typography.bodyLarge)
                }
            }
            if (onEdit != null || onDelete != null) {
                Box {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = null)
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        onEdit?.let { edit ->
                            DropdownMenuItem(
                                text = { Text(strings.transactionEdit.editTitle) },
                                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                                onClick = { menuOpen = false; edit() },
                            )
                        }
                        onDelete?.let { del ->
                            DropdownMenuItem(
                                text = { Text(strings.delete) },
                                leadingIcon = { Icon(Icons.Default.DeleteOutline, contentDescription = null) },
                                onClick = { menuOpen = false; del() },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun TransactionRowRepayPreview() = DebtTrackerPreview(darkTheme = false) {
    TransactionRow(amount = BigDecimal.parseString("500"), method = PaymentMethod.CASH, comment = "Часткове погашення", date = Clock.System.now(), currency = Currency.UAH, onEdit = {}, onDelete = {})
}

@Preview
@Composable
private fun TransactionRowLendPreview() = DebtTrackerPreview(darkTheme = false) {
    TransactionRow(amount = BigDecimal.parseString("-1200"), method = PaymentMethod.CARD, comment = null, date = Clock.System.now(), currency = Currency.UAH, onEdit = {}, onDelete = {})
}

@Preview
@Composable
private fun TransactionRowNoMenuPreview() = DebtTrackerPreview(darkTheme = false) {
    TransactionRow(amount = BigDecimal.parseString("500"), method = PaymentMethod.CASH, comment = "Без меню редагування", date = Clock.System.now(), currency = Currency.UAH)
}

@Preview
@Composable
private fun TransactionRowDarkPreview() = DebtTrackerPreview(darkTheme = true) {
    TransactionRow(amount = BigDecimal.parseString("500"), method = PaymentMethod.CASH, comment = "Часткове погашення", date = Clock.System.now(), currency = Currency.UAH, onEdit = {}, onDelete = {})
}

@Preview(device = DESKTOP)
@Composable
private fun TransactionRowDesktopPreview() = DebtTrackerPreview(darkTheme = false) {
    TransactionRow(amount = BigDecimal.parseString("500"), method = PaymentMethod.CASH, comment = "Часткове погашення", date = Clock.System.now(), currency = Currency.UAH, onEdit = {}, onDelete = {})
}
