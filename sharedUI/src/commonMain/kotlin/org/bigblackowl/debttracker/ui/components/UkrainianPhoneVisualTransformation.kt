package org.bigblackowl.debttracker.ui.components

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

/** Показує ввід номера телефону у вигляді "+38 (XXX) XXX XX XX"; поле зберігає лише цифри. */
internal class UkrainianPhoneVisualTransformation : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText {
        val trimmed = text.text.filter { it.isDigit() }.take(10)

        if (trimmed.isEmpty()) {
            return TransformedText(
                AnnotatedString("+38 "),
                object : OffsetMapping {
                    override fun originalToTransformed(offset: Int): Int = 4
                    override fun transformedToOriginal(offset: Int): Int = 0
                }
            )
        }

        val builder = StringBuilder("+38 ")
        val mapping = mutableListOf<Int>()

        // Мапимо "+38 " на позицію 0
        repeat(4) { mapping.add(0) }

        var digitIndex = 0

        // Обробка (XXX) для короткого вводу
        if (trimmed.length <= 3) {
            builder.append("(")
            mapping.add(0) // Що відкриває дужка
            for (i in trimmed.indices) {
                builder.append(trimmed[i])
                mapping.add(i) // Мапимо цифру на її позицію
            }
            builder.append(") ")
            mapping.add(trimmed.length) // Закриваюча дужка
            mapping.add(trimmed.length) // Пробіл після
            return TransformedText(
                AnnotatedString(builder.toString()),
                object : OffsetMapping {
                    override fun originalToTransformed(offset: Int): Int =
                        mapping.indexOfFirst { it >= offset }.takeIf { it >= 0 } ?: builder.length

                    override fun transformedToOriginal(offset: Int): Int =
                        if (offset < mapping.size) mapping[offset] else trimmed.length
                }
            )
        }

        // Повний формат: +38 (XXX) XXX XX XX
        builder.append("(${trimmed.take(3)}) ")
        mapping.add(0) // Що відкриває дужка
        repeat(3) { mapping.add(digitIndex + it) } // Цифри 0-2
        mapping.add(digitIndex + 3) // Закриваюча дужка
        mapping.add(digitIndex + 3) // Пробіл після
        digitIndex += 3

        // Додаємо блоки: XXX, XX, XX
        fun appendBlock(count: Int, spaceAfter: Boolean = true) {
            val block = trimmed.drop(digitIndex).take(count)
            builder.append(block)
            repeat(block.length) { mapping.add(digitIndex + it) } // Мапимо цифри на їх позиції
            digitIndex += block.length
            if (block.length == count && spaceAfter && digitIndex < trimmed.length) {
                builder.append(" ")
                mapping.add(digitIndex)
            }
        }

        appendBlock(3) // XXX
        appendBlock(2) // XX
        appendBlock(2, spaceAfter = false) // XX

        while (mapping.size < builder.length) {
            mapping.add(trimmed.length)
        }

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int =
                mapping.indexOfFirst { it >= offset }.takeIf { it >= 0 } ?: builder.length

            override fun transformedToOriginal(offset: Int): Int =
                if (offset < mapping.size) mapping[offset] else trimmed.length
        }

        return TransformedText(
            AnnotatedString(builder.toString()),
            offsetMapping
        )
    }
}
