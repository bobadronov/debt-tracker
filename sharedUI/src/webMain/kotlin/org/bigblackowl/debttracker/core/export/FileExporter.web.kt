package org.bigblackowl.debttracker.core.export

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.download
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.delay
import org.w3c.dom.HTMLIFrameElement

/**
 * CSV: FileKit's browser download (`openFileSaver` isn't published for js/wasmJs — browsers block
 * a native Save-As dialog, so FileKit's own web fallback is a direct download instead).
 * PDF: no PdfKmp target here (see build.gradle.kts's pdfMain), and a JS PDF library (e.g. jsPDF)
 * would need a bundled Unicode/Cyrillic font — its built-in fonts only cover WinAnsi. Instead this
 * renders the report as HTML into a hidden iframe and calls the browser's own print(), which lets
 * the user "Save as PDF" through the OS/browser dialog using the browser's native text rendering
 * (correct Cyrillic, no font embedding needed).
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
        val iframe = document.createElement("iframe") as HTMLIFrameElement
        iframe.style.position = "fixed"
        iframe.style.right = "0"
        iframe.style.bottom = "0"
        iframe.style.width = "0"
        iframe.style.height = "0"
        iframe.style.border = "0"
        document.body?.appendChild(iframe)

        val frameDocument = iframe.contentWindow?.document
        frameDocument?.open()
        frameDocument?.write(buildReportHtml(fileName.substringBeforeLast('.'), title, description, headers, rows))
        frameDocument?.close()

        // Let the iframe finish laying out the freshly-written document before print() reads it.
        delay(150)
        iframe.contentWindow?.focus()
        iframe.contentWindow?.print()

        // No cross-browser event fires when the print dialog closes, so remove the iframe on a
        // generous delay instead of leaking it immediately (which would blank the dialog mid-use).
        window.setTimeout({ iframe.remove(); null }, 60_000)
    }
}

private fun escapeHtml(value: String): String = value
    .replace("&", "&amp;")
    .replace("<", "&lt;")
    .replace(">", "&gt;")
    .replace("\"", "&quot;")

private fun buildReportHtml(
    documentTitle: String,
    title: String,
    description: String,
    headers: List<String>,
    rows: List<ExportRow>,
): String {
    val headerCells = headers.joinToString("") { "<th>${escapeHtml(it)}</th>" }
    val bodyRows = rows.joinToString("") { row ->
        "<tr><td>${escapeHtml(row.date)}</td><td>${escapeHtml(row.label)}</td>" +
            "<td>${escapeHtml(row.amount)}</td><td>${escapeHtml(row.comment.orEmpty())}</td></tr>"
    }
    return """
        <!DOCTYPE html>
        <html><head><meta charset="utf-8"><title>${escapeHtml(documentTitle)}</title>
        <style>
            body { font-family: Arial, Helvetica, sans-serif; padding: 24px; color: #111; }
            h1 { font-size: 18px; margin: 0 0 4px; }
            p.desc { font-size: 10px; color: #666; margin: 0 0 16px; }
            table { width: 100%; border-collapse: collapse; font-size: 11px; }
            th, td { border: 0.5px solid #ccc; padding: 6px 8px; text-align: left; }
            th { background: #f5f5f5; font-weight: bold; }
            @media print { @page { margin: 16mm; } }
        </style>
        </head><body>
        <h1>${escapeHtml(title)}</h1>
        <p class="desc">${escapeHtml(description)}</p>
        <table><thead><tr>$headerCells</tr></thead><tbody>$bodyRows</tbody></table>
        </body></html>
    """.trimIndent()
}

@Composable
actual fun rememberFileExporter(): FileExporter = remember { WebFileExporter() }
