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
import platform.UIKit.UIFont
import platform.UIKit.UIGraphicsBeginPDFContextToFile
import platform.UIKit.UIGraphicsBeginPDFPage
import platform.UIKit.UIGraphicsEndPDFContext
import platform.UIKit.UIGraphicsGetCurrentContext
import platform.UIKit.UILabel
import platform.UIKit.NSTextAlignmentLeft

private const val PAGE_WIDTH = 595.0
private const val PAGE_HEIGHT = 842.0
private const val MARGIN = 32.0
private const val LINE_HEIGHT = 20.0

/** Writes to `NSTemporaryDirectory()` (CSV as an NSString, PDF drawn manually via `UIGraphicsBeginPDFContextToFile`) then presents `UIActivityViewController`. */
@OptIn(ExperimentalForeignApi::class)
private class IosFileExporter : FileExporter {

    override suspend fun saveCsv(fileName: String, content: String) {
        val path = NSTemporaryDirectory() + fileName
        val nsContent = NSString.create(string = content)
        nsContent.writeToFile(path, true, NSUTF8StringEncoding, null)
        share(path)
    }

    override suspend fun savePdf(fileName: String, title: String, rows: List<ExportRow>) {
        val path = NSTemporaryDirectory() + fileName
        val bounds = CGRectMake(0.0, 0.0, PAGE_WIDTH, PAGE_HEIGHT)
        UIGraphicsBeginPDFContextToFile(path, bounds, null)

        UIGraphicsBeginPDFPage()
        var y = MARGIN
        drawLine(title, y, bold = true)
        y += LINE_HEIGHT * 1.5

        rows.forEach { row ->
            if (y > PAGE_HEIGHT - MARGIN) {
                UIGraphicsBeginPDFPage()
                y = MARGIN
            }
            val line = "${row.date}   ${row.label}   ${row.amount}${row.comment?.let { "   $it" } ?: ""}"
            drawLine(line, y, bold = false)
            y += LINE_HEIGHT
        }
        UIGraphicsEndPDFContext()
        share(path)
    }

    // CALayer.renderInContext замість NSString/NSAttributedString drawAtPoint —
    // стабільніший Kotlin/Native interop-шлях, той самий системний текстовий
    // рендер (коректна кирилиця через UIFont).
    private fun drawLine(text: String, y: Double, bold: Boolean) {
        val label = UILabel(frame = platform.CoreGraphics.CGRectMake(MARGIN, y, PAGE_WIDTH - MARGIN * 2, LINE_HEIGHT * 1.4))
        label.text = text
        label.font = if (bold) UIFont.boldSystemFontOfSize(16.0) else UIFont.systemFontOfSize(11.0)
        label.textAlignment = NSTextAlignmentLeft
        label.layer.renderInContext(UIGraphicsGetCurrentContext())
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
