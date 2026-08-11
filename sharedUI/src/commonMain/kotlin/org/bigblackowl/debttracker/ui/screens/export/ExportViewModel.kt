package org.bigblackowl.debttracker.ui.screens.export

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.bigblackowl.debttracker.core.export.ExportDirection
import org.bigblackowl.debttracker.core.export.ExportFormat
import org.bigblackowl.debttracker.core.export.ExportRow
import org.bigblackowl.debttracker.core.export.FileExporter
import org.bigblackowl.debttracker.core.export.buildCsvContent
import org.bigblackowl.debttracker.core.i18n.resolveStrings
import org.bigblackowl.debttracker.core.settings.AppSettings
import org.bigblackowl.debttracker.domain.model.formatMoney
import org.bigblackowl.debttracker.domain.repository.AuthRepository
import org.bigblackowl.debttracker.domain.usecase.creditor.ObserveCreditorTransactionsUseCase
import org.bigblackowl.debttracker.domain.usecase.creditor.ObserveCreditorUseCase
import org.bigblackowl.debttracker.domain.usecase.creditor.ObserveCreditorsUseCase
import org.bigblackowl.debttracker.domain.usecase.debtor.ObserveDebtorTransactionsUseCase
import org.bigblackowl.debttracker.domain.usecase.debtor.ObserveDebtorUseCase
import org.bigblackowl.debttracker.domain.usecase.debtor.ObserveDebtorsUseCase
import kotlin.time.Clock

/**
 * ExportScreen's format/direction/date-range selection + the CSV/PDF pipeline (спек §6, п.8) — the
 * screen only creates [FileExporter] (composition-scoped, needs a platform file-save launcher
 * registered during composition) and forwards the user's tap into [ExportIntent.Export].
 */
class ExportViewModel(
    private val debtorId: String?,
    private val creditorId: String?,
    private val observeDebtor: ObserveDebtorUseCase,
    private val observeDebtors: ObserveDebtorsUseCase,
    private val observeDebtorTransactions: ObserveDebtorTransactionsUseCase,
    private val observeCreditor: ObserveCreditorUseCase,
    private val observeCreditors: ObserveCreditorsUseCase,
    private val observeCreditorTransactions: ObserveCreditorTransactionsUseCase,
    private val authRepository: AuthRepository,
    private val appSettings: AppSettings,
) : ViewModel() {

    /** Reached from a Debtor/Creditor detail screen — export only that one contact's history
     * instead of everyone's, since that's what "Export" meant to the user there. */
    val isScoped: Boolean get() = debtorId != null || creditorId != null

    private val _state = MutableStateFlow(ExportState())
    val state: StateFlow<ExportState> = _state.asStateFlow()

    init {
        if (isScoped) {
            viewModelScope.launch {
                val name = when {
                    debtorId != null -> observeDebtor(debtorId).first()?.fullName
                    creditorId != null -> observeCreditor(creditorId).first()?.fullName
                    else -> null
                }
                _state.update { it.copy(scopedContactName = name) }
            }
        }
    }

    fun onIntent(intent: ExportIntent) {
        when (intent) {
            is ExportIntent.SetFormat -> _state.update { it.copy(format = intent.format) }
            is ExportIntent.SetDirection -> _state.update { it.copy(direction = intent.direction) }
            is ExportIntent.Export -> export(intent.fileExporter, intent.fromDate, intent.toDate)
        }
    }

    private fun export(fileExporter: FileExporter, fromDate: LocalDate?, toDate: LocalDate?) {
        viewModelScope.launch {
            _state.update { it.copy(error = null, success = false, isExporting = true) }
            val strings = resolveStrings(appSettings.locale)
            val exportUserName = authRepository.displayName.value ?: authRepository.email.value ?: strings.settingsLocalOnly
            val format = _state.value.format
            val direction = _state.value.direction

            runCatching {
                val rows = collectExportRows(direction, fromDate, toDate)
                val fileDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
                val headers = listOf(strings.csvHeaderDate, strings.csvHeaderContact, strings.csvHeaderAmount, strings.csvHeaderComment)
                when (format) {
                    ExportFormat.CSV -> fileExporter.saveCsv(
                        "${strings.appName}_${exportUserName}_$fileDate.csv",
                        buildCsvContent(rows, headers),
                    )

                    ExportFormat.PDF -> fileExporter.savePdf(
                        "${strings.appName}_${exportUserName}_$fileDate.pdf",
                        strings.appName,
                        strings.exportPdfDescription(exportUserName),
                        headers,
                        rows,
                    )
                }
            }.onSuccess {
                _state.update { it.copy(isExporting = false, success = true) }
            }.onFailure {
                _state.update { it.copy(isExporting = false, error = strings.exportError) }
            }
        }
    }

    private suspend fun collectExportRows(
        direction: ExportDirection,
        fromDate: LocalDate?,
        toDate: LocalDate?,
    ): List<ExportRow> {
        val timeZone = TimeZone.currentSystemDefault()
        val rows = mutableListOf<ExportRow>()

        fun inRange(localDate: LocalDate) =
            (fromDate == null || localDate >= fromDate) && (toDate == null || localDate <= toDate)

        when {
            debtorId != null -> {
                val debtor = observeDebtor(debtorId).first() ?: return emptyList()
                observeDebtorTransactions(debtorId).first().forEach { tx ->
                    val date = tx.date.toLocalDateTime(timeZone).date
                    if (inRange(date)) {
                        rows.add(ExportRow(date.toString(), debtor.fullName, tx.amount.formatMoney(debtor.currency), tx.comment))
                    }
                }
            }

            creditorId != null -> {
                val creditor = observeCreditor(creditorId).first() ?: return emptyList()
                observeCreditorTransactions(creditorId).first().forEach { tx ->
                    val date = tx.date.toLocalDateTime(timeZone).date
                    if (inRange(date)) {
                        rows.add(ExportRow(date.toString(), creditor.fullName, tx.amount.formatMoney(creditor.currency), tx.comment))
                    }
                }
            }

            else -> {
                if (direction == ExportDirection.DEBTORS || direction == ExportDirection.BOTH) {
                    observeDebtors().first().forEach { item ->
                        observeDebtorTransactions(item.debtor.id).first().forEach { tx ->
                            val date = tx.date.toLocalDateTime(timeZone).date
                            if (inRange(date)) {
                                rows.add(ExportRow(date.toString(), item.debtor.fullName, tx.amount.formatMoney(item.debtor.currency), tx.comment))
                            }
                        }
                    }
                }
                if (direction == ExportDirection.CREDITORS || direction == ExportDirection.BOTH) {
                    observeCreditors().first().forEach { item ->
                        observeCreditorTransactions(item.creditor.id).first().forEach { tx ->
                            val date = tx.date.toLocalDateTime(timeZone).date
                            if (inRange(date)) {
                                rows.add(ExportRow(date.toString(), item.creditor.fullName, tx.amount.formatMoney(item.creditor.currency), tx.comment))
                            }
                        }
                    }
                }
            }
        }
        return rows.sortedBy { it.date }
    }
}
