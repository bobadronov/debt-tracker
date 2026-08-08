package org.bigblackowl.debttracker.core.security

private const val HEX_CHARS = "0123456789abcdef"

internal fun ByteArray.toHex(): String = buildString(size * 2) {
    for (byte in this@toHex) {
        val i = byte.toInt() and 0xff
        append(HEX_CHARS[i shr 4])
        append(HEX_CHARS[i and 0x0f])
    }
}

internal fun String.hexToByteArray(): ByteArray {
    require(length % 2 == 0) { "Hex string must have an even length" }
    return ByteArray(length / 2) { i ->
        ((hexDigit(this[i * 2]) shl 4) or hexDigit(this[i * 2 + 1])).toByte()
    }
}

private fun hexDigit(c: Char): Int = when (c) {
    in '0'..'9' -> c - '0'
    in 'a'..'f' -> c - 'a' + 10
    in 'A'..'F' -> c - 'A' + 10
    else -> throw IllegalArgumentException("Invalid hex character: $c")
}
