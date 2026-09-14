package org.bigblackowl.debttracker.core.export

import net.codinux.csv.writer.CsvWriter
import net.codinux.csv.writer.LineDelimiter

enum class ExportFormat { PDF, CSV }
enum class ExportDirection { DEBTORS, CREDITORS, BOTH }

data class ExportRow(
    val date: String,
    val label: String,
    val amount: String,
    val comment: String?,
)

/**
 * CSV (spec §6, item 8) — UTF-8, comma as the separator. Leads with a UTF-8 BOM so Excel (which guesses
 * the system codepage for a BOM-less file) renders Cyrillic content correctly instead of mojibake.
 */
fun buildCsvContent(rows: List<ExportRow>, header: List<String>): String {
    val sb = StringBuilder("﻿")
    val writer = CsvWriter.builder(lineDelimiter = LineDelimiter.LF).writer(sb)
    writer.writeRow(header)
    rows.forEach { row ->
        writer.writeRow(row.date, row.label, row.amount, row.comment.orEmpty())
    }
    return sb.toString()
}
