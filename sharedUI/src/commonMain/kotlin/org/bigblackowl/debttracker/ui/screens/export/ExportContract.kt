package org.bigblackowl.debttracker.ui.screens.export

import kotlinx.datetime.LocalDate
import org.bigblackowl.debttracker.core.export.ExportDirection
import org.bigblackowl.debttracker.core.export.ExportFormat
import org.bigblackowl.debttracker.core.export.FileExporter

data class ExportState(
    val format: ExportFormat = ExportFormat.CSV,
    val direction: ExportDirection = ExportDirection.BOTH,
    /** Contact name for a debtor/creditor-scoped export; null while loading or when not scoped. */
    val scopedContactName: String? = null,
    val isExporting: Boolean = false,
    val error: String? = null,
    val success: Boolean = false,
)

sealed interface ExportIntent {
    data class SetFormat(val format: ExportFormat) : ExportIntent
    data class SetDirection(val direction: ExportDirection) : ExportIntent
    data class Export(val fileExporter: FileExporter, val fromDate: LocalDate?, val toDate: LocalDate?) : ExportIntent
}
