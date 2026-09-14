@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package org.bigblackowl.debttracker.data.local

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import org.bigblackowl.debttracker.data.local.dao.CreditorDao
import org.bigblackowl.debttracker.data.local.dao.CreditorTransactionDao
import org.bigblackowl.debttracker.data.local.dao.DebtTransactionDao
import org.bigblackowl.debttracker.data.local.dao.DebtorDao
import org.bigblackowl.debttracker.data.local.entity.CreditorEntity
import org.bigblackowl.debttracker.data.local.entity.CreditorTransactionEntity
import org.bigblackowl.debttracker.data.local.entity.DebtTransactionEntity
import org.bigblackowl.debttracker.data.local.entity.DebtorEntity

@Database(
    entities = [
        DebtorEntity::class,
        DebtTransactionEntity::class,
        CreditorEntity::class,
        CreditorTransactionEntity::class,
    ],
    version = 6,
)
@TypeConverters(Converters::class)
@ConstructedBy(DebtTrackerDatabaseConstructor::class)
abstract class DebtTrackerDatabase : RoomDatabase() {
    abstract fun debtorDao(): DebtorDao
    abstract fun debtTransactionDao(): DebtTransactionDao
    abstract fun creditorDao(): CreditorDao
    abstract fun creditorTransactionDao(): CreditorTransactionDao
}

/** v1 → v2: added multi-currency support (spec — UAH/USD/PLN/EUR) — `currency` column on creditors/debtors, defaulting to UAH. */
val MIGRATION_1_2: Migration = object : Migration(1, 2) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE creditors ADD COLUMN currency TEXT NOT NULL DEFAULT 'UAH'")
        connection.execSQL("ALTER TABLE debtors ADD COLUMN currency TEXT NOT NULL DEFAULT 'UAH'")
    }
}

/** v2 → v3: `email` column on creditors/debtors — used to look up a registered user's profile (autofill). */
val MIGRATION_2_3: Migration = object : Migration(2, 3) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE creditors ADD COLUMN email TEXT")
        connection.execSQL("ALTER TABLE debtors ADD COLUMN email TEXT")
    }
}

/** v3 → v4: columns for two-way sync with a mirrored account (linked_user_id/mirror_*_id) — spec §7, migration 0007. */
val MIGRATION_3_4: Migration = object : Migration(3, 4) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE debtors ADD COLUMN linkedUserId TEXT")
        connection.execSQL("ALTER TABLE debtors ADD COLUMN mirrorCreditorId TEXT")
        connection.execSQL("ALTER TABLE creditors ADD COLUMN linkedUserId TEXT")
        connection.execSQL("ALTER TABLE creditors ADD COLUMN mirrorDebtorId TEXT")
        connection.execSQL("ALTER TABLE debt_transactions ADD COLUMN mirrorTransactionId TEXT")
        connection.execSQL("ALTER TABLE creditor_transactions ADD COLUMN mirrorTransactionId TEXT")
    }
}

/** v4 → v5: removed the "last card digits" feature (cardLastDigits) — the column is no longer collected/shown. */
val MIGRATION_4_5: Migration = object : Migration(4, 5) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE debt_transactions DROP COLUMN cardLastDigits")
        connection.execSQL("ALTER TABLE creditor_transactions DROP COLUMN cardLastDigits")
    }
}

/** v5 → v6: per-contact `dueDate` + `reminderLeadDays` (debt repayment reminders) on creditors/debtors — migration 0010. */
val MIGRATION_5_6: Migration = object : Migration(5, 6) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE debtors ADD COLUMN dueDate INTEGER")
        connection.execSQL("ALTER TABLE debtors ADD COLUMN reminderLeadDays TEXT NOT NULL DEFAULT ''")
        connection.execSQL("ALTER TABLE creditors ADD COLUMN dueDate INTEGER")
        connection.execSQL("ALTER TABLE creditors ADD COLUMN reminderLeadDays TEXT NOT NULL DEFAULT ''")
    }
}

/** Room KSP generates the `actual` implementation on each platform (android/jvm/iosArm64/iosSimulatorArm64). */
expect object DebtTrackerDatabaseConstructor : RoomDatabaseConstructor<DebtTrackerDatabase> {
    override fun initialize(): DebtTrackerDatabase
}
