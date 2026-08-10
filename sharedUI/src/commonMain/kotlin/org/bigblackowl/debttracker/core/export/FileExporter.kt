package org.bigblackowl.debttracker.core.export

import androidx.compose.runtime.Composable

/**
 * Android/iOS: share sheet, локальна генерація. Desktop: збереження у файл
 * (спек §6, п.8). PDF рендериться нативно на кожній платформі (Canvas/
 * UIGraphicsPDFContext/растеризація+PDFBox на Desktop) — Unicode/кирилиця
 * коректні без embedding шрифтів, бо кожна платформа використовує системний
 * текстовий рендер.
 */
interface FileExporter {
    suspend fun saveCsv(fileName: String, content: String)

    /**
     * [title] is the program header, [description] a one-line explainer of what the document is —
     * both drawn above the table. [headers] labels the table's columns (same order as [ExportRow]'s
     * date/createdAt/label/amount/comment fields).
     */
    suspend fun savePdf(fileName: String, title: String, description: String, headers: List<String>, rows: List<ExportRow>)
}

@Composable
expect fun rememberFileExporter(): FileExporter
