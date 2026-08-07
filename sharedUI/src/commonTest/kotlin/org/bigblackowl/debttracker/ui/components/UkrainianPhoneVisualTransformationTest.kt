package org.bigblackowl.debttracker.ui.components

import androidx.compose.ui.text.AnnotatedString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class UkrainianPhoneVisualTransformationTest {

    private val transformation = UkrainianPhoneVisualTransformation()

    private fun render(raw: String): String = transformation.filter(AnnotatedString(raw)).text.text

    @Test
    fun `empty input renders just the prefix`() {
        assertEquals("+38 ", render(""))
    }

    @Test
    fun `single digit is wrapped in open parenthesis`() {
        assertEquals("+38 (5) ", render("5"))
    }

    @Test
    fun `three digits fill the area code group`() {
        assertEquals("+38 (050) ", render("050"))
    }

    @Test
    fun `four digits start the next group without trailing space`() {
        assertEquals("+38 (050) 1", render("0501"))
    }

    @Test
    fun `ten digits render the full formatted number`() {
        assertEquals("+38 (050) 123 45 67", render("0501234567"))
    }

    @Test
    fun `digits beyond ten are truncated`() {
        assertEquals("+38 (050) 123 45 67", render("05012345679999"))
    }

    @Test
    fun `non-digit characters are ignored`() {
        // The underlying field only ever stores digits (screens filter before this runs), but the
        // transformation itself must stay robust if a non-digit character somehow slips through.
        assertEquals("+38 (050) 123 45 67", render("050-123-45-67"))
    }

    @Test
    fun `letters mixed with digits are ignored`() {
        assertEquals("+38 (5) ", render("abc5xyz"))
    }

    @Test
    fun `offset mapping does not throw for boundary offsets`() {
        val transformed = transformation.filter(AnnotatedString("0501234567"))
        val text = transformed.text.text

        val originalStart = transformed.offsetMapping.transformedToOriginal(0)
        val originalEnd = transformed.offsetMapping.transformedToOriginal(text.length)
        assertTrue(originalStart in 0..10)
        assertTrue(originalEnd in 0..10)

        val transformedStart = transformed.offsetMapping.originalToTransformed(0)
        val transformedEnd = transformed.offsetMapping.originalToTransformed(10)
        assertTrue(transformedStart in 0..text.length)
        assertTrue(transformedEnd in 0..text.length)
    }
}
