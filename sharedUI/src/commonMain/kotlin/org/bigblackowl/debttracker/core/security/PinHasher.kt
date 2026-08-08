package org.bigblackowl.debttracker.core.security

import kotlin.random.Random

/**
 * Salts and stretches the desktop app-lock PIN (спек: PIN-фолбек лише на Desktop) before it
 * touches persistent storage, so the raw PIN never sits in the settings store (Java
 * Preferences / Windows registry) in the clear. Iteration count is a plain repeated-SHA-256
 * stretch rather than PBKDF2/HMAC — the PIN's own search space (short digit strings) is the
 * real limiting factor, so this just raises the per-guess cost without adding an HMAC
 * implementation for little extra benefit.
 */
internal object PinHasher {
    private const val ITERATIONS = 100_000
    private const val SALT_SIZE = 16

    fun newSalt(): ByteArray = Random.nextBytes(SALT_SIZE)

    fun hash(pin: String, salt: ByteArray): ByteArray {
        var digest = sha256(salt + pin.encodeToByteArray())
        repeat(ITERATIONS - 1) { digest = sha256(salt + digest) }
        return digest
    }

    fun matches(pin: String, salt: ByteArray, expectedHash: ByteArray): Boolean {
        val actual = hash(pin, salt)
        if (actual.size != expectedHash.size) return false
        var diff = 0
        for (i in actual.indices) diff = diff or (actual[i].toInt() xor expectedHash[i].toInt())
        return diff == 0
    }
}
