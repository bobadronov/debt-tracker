package org.bigblackowl.debttracker.core.export

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSURL
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.writeToFile
import platform.Foundation.create
import platform.Foundation.NSTemporaryDirectory
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication

/**
 * CSV: written to `NSTemporaryDirectory()` as an `NSString`, then handed to `UIActivityViewController`.
 * PDF: built with the PdfKmp vector DSL ([buildReportDocument]) and opened in PdfKmp's own in-app
 * viewer (search, share, download) via [openReportInViewer].
 */
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
        openReportInViewer(buildReportDocument(title, description, headers, rows), fileName, title)
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
