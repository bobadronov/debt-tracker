package org.bigblackowl.debttracker.core.export

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import java.io.File
import javax.swing.JFileChooser

/**
 * CSV: written through a native **Save As** dialog. PDF: built with the PdfKmp vector DSL
 * ([buildReportDocument]) and opened in PdfKmp's own Compose-for-Desktop viewer window
 * (search, share, download) via [openReportInViewer].
 */
private class DesktopFileExporter : FileExporter {

    override suspend fun saveCsv(fileName: String, content: String) {
        val file = chooseSaveFile(fileName) ?: return
        file.writeText(content, Charsets.UTF_8)
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

    private fun chooseSaveFile(suggestedName: String): File? {
        val chooser = JFileChooser().apply { selectedFile = File(suggestedName) }
        val result = chooser.showSaveDialog(null)
        return if (result == JFileChooser.APPROVE_OPTION) chooser.selectedFile else null
    }
}

@Composable
actual fun rememberFileExporter(): FileExporter = remember { DesktopFileExporter() }
