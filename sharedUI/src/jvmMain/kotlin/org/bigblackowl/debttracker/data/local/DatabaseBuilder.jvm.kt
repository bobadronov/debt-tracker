package org.bigblackowl.debttracker.data.local

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import java.io.File

/** Per-OS app-data directory: `%APPDATA%/DebtTracker` (Windows), `~/Library/Application Support/DebtTracker` (macOS), `~/.local/share/DebtTracker` (Linux). */
private fun appDataDir(): File {
    val userHome = System.getProperty("user.home")
    val os = System.getProperty("os.name").lowercase()
    return when {
        os.contains("win") -> File(System.getenv("APPDATA") ?: userHome, "DebtTracker")
        os.contains("mac") -> File(userHome, "Library/Application Support/DebtTracker")
        else -> File(userHome, ".local/share/DebtTracker")
    }
}

/** Opens (or creates) the app's Room database file in [appDataDir]. */
fun buildDatabase(): DebtTrackerDatabase {
    val dir = appDataDir().apply { mkdirs() }
    val dbFile = File(dir, "debt_tracker.db")
    return Room.databaseBuilder<DebtTrackerDatabase>(name = dbFile.absolutePath)
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
        .build()
}
