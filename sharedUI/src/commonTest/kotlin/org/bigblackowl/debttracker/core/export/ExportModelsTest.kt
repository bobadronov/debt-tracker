package org.bigblackowl.debttracker.core.export

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ExportModelsTest {

    @Test
    fun `content starts with a UTF-8 BOM`() {
        val content = buildCsvContent(emptyList(), listOf("Date", "Label"))
        assertTrue(content.startsWith("﻿"))
    }

    @Test
    fun `header is comma joined after the BOM`() {
        val content = buildCsvContent(emptyList(), listOf("Date", "Label", "Amount"))
        assertEquals("﻿Date,Label,Amount\n", content)
    }

    @Test
    fun `row renders date label amount and comment in order`() {
        val row = ExportRow(date = "2026-08-26", label = "Олена", amount = "100.00", comment = "note")
        val content = buildCsvContent(listOf(row), listOf("Date", "Label", "Amount", "Comment"))
        assertEquals("﻿Date,Label,Amount,Comment\n2026-08-26,Олена,100.00,note\n", content)
    }

    @Test
    fun `null comment renders as empty field`() {
        val row = ExportRow(date = "2026-08-26", label = "John", amount = "50", comment = null)
        val content = buildCsvContent(listOf(row), listOf("Date", "Label", "Amount", "Comment"))
        assertEquals("﻿Date,Label,Amount,Comment\n2026-08-26,John,50,\n", content)
    }

    @Test
    fun `field containing a comma is quoted`() {
        val row = ExportRow(date = "2026-08-26", label = "Smith, John", amount = "10", comment = null)
        val content = buildCsvContent(listOf(row), listOf("Date", "Label", "Amount", "Comment"))
        assertTrue(content.contains("\"Smith, John\""))
    }

    @Test
    fun `field containing a quote is escaped by doubling and wrapped in quotes`() {
        val row = ExportRow(date = "2026-08-26", label = "John \"JJ\" Smith", amount = "10", comment = null)
        val content = buildCsvContent(listOf(row), listOf("Date", "Label", "Amount", "Comment"))
        assertTrue(content.contains("\"John \"\"JJ\"\" Smith\""))
    }

    @Test
    fun `field containing a newline is wrapped in quotes`() {
        val row = ExportRow(date = "2026-08-26", label = "line1\nline2", amount = "10", comment = null)
        val content = buildCsvContent(listOf(row), listOf("Date", "Label", "Amount", "Comment"))
        assertTrue(content.contains("\"line1\nline2\""))
    }

    @Test
    fun `plain field without special characters is left unquoted`() {
        val row = ExportRow(date = "2026-08-26", label = "John Smith", amount = "10", comment = null)
        val content = buildCsvContent(listOf(row), listOf("Date", "Label", "Amount", "Comment"))
        assertTrue(content.contains("\n2026-08-26,John Smith,10,\n"))
    }

    @Test
    fun `multiple rows each end with a newline`() {
        val rows = listOf(
            ExportRow(date = "2026-08-01", label = "A", amount = "1", comment = null),
            ExportRow(date = "2026-08-02", label = "B", amount = "2", comment = null),
        )
        val content = buildCsvContent(rows, listOf("Date", "Label", "Amount", "Comment"))
        assertEquals(
            "﻿Date,Label,Amount,Comment\n2026-08-01,A,1,\n2026-08-02,B,2,\n",
            content,
        )
    }
}
