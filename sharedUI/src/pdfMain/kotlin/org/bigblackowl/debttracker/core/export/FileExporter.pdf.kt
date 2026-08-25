package org.bigblackowl.debttracker.core.export

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.openFileSaver
import io.github.vinceglb.filekit.writeString

/**
 * Shared across Android/Desktop/iOS (not Web — `openFileSaver` isn't published there; see
 * [FileExporter.web.kt][org.bigblackowl.debttracker.core.export] and build.gradle.kts's pdfMain).
 * CSV: FileKit's native save dialog. PDF: built with the PdfKmp vector DSL
 * ([buildReportDocument]) and opened in PdfKmp's own in-app viewer via [openReportInViewer].
 */
private class PdfCapableFileExporter : FileExporter {

    override suspend fun saveCsv(fileName: String, content: String) {
        val file = FileKit.openFileSaver(
            suggestedName = fileName.substringBeforeLast('.'),
            defaultExtension = fileName.substringAfterLast('.', "csv"),
        ) ?: return
        file.writeString(content)
    }

    override suspend fun savePdf(
        fileName: String,
        title: String,
        description: String,
        headers: List<String>,
        rows: List<ExportRow>,
    ) {
        openReportInViewer(buildReportDocument(title, description, headers, rows), fileName, title)
    }
}

@Composable
actual fun rememberFileExporter(): FileExporter = remember { PdfCapableFileExporter() }
