package org.bigblackowl.debttracker.core.export

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory
import java.awt.Color
import java.awt.Font
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.File
import javax.swing.JFileChooser

private const val PAGE_WIDTH = 595
private const val PAGE_HEIGHT = 842
private const val MARGIN = 32
private const val LINE_HEIGHT = 22
private const val LINES_PER_PAGE = (PAGE_HEIGHT - 2 * MARGIN) / LINE_HEIGHT

// Fixed column x-offsets for the table (date / createdAt / contact / amount / comment), tuned to A4 width.
private val COLUMN_X = intArrayOf(MARGIN, MARGIN + 80, MARGIN + 170, MARGIN + 320, MARGIN + 400)

private sealed interface PdfLine {
    data class Heading(val text: String, val bold: Boolean, val fontSize: Int) : PdfLine
    data class Row(val cells: List<String>, val bold: Boolean) : PdfLine
    data object Rule : PdfLine
}

/**
 * Desktop немає вбудованого PDF-writer'а в JDK (спек §6, п.8). Кожна
 * сторінка рендериться як растрове зображення через Java2D (Graphics2D —
 * коректний Unicode/кирилиця через системні шрифти) і вкладається в PDF
 * через PDFBox — без embedding TTF-шрифтів, тому текст не виділяється
 * мишею, але кирилиця відображається правильно на будь-якій ОС.
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
        val file = chooseSaveFile(fileName) ?: return

        val headingLines = listOf(
            PdfLine.Heading(title, bold = true, fontSize = 16),
            PdfLine.Heading(description, bold = false, fontSize = 10),
        )
        val tableHeaderLines = listOf(PdfLine.Row(headers, bold = true), PdfLine.Rule)
        val dataLines = rows.map {
            PdfLine.Row(listOf(it.date, it.createdAt, it.label, it.amount, it.comment.orEmpty()), bold = false)
        }

        val linesPerDataPage = LINES_PER_PAGE - tableHeaderLines.size
        val linesPerFirstPage = linesPerDataPage - headingLines.size

        val pages = mutableListOf<List<PdfLine>>()
        var remaining = dataLines
        pages.add(headingLines + tableHeaderLines + remaining.take(linesPerFirstPage))
        remaining = remaining.drop(linesPerFirstPage)
        while (remaining.isNotEmpty()) {
            pages.add(tableHeaderLines + remaining.take(linesPerDataPage))
            remaining = remaining.drop(linesPerDataPage)
        }

        val document = PDDocument()
        pages.forEach { pageLines ->
            val image = renderPageImage(pageLines)
            val page = PDPage(PDRectangle(PAGE_WIDTH.toFloat(), PAGE_HEIGHT.toFloat()))
            document.addPage(page)
            val pdImage = LosslessFactory.createFromImage(document, image)
            PDPageContentStream(document, page).use { stream ->
                stream.drawImage(pdImage, 0f, 0f, PAGE_WIDTH.toFloat(), PAGE_HEIGHT.toFloat())
            }
        }
        document.save(file)
        document.close()
    }

    private fun renderPageImage(lines: List<PdfLine>): BufferedImage {
        val image = BufferedImage(PAGE_WIDTH, PAGE_HEIGHT, BufferedImage.TYPE_INT_RGB)
        val g: Graphics2D = image.createGraphics()
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
        g.color = Color.WHITE
        g.fillRect(0, 0, PAGE_WIDTH, PAGE_HEIGHT)
        g.color = Color.BLACK
        var y = MARGIN + LINE_HEIGHT
        lines.forEach { line ->
            when (line) {
                is PdfLine.Heading -> {
                    g.font = Font("SansSerif", if (line.bold) Font.BOLD else Font.PLAIN, line.fontSize)
                    g.drawString(line.text, MARGIN.toFloat(), y.toFloat())
                    y += LINE_HEIGHT
                }

                is PdfLine.Row -> {
                    g.font = Font("SansSerif", if (line.bold) Font.BOLD else Font.PLAIN, if (line.bold) 11 else 10)
                    line.cells.forEachIndexed { i, cell -> g.drawString(cell, COLUMN_X[i].toFloat(), y.toFloat()) }
                    y += LINE_HEIGHT
                }

                PdfLine.Rule -> {
                    g.drawLine(MARGIN, y - LINE_HEIGHT / 2, PAGE_WIDTH - MARGIN, y - LINE_HEIGHT / 2)
                }
            }
        }
        g.dispose()
        return image
    }

    private fun chooseSaveFile(suggestedName: String): File? {
        val chooser = JFileChooser().apply { selectedFile = File(suggestedName) }
        val result = chooser.showSaveDialog(null)
        return if (result == JFileChooser.APPROVE_OPTION) chooser.selectedFile else null
    }
}

@Composable
actual fun rememberFileExporter(): FileExporter = remember { DesktopFileExporter() }
