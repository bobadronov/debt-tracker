package org.bigblackowl.debttracker.data.local

import android.content.Context
import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers

/** Opens (or creates) the app's Room database file in the app's private data directory. */
fun buildDatabase(context: Context): DebtTrackerDatabase {
    val appContext = context.applicationContext
    val dbFile = appContext.getDatabasePath("debt_tracker.db")
    return Room.databaseBuilder<DebtTrackerDatabase>(
        context = appContext,
        name = dbFile.absolutePath
    )
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
        .build()
}
