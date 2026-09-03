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
        assertEquals("+380 ", render(""))
    }

    @Test
    fun `single digit sits right after the prefix`() {
        assertEquals("+380 5", render("5"))
    }

    @Test
    fun `two digits fill the operator-code group`() {
        assertEquals("+380 50", render("50"))
    }

    @Test
    fun `three digits start the next group with a separator`() {
        assertEquals("+380 50 1", render("501"))
    }

    @Test
    fun `nine digits render the full formatted number`() {
        assertEquals("+380 50 123 45 67", render("501234567"))
    }

    @Test
    fun `digits beyond nine are truncated`() {
        assertEquals("+380 50 123 45 67", render("5012345679999"))
    }

    @Test
    fun `non-digit characters are ignored`() {
        assertEquals("+380 50 123 45 67", render("50-123-45-67"))
    }

    @Test
    fun `offset mapping does not throw for boundary offsets`() {
        val transformed = transformation.filter(AnnotatedString("501234567"))
        val text = transformed.text.text

        val originalStart = transformed.offsetMapping.transformedToOriginal(0)
        val originalEnd = transformed.offsetMapping.transformedToOriginal(text.length)
        assertTrue(originalStart in 0..9)
        assertTrue(originalEnd in 0..9)

        val transformedStart = transformed.offsetMapping.originalToTransformed(0)
        val transformedEnd = transformed.offsetMapping.originalToTransformed(9)
        assertTrue(transformedStart in 0..text.length)
        assertTrue(transformedEnd in 0..text.length)
    }

    @Test
    fun `cursor after the last digit maps to the end`() {
        val transformed = transformation.filter(AnnotatedString("501234567"))
        assertEquals(transformed.text.text.length, transformed.offsetMapping.originalToTransformed(9))
    }
}
