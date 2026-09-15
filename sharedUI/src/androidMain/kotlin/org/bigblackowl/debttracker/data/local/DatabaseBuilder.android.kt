package org.bigblackowl.debttracker.data.local

import android.content.Context
import android.util.Base64
import android.util.Log
import androidx.room.Room
import kotlinx.coroutines.Dispatchers
import net.zetetic.database.sqlcipher.SQLiteDatabase
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import org.bigblackowl.debttracker.core.security.AndroidKeystoreStringCipher
import java.io.File
import java.security.SecureRandom

// SPIKE (B3 — Room DB encryption): SQLCipher via the legacy SupportSQLiteOpenHelper bridge
// instead of BundledSQLiteDriver. Passphrase is a random base64 string (safe to embed literally in
// the migration's ATTACH...KEY SQL below — no quotes/NULs in its alphabet), Keystore-encrypted at
// rest via the same AndroidKeystoreStringCipher used for the Supabase session token.
private const val DB_KEY_PREFS = "debt_tracker_db_key"
private const val DB_KEY_PREF_NAME = "key"
private const val TAG = "DbEncryptionSpike"

private fun dbPassphrase(context: Context): String {
    val prefs = context.getSharedPreferences(DB_KEY_PREFS, Context.MODE_PRIVATE)
    prefs.getString(DB_KEY_PREF_NAME, null)?.let { return AndroidKeystoreStringCipher.decrypt(it) }
    val passphrase = Base64.encodeToString(ByteArray(32).also { SecureRandom().nextBytes(it) }, Base64.NO_WRAP)
    prefs.edit().putString(DB_KEY_PREF_NAME, AndroidKeystoreStringCipher.encrypt(passphrase)).apply()
    return passphrase
}

// "SQLite format 3" + a trailing NUL byte, the fixed magic header of an unencrypted SQLite file —
// built via byteArrayOf rather than a string literal so the NUL never sits inside source text.
private val PLAINTEXT_SQLITE_HEADER = "SQLite format 3".toByteArray(Charsets.US_ASCII) + byteArrayOf(0)

private fun isPlaintextSqlite(file: File): Boolean {
    if (!file.exists() || file.length() < 16) return false
    val header = ByteArray(16)
    file.inputStream().use { it.read(header) }
    return header.contentEquals(PLAINTEXT_SQLITE_HEADER)
}

/**
 * One-time re-encryption of a pre-existing plaintext Room DB (from before SQLCipher was adopted).
 * SQLCipher silently opens a plaintext file as unencrypted if you just hand it a key — it does NOT
 * encrypt it in place — so switching the builder alone would leave existing installs' data
 * unprotected forever. `sqlcipher_export()` clones schema+data into a freshly keyed DB; `PRAGMA
 * user_version` is deliberately NOT part of that export (Zetetic's own migration docs call this
 * out) and must be copied by hand or Room would see version 0 and misbehave on next open.
 *
 * The new encrypted file is created via [SQLiteDatabase.openOrCreateDatabase] (the same call shape
 * as a normal keyed open) and made "main"; the *existing* plaintext file is then ATTACHed (with an
 * empty key, meaning "don't decrypt, it's plain") and exported into "main". Doing it the other way
 * around — main = plaintext, ATTACH a brand-new encrypted file — hits an emulator-observed
 * `os_unix.c open() ENOENT` when SQLite's path canonicalization resolves the new file's `/data/user/0/…`
 * path to `/data/data/…` inside ATTACH's create path specifically; attaching an *existing* file (no
 * creation involved) doesn't hit it. ATTACH's filename/KEY clauses must also be literal SQL text,
 * not bound `?` parameters — binding them silently no-ops (SQLite then tries to attach a file
 * literally named "?"). Safe to inline here only because [passphrase] is base64 (no `'`) and the
 * path is Android's own app-private dir, not attacker-controlled input.
 */
private fun migratePlaintextDbIfNeeded(dbFile: File, passphrase: String) {
    if (!isPlaintextSqlite(dbFile)) return
    Log.i(TAG, "Found plaintext DB, migrating to SQLCipher")

    val tmpFile = File(dbFile.parentFile, "${dbFile.name}.sqlcipher_tmp")
    tmpFile.delete()
    File(dbFile.parentFile, "${tmpFile.name}-wal").delete()
    File(dbFile.parentFile, "${tmpFile.name}-shm").delete()

    val encryptedDb = SQLiteDatabase.openOrCreateDatabase(tmpFile, passphrase, null, null)
    try {
        val escapedSourcePath = dbFile.absolutePath.replace("'", "''")
        encryptedDb.execSQL("ATTACH DATABASE '$escapedSourcePath' AS plaintext KEY ''")
        val userVersion = encryptedDb.rawQuery("PRAGMA plaintext.user_version", null).use { c ->
            c.moveToFirst()
            c.getInt(0)
        }
        Log.i(TAG, "Source user_version=$userVersion")

        encryptedDb.rawQuery("SELECT sqlcipher_export('main', 'plaintext')", null).use { it.moveToFirst() }
        encryptedDb.execSQL("PRAGMA main.user_version = $userVersion")
        encryptedDb.execSQL("DETACH DATABASE plaintext")
    } finally {
        encryptedDb.close()
    }

    // Drop the old plaintext main file + its WAL/SHM siblings, then swap the migrated file in.
    File(dbFile.parentFile, "${dbFile.name}-wal").delete()
    File(dbFile.parentFile, "${dbFile.name}-shm").delete()
    dbFile.delete()
    check(tmpFile.renameTo(dbFile)) { "Failed to move migrated DB into place" }
    Log.i(TAG, "Migration complete")
}

/** Opens (or creates) the app's Room database file in the app's private data directory. */
fun buildDatabase(context: Context): DebtTrackerDatabase {
    System.loadLibrary("sqlcipher") // sqlcipher-android ships no loadLibs() helper — must be loaded explicitly
    val appContext = context.applicationContext
    val dbFile = appContext.getDatabasePath("debt_tracker.db")
    val passphrase = dbPassphrase(appContext)
    migratePlaintextDbIfNeeded(dbFile, passphrase)
    return Room.databaseBuilder<DebtTrackerDatabase>(
        context = appContext,
        name = dbFile.absolutePath
    )
        .openHelperFactory(SupportOpenHelperFactory(passphrase.toByteArray(Charsets.UTF_8)))
        .setQueryCoroutineContext(Dispatchers.IO)
        .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
        .build()
}
