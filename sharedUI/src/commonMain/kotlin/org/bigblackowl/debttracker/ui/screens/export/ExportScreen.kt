package org.bigblackowl.debttracker.ui.screens.export

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Download
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Devices.DESKTOP
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.bigblackowl.debttracker.core.export.ExportDirection
import org.bigblackowl.debttracker.core.export.ExportFormat
import org.bigblackowl.debttracker.core.export.rememberFileExporter
import org.bigblackowl.debttracker.core.i18n.LocalStrings
import org.bigblackowl.debttracker.preview.DebtTrackerPreview
import org.bigblackowl.debttracker.theme.Dimens
import org.bigblackowl.debttracker.theme.debtAccentColors
import org.bigblackowl.debttracker.ui.components.LoadingButton
import org.bigblackowl.debttracker.ui.components.PlaceholderScreen
import org.bigblackowl.debttracker.ui.components.SettingsRow
import org.bigblackowl.debttracker.ui.components.SettingsSection
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * ExportScreen: формат (PDF/CSV) + діапазон дат + напрямок (спек §6, п. 8). Selection state and the
 * CSV/PDF pipeline live in [ExportViewModel] — this screen only owns the composition-scoped
 * [rememberFileExporter] object and Material3's own [rememberDateRangePickerState].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportScreen(
    onBack: () -> Unit,
    debtorId: String? = null,
    creditorId: String? = null,
    viewModel: ExportViewModel = koinViewModel { parametersOf(debtorId, creditorId) },
) {
    val fileExporter = rememberFileExporter()
    val state by viewModel.state.collectAsState()

    var showDateRangePicker by remember { mutableStateOf(false) }
    val dateRangePickerState = rememberDateRangePickerState()
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
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier = Modifier.width(Dimens.contentMaxWidth),
                verticalArrangement = Arrangement.spacedBy(Dimens.space12),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (viewModel.isScoped) {
                    Text(
                        state.scopedContactName ?: "…",
                        modifier = Modifier.fillMaxWidth(),
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center
                    )
                }

                SettingsSection(strings.exportFormat) {
                    Column(modifier = Modifier.fillMaxWidth().padding(Dimens.space16)) {
                        val formatOptions = listOf(
                            ExportFormat.CSV to strings.exportFormatCsv,
                            ExportFormat.PDF to strings.exportFormatPdf,
                        )
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            formatOptions.forEachIndexed { index, (value, label) ->
                                SegmentedButton(
                                    selected = state.format == value,
                                    onClick = { viewModel.onIntent(ExportIntent.SetFormat(value)) },
                                    shape = SegmentedButtonDefaults.itemShape(
                                        index = index,
                                        count = formatOptions.size
                                    ),
                                    label = { Text(label) },
                                )
                            }
                        }
                    }
                }

                if (!viewModel.isScoped) {
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
                                        selected = state.direction == value,
                                        onClick = { viewModel.onIntent(ExportIntent.SetDirection(value)) },
                                        shape = SegmentedButtonDefaults.itemShape(
                                            index = index,
                                            count = directionOptions.size
                                        ),
                                        label = { Text(label) },
                                    )
                                }
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

                state.error?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.debtAccentColors.debt
                    )
                }
                if (state.success) {
                    Text(
                        strings.exportDone,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.debtAccentColors.repay
                    )
                }

                LoadingButton(
                    onClick = { viewModel.onIntent(ExportIntent.Export(fileExporter, fromDate, toDate)) },
                    isLoading = state.isExporting,
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = {
                        Icon(
                            Icons.Filled.Download,
                            contentDescription = null,
                            modifier = Modifier.padding(end = Dimens.space8)
                        )
                    },
                    label = { Text(strings.exportSubmit) },
                )
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
private fun ExportScreenLightPhonePreview() = DebtTrackerPreview(darkTheme = false) {
    ExportScreen(onBack = {})
}

@Preview
@Composable
private fun ExportScreenDarkPhonePreview() = DebtTrackerPreview(darkTheme = true) {
    ExportScreen(onBack = {})
}

@Preview(device = DESKTOP)
@Composable
private fun ExportScreenLightDesktopPreview() = DebtTrackerPreview(darkTheme = false) {
    ExportScreen(onBack = {})
}

@Preview(device = DESKTOP)
@Composable
private fun ExportScreenDarkDesktopPreview() = DebtTrackerPreview(darkTheme = true) {
    ExportScreen(onBack = {})
}
