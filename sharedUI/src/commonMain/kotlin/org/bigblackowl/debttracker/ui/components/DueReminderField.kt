package org.bigblackowl.debttracker.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import org.bigblackowl.debttracker.core.i18n.LocalStrings
import org.bigblackowl.debttracker.domain.model.formatDueDateTime
import org.bigblackowl.debttracker.theme.Dimens

/**
 * "Repayment reminder" field for the add/edit contact form: an optional due date+time plus chips
 * for extra lead-day reminders. The on-the-day reminder is implied whenever a date is set
 * ("сьогодні обовязково") — its chip is shown selected-and-disabled for clarity.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DueReminderField(
    dueDate: kotlin.time.Instant?,
    onDueDateChange: (kotlin.time.Instant?) -> Unit,
    reminderLeadDays: Set<Int>,
    onToggleReminderLead: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStrings.current
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var pendingDateMillis by remember { mutableStateOf<Long?>(null) }

    val r = strings.dueReminder
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Dimens.space8)) {
        OutlinedCard(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(Dimens.space16),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.NotificationsActive, contentDescription = null)
                Spacer(Modifier.width(Dimens.space12))
                Column(Modifier.weight(1f)) {
                    Text(r.label, style = MaterialTheme.typography.labelMedium)
                    Text(
                        text = dueDate?.formatDueDateTime() ?: r.notSet,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
                if (dueDate != null) {
                    IconButton(onClick = { onDueDateChange(null) }) {
                        Icon(Icons.Filled.Close, contentDescription = r.clear)
                    }
                } else {
                    Icon(Icons.Filled.EditCalendar, contentDescription = r.label)
                }
            }
        }

        if (dueDate != null) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Dimens.space8)) {
                FilterChip(
                    selected = true,
                    enabled = false,
                    onClick = {},
                    label = { Text(r.leadOnDay) },
                )
                FilterChip(
                    selected = 1 in reminderLeadDays,
                    onClick = { onToggleReminderLead(1) },
                    label = { Text(r.lead1Day) },
                )
                FilterChip(
                    selected = 2 in reminderLeadDays,
                    onClick = { onToggleReminderLead(2) },
                    label = { Text(r.lead2Days) },
                )
            }
        }
    }

    if (showDatePicker) {
        val dpState = rememberDatePickerState(initialSelectedDateMillis = dueDate?.toEpochMilliseconds())
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pendingDateMillis = dpState.selectedDateMillis
                    showDatePicker = false
                    if (pendingDateMillis != null) showTimePicker = true
                }) { Text(strings.confirm) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text(strings.cancel) }
            },
        ) {
            DatePicker(state = dpState)
        }
    }

    if (showTimePicker) {
        val existing = dueDate?.toLocalDateTime(TimeZone.currentSystemDefault())
        val tpState = rememberTimePickerState(
            initialHour = existing?.hour ?: 12,
            initialMinute = existing?.minute ?: 0,
            is24Hour = true,
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val dateMillis = pendingDateMillis
                    showTimePicker = false
                    if (dateMillis != null) {
                        // DatePicker keeps the picked calendar day as UTC-midnight millis (M3 default).
                        val pickedDate = kotlin.time.Instant.fromEpochMilliseconds(dateMillis)
                            .toLocalDateTime(TimeZone.UTC).date
                        val local = LocalDateTime(pickedDate, LocalTime(tpState.hour, tpState.minute))
                        onDueDateChange(local.toInstant(TimeZone.currentSystemDefault()))
                    }
                }) { Text(strings.confirm) }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text(strings.cancel) }
            },
            text = { TimePicker(state = tpState) },
        )
    }
}
