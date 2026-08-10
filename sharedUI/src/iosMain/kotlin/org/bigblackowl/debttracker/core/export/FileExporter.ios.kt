package org.bigblackowl.debttracker.core.export

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.cinterop.ExperimentalForeignApi
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSURL
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.writeToFile
import platform.Foundation.create
import platform.Foundation.NSTemporaryDirectory
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIColor
import platform.UIKit.UIFont
import platform.UIKit.UIGraphicsBeginPDFContextToFile
import platform.UIKit.UIGraphicsBeginPDFPage
import platform.UIKit.UIGraphicsEndPDFContext
import platform.UIKit.UIGraphicsGetCurrentContext
import platform.UIKit.UILabel
import platform.UIKit.UIView
import platform.UIKit.NSTextAlignmentLeft

private const val PAGE_WIDTH = 595.0
private const val PAGE_HEIGHT = 842.0
private const val MARGIN = 32.0
private const val LINE_HEIGHT = 20.0

// Fixed column x-offsets for the table (date / createdAt / contact / amount / comment), tuned to A4 width.
private val COLUMN_X = doubleArrayOf(MARGIN, MARGIN + 80.0, MARGIN + 170.0, MARGIN + 320.0, MARGIN + 400.0)
private val COLUMN_WIDTHS = DoubleArray(COLUMN_X.size) { i ->
    (if (i + 1 < COLUMN_X.size) COLUMN_X[i + 1] else PAGE_WIDTH - MARGIN) - COLUMN_X[i] - 4.0
}

/** Writes to `NSTemporaryDirectory()` (CSV as an NSString, PDF drawn manually via `UIGraphicsBeginPDFContextToFile`) then presents `UIActivityViewController`. */
@OptIn(ExperimentalForeignApi::class)
private class IosFileExporter : FileExporter {

    override suspend fun saveCsv(fileName: String, content: String) {
        val path = NSTemporaryDirectory() + fileName
        val nsContent = NSString.create(string = content)
        nsContent.writeToFile(path, true, NSUTF8StringEncoding, null)
        share(path)
    }

    override suspend fun savePdf(
        fileName: String,
        title: String,
        description: String,
        headers: List<String>,
        rows: List<ExportRow>,
    ) {
        val path = NSTemporaryDirectory() + fileName
        val bounds = CGRectMake(0.0, 0.0, PAGE_WIDTH, PAGE_HEIGHT)
        UIGraphicsBeginPDFContextToFile(path, bounds, null)

        UIGraphicsBeginPDFPage()
        var y = MARGIN
        drawLine(title, y, fontSize = 16.0, bold = true)
        y += LINE_HEIGHT
        drawLine(description, y, fontSize = 10.0, bold = false)
        y += LINE_HEIGHT * 1.5

        fun drawTableHeader() {
            drawRow(headers, y, bold = true)
            y += LINE_HEIGHT * 0.7
            drawRule(y)
            y += LINE_HEIGHT * 0.8
        }
        drawTableHeader()

        rows.forEach { row ->
            if (y > PAGE_HEIGHT - MARGIN) {
                UIGraphicsBeginPDFPage()
                y = MARGIN
                drawTableHeader()
            }
            drawRow(listOf(row.date, row.createdAt, row.label, row.amount, row.comment.orEmpty()), y, bold = false)
            y += LINE_HEIGHT
        }
        UIGraphicsEndPDFContext()
        share(path)
    }

    // CALayer.renderInContext замість NSString/NSAttributedString drawAtPoint —
    // стабільніший Kotlin/Native interop-шлях, той самий системний текстовий
    // рендер (коректна кирилиця через UIFont).
    private fun drawLine(text: String, y: Double, fontSize: Double, bold: Boolean) {
        val label = UILabel(frame = CGRectMake(MARGIN, y, PAGE_WIDTH - MARGIN * 2, LINE_HEIGHT * 1.4))
        label.text = text
        label.font = if (bold) UIFont.boldSystemFontOfSize(fontSize) else UIFont.systemFontOfSize(fontSize)
        label.textAlignment = NSTextAlignmentLeft
        label.layer.renderInContext(UIGraphicsGetCurrentContext())
    }

    private fun drawRow(cells: List<String>, y: Double, bold: Boolean) {
        cells.forEachIndexed { i, cell ->
            val label = UILabel(frame = CGRectMake(COLUMN_X[i], y, COLUMN_WIDTHS[i], LINE_HEIGHT * 1.4))
            label.text = cell
            label.font = if (bold) UIFont.boldSystemFontOfSize(11.0) else UIFont.systemFontOfSize(10.0)
            label.textAlignment = NSTextAlignmentLeft
            label.numberOfLines = 1
            label.layer.renderInContext(UIGraphicsGetCurrentContext())
        }
    }

    // A hairline UIView rendered via its layer — the same interop-safe path as drawLine/drawRow,
    // instead of driving CGContext's path-stroking API directly.
    private fun drawRule(y: Double) {
        val rule = UIView(frame = CGRectMake(MARGIN, y, PAGE_WIDTH - MARGIN * 2, 1.0))
        rule.backgroundColor = UIColor.darkGrayColor
        rule.layer.renderInContext(UIGraphicsGetCurrentContext())
    }

    private fun share(path: String) {
        val url = NSURL.fileURLWithPath(path)
        val activityController = UIActivityViewController(listOf(url), null)
        val rootViewController = UIApplication.sharedApplication.keyWindow?.rootViewController
        rootViewController?.presentViewController(activityController, animated = true, completion = null)
    }
}

@Composable
actual fun rememberFileExporter(): FileExporter = remember { IosFileExporter() }
