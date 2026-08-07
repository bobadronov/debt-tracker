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
    version = 3,
)
@TypeConverters(Converters::class)
@ConstructedBy(DebtTrackerDatabaseConstructor::class)
abstract class DebtTrackerDatabase : RoomDatabase() {
    abstract fun debtorDao(): DebtorDao
    abstract fun debtTransactionDao(): DebtTransactionDao
    abstract fun creditorDao(): CreditorDao
    abstract fun creditorTransactionDao(): CreditorTransactionDao
}

/** v1 → v2: додано мультивалютність (спек — грн/долари/злоті/євро) — колонка `currency` на creditors/debtors, за замовчуванням UAH. */
val MIGRATION_1_2: Migration = object : Migration(1, 2) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE creditors ADD COLUMN currency TEXT NOT NULL DEFAULT 'UAH'")
        connection.execSQL("ALTER TABLE debtors ADD COLUMN currency TEXT NOT NULL DEFAULT 'UAH'")
    }
}

/** v2 → v3: колонка `email` на creditors/debtors — використовується для пошуку профілю зареєстрованого користувача (автозаповнення). */
val MIGRATION_2_3: Migration = object : Migration(2, 3) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE creditors ADD COLUMN email TEXT")
        connection.execSQL("ALTER TABLE debtors ADD COLUMN email TEXT")
    }
}

/** Room KSP генерує `actual`-реалізацію на кожній платформі (android/jvm/iosArm64/iosSimulatorArm64). */
@Suppress("KotlinNoActualForExpect")
expect object DebtTrackerDatabaseConstructor : RoomDatabaseConstructor<DebtTrackerDatabase> {
    override fun initialize(): DebtTrackerDatabase
}
