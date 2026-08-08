package org.bigblackowl.debttracker.ui.screens.export

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.Button
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Devices.DESKTOP
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.bigblackowl.debttracker.core.export.ExportDirection
import org.bigblackowl.debttracker.core.export.ExportFormat
import org.bigblackowl.debttracker.core.export.ExportRow
import org.bigblackowl.debttracker.core.export.buildCsvContent
import org.bigblackowl.debttracker.core.export.rememberFileExporter
import org.bigblackowl.debttracker.core.i18n.LocalStrings
import org.bigblackowl.debttracker.domain.model.formatMoney
import org.bigblackowl.debttracker.domain.usecase.creditor.ObserveCreditorTransactionsUseCase
import org.bigblackowl.debttracker.domain.usecase.creditor.ObserveCreditorsUseCase
import org.bigblackowl.debttracker.domain.usecase.debtor.ObserveDebtorTransactionsUseCase
import org.bigblackowl.debttracker.domain.usecase.debtor.ObserveDebtorsUseCase
import org.bigblackowl.debttracker.preview.DebtTrackerPreview
import org.bigblackowl.debttracker.theme.Dimens
import org.bigblackowl.debttracker.theme.debtAccentColors
import org.bigblackowl.debttracker.ui.components.PlaceholderScreen
import org.bigblackowl.debttracker.ui.components.SettingsRow
import org.bigblackowl.debttracker.ui.components.SettingsSection
import org.koin.compose.koinInject

