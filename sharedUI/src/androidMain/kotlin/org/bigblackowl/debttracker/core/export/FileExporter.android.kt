package org.bigblackowl.debttracker.core.export

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import java.io.File

/**
 * CSV: written to [Context.cacheDir] then handed to the system share sheet. PDF: built with the
 * PdfKmp vector DSL ([buildReportDocument]) and opened in PdfKmp's own in-app viewer (search,
 * share, download) via [openReportInViewer].
 */
private class AndroidFileExporter(private val context: Context) : FileExporter {

    override suspend fun saveCsv(fileName: String, content: String) {
        val file = File(context.cacheDir, fileName)
        file.writeText(content, Charsets.UTF_8)
        share(file, "text/csv")
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

    private fun share(file: File, mimeType: String) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(sendIntent, null).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }
}

@Composable
actual fun rememberFileExporter(): FileExporter {
    val context = LocalContext.current
    return remember(context) { AndroidFileExporter(context.applicationContext) }
}
