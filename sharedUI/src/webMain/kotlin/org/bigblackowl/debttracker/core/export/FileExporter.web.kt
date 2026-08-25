package org.bigblackowl.debttracker.core.export

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import io.github.aakira.napier.Napier
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.download

/**
 * CSV: FileKit's browser download (`openFileSaver` isn't published for js/wasmJs — browsers block
 * a native Save-As dialog, so FileKit's own web fallback is a direct download instead).
 * PDF: unavailable — pdfkmp-viewer/PdfKmp publish no js/wasmJs artifacts (see build.gradle.kts's
 * pdfMain), so there's no vector DSL to build the document from on Web.
 */
private class WebFileExporter : FileExporter {
    override suspend fun saveCsv(fileName: String, content: String) {
        FileKit.download(bytes = content.encodeToByteArray(), fileName = fileName)
    }

    override suspend fun savePdf(
        fileName: String,
        title: String,
        description: String,
        headers: List<String>,
        rows: List<ExportRow>,
    ) {
        Napier.w(tag = "FileExporter") { "savePdf($fileName): PDF export isn't available on Web (no PdfKmp target)" }
    }
}

@Composable
actual fun rememberFileExporter(): FileExporter = remember { WebFileExporter() }