/** ExportScreen: формат (PDF/CSV) + діапазон дат + напрямок (спек §6, п. 8). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportScreen(onBack: () -> Unit) {
    val observeDebtors = koinInject<ObserveDebtorsUseCase>()
    val observeDebtorTransactions = koinInject<ObserveDebtorTransactionsUseCase>()
    val observeCreditors = koinInject<ObserveCreditorsUseCase>()
    val observeCreditorTransactions = koinInject<ObserveCreditorTransactionsUseCase>()
    val fileExporter = rememberFileExporter()
    val scope = rememberCoroutineScope()

    var format by remember { mutableStateOf(ExportFormat.CSV) }
    var direction by remember { mutableStateOf(ExportDirection.BOTH) }
    var showDateRangePicker by remember { mutableStateOf(false) }
    val dateRangePickerState = rememberDateRangePickerState()
    var isExporting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var success by remember { mutableStateOf(false) }
    val strings = LocalStrings.current

    // DateRangePickerState тримає межі в UTC-мілісекундах (стандарт M3) — конвертуємо в LocalDate тут один раз.
    val fromDate = dateRangePickerState.selectedStartDateMillis?.let {
        kotlin.time.Instant.fromEpochMilliseconds(it).toLocalDateTime(TimeZone.UTC).date
    }
    val toDate = dateRangePickerState.selectedEndDateMillis?.let {
        kotlin.time.Instant.fromEpochMilliseconds(it).toLocalDateTime(TimeZone.UTC).date
    }

    PlaceholderScreen(title = strings.exportTitle, onBack = onBack) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(Dimens.space24),
        ) {
            SettingsSection(strings.exportFormat) {
                Column(modifier = Modifier.fillMaxWidth().padding(Dimens.space16)) {
                    val formatOptions = listOf(
                        ExportFormat.CSV to strings.exportFormatCsv,
                        ExportFormat.PDF to strings.exportFormatPdf,
                    )
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        formatOptions.forEachIndexed { index, (value, label) ->
                            SegmentedButton(
                                selected = format == value,
                                onClick = { format = value },
                                shape = SegmentedButtonDefaults.itemShape(index = index, count = formatOptions.size),
                                label = { Text(label) },
                            )
                        }
                    }
                }
            }

            SettingsSection(strings.exportDirection) {
                Column(modifier = Modifier.fillMaxWidth().padding(Dimens.space16)) {
                    val directionOptions = listOf(
                        ExportDirection.DEBTORS to strings.exportDirectionDebtors,
                        ExportDirection.CREDITORS to strings.exportDirectionCreditors,
                        ExportDirection.BOTH to strings.exportDirectionBoth,
                    )
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        directionOptions.forEachIndexed { index, (value, label) ->
                            SegmentedButton(
                                selected = direction == value,
                                onClick = { direction = value },
                                shape = SegmentedButtonDefaults.itemShape(index = index, count = directionOptions.size),
                                label = { Text(label) },
                            )
                        }
                    }
                }
            }

            SettingsSection(strings.exportDateRangeHint) {
                SettingsRow(
                    icon = Icons.Filled.CalendarMonth,
                    title = if (fromDate != null || toDate != null) {
                        "${fromDate?.toString() ?: strings.exportFrom} — ${toDate?.toString() ?: strings.exportTo}"
                    } else {
                        "${strings.exportFrom} — ${strings.exportTo}"
                    },
                    onClick = { showDateRangePicker = true },
                    trailing = {
                        Icon(
                            Icons.Filled.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                )
            }

            error?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.debtAccentColors.debt)
            }
            if (success) {
                Text(strings.exportDone, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.debtAccentColors.repay)
            }

            Button(
                onClick = {
                    error = null
                    success = false
                    isExporting = true
                    scope.launch {
                        runCatching {
                            val rows = collectExportRows(
                                direction, fromDate, toDate,
                                observeDebtors, observeDebtorTransactions,
                                observeCreditors, observeCreditorTransactions,
                            )
                            when (format) {
                                ExportFormat.CSV -> fileExporter.saveCsv(
                                    "debt_tracker_export.csv",
                                    buildCsvContent(
                                        rows,
                                        listOf(
                                            strings.csvHeaderDate,
                                            strings.csvHeaderContact,
                                            strings.csvHeaderAmount,
                                            strings.csvHeaderComment
                                        ),
                                    ),
                                )

                                ExportFormat.PDF -> fileExporter.savePdf(
                                    "debt_tracker_export.pdf",
                                    "${strings.appName} — ${strings.exportTitle}",
                                    rows
                                )
                            }
                        }.onSuccess {
                            isExporting = false
                            success = true
                        }.onFailure { e ->
                            isExporting = false
                            error = e.message ?: strings.exportError
                        }
                    }
                },
                enabled = !isExporting,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (isExporting) {
                    CircularWavyProgressIndicator(modifier = Modifier.padding(end = Dimens.space8))
                } else {
                    Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.padding(end = Dimens.space8))
                    Text(strings.exportSubmit)
                }
            }
        }
    }
    if (showDateRangePicker) {
        DatePickerDialog(
            onDismissRequest = { showDateRangePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    showDateRangePicker = false
                }) { Text(strings.confirm) }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDateRangePicker = false
                }) { Text(strings.cancel) }
            },
        ) {
            DateRangePicker(state = dateRangePickerState, modifier = Modifier.weight(1f))
        }
    }
}

@Preview
@Composable
private fun ExportScreenPreview() = DebtTrackerPreview {
    ExportScreen(onBack = {})
}

@Preview(device = DESKTOP)
@Composable
private fun ExportScreenPreview2() = DebtTrackerPreview {
    ExportScreen(onBack = {})
}

private suspend fun collectExportRows(
    direction: ExportDirection,
    fromDate: LocalDate?,
    toDate: LocalDate?,
    observeDebtors: ObserveDebtorsUseCase,
    observeDebtorTransactions: ObserveDebtorTransactionsUseCase,
    observeCreditors: ObserveCreditorsUseCase,
    observeCreditorTransactions: ObserveCreditorTransactionsUseCase,
): List<ExportRow> {
    val timeZone = TimeZone.currentSystemDefault()
    val rows = mutableListOf<ExportRow>()

    fun inRange(localDate: LocalDate) =
        (fromDate == null || localDate >= fromDate) && (toDate == null || localDate <= toDate)

    if (direction == ExportDirection.DEBTORS || direction == ExportDirection.BOTH) {
        observeDebtors().first().forEach { item ->
            observeDebtorTransactions(item.debtor.id).first().forEach { tx ->
                val date = tx.date.toLocalDateTime(timeZone).date
                if (inRange(date)) {
                    rows.add(
                        ExportRow(
                            date.toString(),
                            item.debtor.fullName,
                            tx.amount.formatMoney(item.debtor.currency),
                            tx.comment
                        )
                    )
                }
            }
        }
    }
    if (direction == ExportDirection.CREDITORS || direction == ExportDirection.BOTH) {
        observeCreditors().first().forEach { item ->
            observeCreditorTransactions(item.creditor.id).first().forEach { tx ->
                val date = tx.date.toLocalDateTime(timeZone).date
                if (inRange(date)) {
                    rows.add(
                        ExportRow(
                            date.toString(),
                            item.creditor.fullName,
                            tx.amount.formatMoney(item.creditor.currency),
                            tx.comment
                        )
                    )
                }
            }
        }
    }
    return rows.sortedBy { it.date }
}
