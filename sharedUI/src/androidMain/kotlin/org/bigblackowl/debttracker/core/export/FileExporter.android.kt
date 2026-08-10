package org.bigblackowl.debttracker.core.export

import android.content.Context
import android.content.Intent
import android.graphics.Color
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

// Fixed column x-offsets for the table (date / createdAt / contact / amount / comment), tuned to A4 width.
private val COLUMN_X = floatArrayOf(MARGIN, MARGIN + 80f, MARGIN + 170f, MARGIN + 320f, MARGIN + 400f)

/** Writes to [Context.cacheDir] (CSV as text, PDF drawn manually via [PdfDocument]) then opens the system share sheet. */
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
        val document = PdfDocument()
        val titlePaint = Paint().apply { textSize = 16f; isFakeBoldText = true }
        val descriptionPaint = Paint().apply { textSize = 10f; color = Color.DKGRAY }
        val headerPaint = Paint().apply { textSize = 11f; isFakeBoldText = true }
        val linePaint = Paint().apply { strokeWidth = 1f; color = Color.DKGRAY }
        val textPaint = Paint().apply { textSize = 10f }

        var pageNumber = 1
        var page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
        var canvas = page.canvas
        var y = MARGIN + LINE_HEIGHT
        canvas.drawText(title, MARGIN, y, titlePaint)
        y += LINE_HEIGHT
        canvas.drawText(description, MARGIN, y, descriptionPaint)
        y += LINE_HEIGHT * 1.5f

        fun drawTableHeader() {
            headers.forEachIndexed { i, header -> canvas.drawText(header, COLUMN_X[i], y, headerPaint) }
            y += LINE_HEIGHT * 0.5f
            canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, linePaint)
            y += LINE_HEIGHT
        }
        drawTableHeader()

        rows.forEach { row ->
            if (y > PAGE_HEIGHT - MARGIN) {
                document.finishPage(page)
                pageNumber++
                page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
                canvas = page.canvas
                y = MARGIN + LINE_HEIGHT
                drawTableHeader()
            }
            val cells = listOf(row.date, row.createdAt, row.label, row.amount, row.comment.orEmpty())
            cells.forEachIndexed { i, cell -> canvas.drawText(cell, COLUMN_X[i], y, textPaint) }
            y += LINE_HEIGHT
        }
        canvas.drawLine(MARGIN, y - LINE_HEIGHT * 0.6f, PAGE_WIDTH - MARGIN, y - LINE_HEIGHT * 0.6f, linePaint)
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
