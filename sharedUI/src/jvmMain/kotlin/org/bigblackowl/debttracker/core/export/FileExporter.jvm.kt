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

    override suspend fun savePdf(fileName: String, title: String, rows: List<ExportRow>) {
        val file = chooseSaveFile(fileName) ?: return

        val lines = buildList {
            add(title to true)
            rows.forEach { row ->
                add("${row.date}   ${row.label}   ${row.amount}${row.comment?.let { "   $it" } ?: ""}" to false)
            }
        }

        val document = PDDocument()
        lines.chunked(LINES_PER_PAGE).forEach { pageLines ->
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

    private fun renderPageImage(lines: List<Pair<String, Boolean>>): BufferedImage {
        val image = BufferedImage(PAGE_WIDTH, PAGE_HEIGHT, BufferedImage.TYPE_INT_RGB)
        val g: Graphics2D = image.createGraphics()
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
        g.color = Color.WHITE
        g.fillRect(0, 0, PAGE_WIDTH, PAGE_HEIGHT)
        g.color = Color.BLACK
        var y = MARGIN + LINE_HEIGHT
        lines.forEach { (text, bold) ->
            g.font = Font("SansSerif", if (bold) Font.BOLD else Font.PLAIN, if (bold) 16 else 11)
            g.drawString(text, MARGIN.toFloat(), y.toFloat())
            y += LINE_HEIGHT
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
