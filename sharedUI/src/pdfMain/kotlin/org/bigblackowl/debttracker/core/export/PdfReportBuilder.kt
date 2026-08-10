package org.bigblackowl.debttracker.core.export

import com.conamobile.pdfkmp.PdfDocument
import com.conamobile.pdfkmp.geometry.Padding
import com.conamobile.pdfkmp.pdf
import com.conamobile.pdfkmp.style.PdfColor
import com.conamobile.pdfkmp.style.TableBorder
import com.conamobile.pdfkmp.style.TableColumn
import com.conamobile.pdfkmp.unit.dp
import com.conamobile.pdfkmp.unit.sp
import com.conamobile.pdfkmp.viewer.KmpPdfLauncher

/** Shared across Android/Desktop/iOS (пре Web — pdfkmp-viewer isn't published there, see build.gradle.kts pdfMain). */
internal fun buildReportDocument(
    title: String,
    description: String,
    headers: List<String>,
    rows: List<ExportRow>,
): PdfDocument = pdf {
    metadata { this.title = title }
    page {
        spacing = 12.dp
        text(title) { fontSize = 18.sp; bold = true }
        text(description) { fontSize = 10.sp; color = PdfColor.Gray }
        spacer(height = 12.dp)
        table(
            columns = listOf(
                TableColumn.Fixed(90.dp),
                TableColumn.Weight(2f),
                TableColumn.Fixed(80.dp),
                TableColumn.Weight(2f),
            ),
            border = TableBorder(color = PdfColor.LightGray, width = 0.5.dp),
            cellPadding = Padding.symmetric(horizontal = 8.dp, vertical = 6.dp),
        ) {
            header(background = PdfColor.fromRgb(0xF5F5F5)) {
                headers.forEach { cell(it) { bold = true } }
            }
            rows.forEach { r ->
                row {
                    cell(r.date)
                    cell(r.label)
                    cell(r.amount)
                    cell(r.comment.orEmpty())
                }
            }
        }
    }
}

/** Opens [document] in PdfKmp's built-in viewer (topbar with search/share/download) — no custom screen needed. */
internal fun openReportInViewer(document: PdfDocument, fileName: String, title: String) {
    KmpPdfLauncher.open(document, title = title, fileName = fileName)
}
