package org.bigblackowl.debttracker.ui.components

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import org.bigblackowl.debttracker.domain.validation.UA_PHONE_NATIONAL_LEN

private const val PREFIX = "+380 "

/**
 * Renders phone entry as `+380 XX XXX XX XX`. The field itself stores only the bare 9-digit
 * national number (screens run every keystroke through
 * [org.bigblackowl.debttracker.domain.validation.sanitizePhoneInput] first), so this only has to
 * prepend `+380` and group the digits 2-3-2-2.
 */
internal class UkrainianPhoneVisualTransformation : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText {
        val digits = text.text.filter { it.isDigit() }.take(UA_PHONE_NATIONAL_LEN)

        if (digits.isEmpty()) {
            return TransformedText(
                AnnotatedString(PREFIX),
                object : OffsetMapping {
                    override fun originalToTransformed(offset: Int): Int = PREFIX.length
                    override fun transformedToOriginal(offset: Int): Int = 0
                },
            )
        }

        val out = StringBuilder(PREFIX)
        // map[t] = original digit index at transformed position t; -1 for the fixed prefix.
        val map = MutableList(PREFIX.length) { -1 }

        var idx = 0
        for (size in intArrayOf(2, 3, 2, 2)) {
            if (idx >= digits.length) break
            val end = minOf(idx + size, digits.length)
            for (i in idx until end) {
                out.append(digits[i]); map.add(i)
            }
            idx = end
            if (idx < digits.length) {
                out.append(' '); map.add(idx)
            }
        }
        while (map.size < out.length) map.add(digits.length)

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int =
                map.indexOfFirst { it >= offset }.takeIf { it >= 0 } ?: out.length

            override fun transformedToOriginal(offset: Int): Int =
                (if (offset < map.size) map[offset] else digits.length).coerceAtLeast(0)
        }

        return TransformedText(AnnotatedString(out.toString()), offsetMapping)
    }
}
