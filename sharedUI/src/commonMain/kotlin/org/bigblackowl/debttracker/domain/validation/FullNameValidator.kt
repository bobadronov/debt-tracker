package org.bigblackowl.debttracker.domain.validation

/** Мінімум два слова (кирилиця/латиниця), кожне ≥ 2 символи, без цифр (спек §4). */
private val NAME_WORD_REGEX = Regex("^[A-Za-zА-Яа-яЇїІіЄєҐґ]{2,}$")

fun isValidFullName(value: String): Boolean {
    val words = value.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
    return words.size >= 2 && words.all { NAME_WORD_REGEX.matches(it) }
}
