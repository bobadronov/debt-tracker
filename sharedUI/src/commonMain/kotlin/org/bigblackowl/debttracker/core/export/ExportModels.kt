package org.bigblackowl.debttracker.core.export

enum class ExportFormat { PDF, CSV }
enum class ExportDirection { DEBTORS, CREDITORS, BOTH }

data class ExportRow(
    val date: String,
    val label: String,
    val amount: String,
    val comment: String?,
)

private fun csvEscape(value: String): String =
    if (value.contains(',') || value.contains('"') || value.contains('\n')) {
        "\"${value.replace("\"", "\"\"")}\""
    } else {
        value
    }

/** CSV (спек §6, п.8) — UTF-8, кома як роздільник. */
fun buildCsvContent(rows: List<ExportRow>, header: List<String>): String {
    val sb = StringBuilder()
    sb.append(header.joinToString(",") { csvEscape(it) }).append('\n')
    rows.forEach { row ->
        sb.append(csvEscape(row.date)).append(',')
            .append(csvEscape(row.label)).append(',')
            .append(csvEscape(row.amount)).append(',')
            .append(csvEscape(row.comment.orEmpty()))
            .append('\n')
    }
    return sb.toString()
}
