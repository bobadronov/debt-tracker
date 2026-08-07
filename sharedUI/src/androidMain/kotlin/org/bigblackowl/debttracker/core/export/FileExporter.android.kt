package org.bigblackowl.debttracker.core.export

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

private const val PAGE_WIDTH = 595
private const val PAGE_HEIGHT = 842
private const val MARGIN = 32f
private const val LINE_HEIGHT = 20f

/** Writes to [Context.cacheDir] (CSV as text, PDF drawn manually via [PdfDocument]) then opens the system share sheet. */
private class AndroidFileExporter(private val context: Context) : FileExporter {

    override suspend fun saveCsv(fileName: String, content: String) {
        val file = File(context.cacheDir, fileName)
        file.writeText(content, Charsets.UTF_8)
        share(file, "text/csv")
    }

    override suspend fun savePdf(fileName: String, title: String, rows: List<ExportRow>) {
        val document = PdfDocument()
        val titlePaint = Paint().apply { textSize = 16f; isFakeBoldText = true }
        val textPaint = Paint().apply { textSize = 11f }

        var pageNumber = 1
        var page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
        var canvas = page.canvas
        var y = MARGIN + LINE_HEIGHT
        canvas.drawText(title, MARGIN, y, titlePaint)
        y += LINE_HEIGHT * 1.5f

        rows.forEach { row ->
            if (y > PAGE_HEIGHT - MARGIN) {
                document.finishPage(page)
                pageNumber++
                page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
                canvas = page.canvas
                y = MARGIN + LINE_HEIGHT
            }
            val line = "${row.date}   ${row.label}   ${row.amount}${row.comment?.let { "   $it" } ?: ""}"
            canvas.drawText(line, MARGIN, y, textPaint)
            y += LINE_HEIGHT
        }
        document.finishPage(page)

        val file = File(context.cacheDir, fileName)
        FileOutputStream(file).use { document.writeTo(it) }
        document.close()
        share(file, "application/pdf")
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
