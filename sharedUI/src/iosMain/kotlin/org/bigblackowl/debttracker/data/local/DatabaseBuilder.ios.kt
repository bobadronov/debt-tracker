package org.bigblackowl.debttracker.data.local

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.Dispatchers
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSFileProtectionComplete
import platform.Foundation.NSFileProtectionKey
import platform.Foundation.NSUserDomainMask

@OptIn(ExperimentalForeignApi::class)
private fun documentDirectory(): String {
    val url = NSFileManager.defaultManager.URLForDirectory(
        directory = NSDocumentDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = false,
        error = null,
    )
    return requireNotNull(url?.path)
}

/**
 * Marks [path] as only readable while the device is unlocked (`NSFileProtectionComplete`) — iOS's
 * built-in Data Protection, encrypted at rest tied to the device passcode/Secure Enclave. No
 * app-level cipher needed, and no-op (returns quietly) if [path] doesn't exist yet.
 */
@OptIn(ExperimentalForeignApi::class)
private fun protectFile(path: String) {
    NSFileManager.defaultManager.setAttributes(
        attributes = mapOf(NSFileProtectionKey to NSFileProtectionComplete),
        ofItemAtPath = path,
        error = null,
    )
}

/** Opens (or creates) the app's Room database file in the app's Documents directory. */
fun buildDatabase(): DebtTrackerDatabase {
    val dbFilePath = documentDirectory() + "/debt_tracker.db"
    return Room.databaseBuilder<DebtTrackerDatabase>(name = dbFilePath)
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.Default)
        .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
        .build()
        .also {
            // WAL/SHM sidecars are created lazily on first write, so may not exist yet on a fresh
            // install — harmless no-op in that case, protected from the next app launch onward.
            protectFile(dbFilePath)
            protectFile("$dbFilePath-wal")
            protectFile("$dbFilePath-shm")
        }
}
