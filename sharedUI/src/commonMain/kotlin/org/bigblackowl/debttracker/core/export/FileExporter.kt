package org.bigblackowl.debttracker.core.export

import androidx.compose.runtime.Composable

/**
 * CSV: FileKit's native save dialog (Android/iOS/Desktop, `pdfMain` source set) or a browser
 * download (Web, `webMain`) — локальна генерація (спек §6, п.8).
 * PDF: built with the PdfKmp vector DSL ([org.bigblackowl.debttracker.core.export.buildReportDocument],
 * `pdfMain` source set) and opened in PdfKmp's own in-app viewer instead of an immediate share/save —
 * see [savePdf].
 */
interface FileExporter {
    suspend fun saveCsv(fileName: String, content: String)

    /**
     * Builds a PDF report and opens it in an in-app viewer (search, share, download) rather than
     * saving/sharing immediately. [title] is the program header, [description] a one-line explainer
     * of what the document is — both drawn above the table. [headers] labels the table's columns
     * (same order as [ExportRow]'s date/label/amount/comment fields).
     */
    suspend fun savePdf(fileName: String, title: String, description: String, headers: List<String>, rows: List<ExportRow>)
}

@Composable
expect fun rememberFileExporter(): FileExporter
