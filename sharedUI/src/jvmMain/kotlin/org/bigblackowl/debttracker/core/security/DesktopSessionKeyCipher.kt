package org.bigblackowl.debttracker.core.security

import com.github.javakeyring.Keyring
import java.io.File
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

private const val KEYRING_DOMAIN = "DebtTracker"
private const val KEYRING_ACCOUNT = "session-key"
private const val TRANSFORMATION = "AES/GCM/NoPadding"
private const val GCM_TAG_LENGTH_BITS = 128
private const val GCM_IV_LENGTH_BYTES = 12
private const val AES_KEY_SIZE_BYTES = 32

/** Per-OS app-data directory — mirrors [org.bigblackowl.debttracker.data.local.buildDatabase]'s `appDataDir()`. */
private fun desktopAppDataDir(): File {
    val userHome = System.getProperty("user.home")
    val os = System.getProperty("os.name").lowercase()
    return when {
        os.contains("win") -> File(System.getenv("APPDATA") ?: userHome, "DebtTracker")
        os.contains("mac") -> File(userHome, "Library/Application Support/DebtTracker")
        else -> File(userHome, ".local/share/DebtTracker")
    }
}

/**
 * AES-256-GCM [StringCipher] for Desktop, keyed by a random per-install key — no user-entered
 * password. The key itself is stored in the OS credential store (Windows Credential Manager /
 * macOS Keychain / Linux Secret Service) via [Keyring]. When no OS keyring backend is available
 * (e.g. headless Linux without a Secret Service daemon), falls back to a random key in a local
 * file readable only by the current OS user — weaker (no OS-level encryption of the key itself),
 * but still keeps the Supabase session out of a plain, fully-readable settings file in the common
 * case, and never throws just because no keyring exists.
 */
object DesktopSessionKeyCipher : StringCipher {

    private val keyBytes: ByteArray by lazy { keyringKey() ?: fileBackedKey() }

    private fun keyringKey(): ByteArray? = try {
        Keyring.create().use { keyring ->
            val existing = runCatching { keyring.getPassword(KEYRING_DOMAIN, KEYRING_ACCOUNT) }.getOrNull()
            if (existing != null) {
                Base64.getDecoder().decode(existing)
            } else {
                val fresh = ByteArray(AES_KEY_SIZE_BYTES).also { SecureRandom().nextBytes(it) }
                keyring.setPassword(KEYRING_DOMAIN, KEYRING_ACCOUNT, Base64.getEncoder().encodeToString(fresh))
                fresh
            }
        }
    } catch (e: Exception) {
        null // No OS keyring backend available — fall back to fileBackedKey().
    }

    private fun fileBackedKey(): ByteArray {
        val file = File(desktopAppDataDir().apply { mkdirs() }, ".session_key")
        if (file.exists()) return Base64.getDecoder().decode(file.readText())
        val fresh = ByteArray(AES_KEY_SIZE_BYTES).also { SecureRandom().nextBytes(it) }
        file.writeText(Base64.getEncoder().encodeToString(fresh))
        // Best-effort — restricts the key file to the owning OS user where the filesystem supports it.
        runCatching {
            file.setReadable(false, false)
            file.setWritable(false, false)
            file.setReadable(true, true)
            file.setWritable(true, true)
        }
        return fresh
    }

    private fun secretKeySpec() = SecretKeySpec(keyBytes, "AES")

    override fun encrypt(plaintext: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION).apply { init(Cipher.ENCRYPT_MODE, secretKeySpec()) }
        val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        return Base64.getEncoder().encodeToString(cipher.iv + ciphertext)
    }

    override fun decrypt(ciphertext: String): String {
        val combined = Base64.getDecoder().decode(ciphertext)
        val iv = combined.copyOfRange(0, GCM_IV_LENGTH_BYTES)
        val actualCiphertext = combined.copyOfRange(GCM_IV_LENGTH_BYTES, combined.size)
        val cipher = Cipher.getInstance(TRANSFORMATION).apply {
            init(Cipher.DECRYPT_MODE, secretKeySpec(), GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
        }
        return cipher.doFinal(actualCiphertext).toString(Charsets.UTF_8)
    }
}
