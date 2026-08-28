package org.bigblackowl.debttracker.preview

import com.ionspin.kotlin.bignum.decimal.BigDecimal
import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import org.bigblackowl.debttracker.core.notifications.LocalNotifier
import org.bigblackowl.debttracker.core.sound.SoundEffect
import org.bigblackowl.debttracker.core.sound.SoundPlayer
import org.bigblackowl.debttracker.domain.model.AppNotification
import org.bigblackowl.debttracker.domain.model.Creditor
import org.bigblackowl.debttracker.domain.model.CreditorTransaction
import org.bigblackowl.debttracker.domain.model.CreditorWithBalance
import org.bigblackowl.debttracker.domain.model.Currency
import org.bigblackowl.debttracker.domain.model.DebtStatus
import org.bigblackowl.debttracker.domain.model.DebtTransaction
import org.bigblackowl.debttracker.domain.model.Debtor
import org.bigblackowl.debttracker.domain.model.DebtorWithBalance
import org.bigblackowl.debttracker.domain.model.MyDebtTransactionType
import org.bigblackowl.debttracker.domain.model.PaymentMethod
import org.bigblackowl.debttracker.domain.model.DeviceSession
import org.bigblackowl.debttracker.domain.model.ProfileSuggestion
import org.bigblackowl.debttracker.domain.model.SyncStatus
import org.bigblackowl.debttracker.domain.model.SyncUiStatus
import org.bigblackowl.debttracker.domain.model.TransactionType
import org.bigblackowl.debttracker.domain.model.creditorBalance
import org.bigblackowl.debttracker.domain.model.debtorBalance
import org.bigblackowl.debttracker.domain.repository.AuthRepository
import org.bigblackowl.debttracker.domain.repository.CreditorRepository
import org.bigblackowl.debttracker.domain.repository.DebtorRepository
import org.bigblackowl.debttracker.domain.repository.NotificationRepository
import org.bigblackowl.debttracker.domain.repository.ProfileLookupRepository
import org.bigblackowl.debttracker.domain.repository.SessionRepository
import org.bigblackowl.debttracker.domain.sync.SyncStatusProvider
import org.bigblackowl.debttracker.core.platform.AppPlatform
import kotlinx.coroutines.flow.emptyFlow
import kotlin.time.Clock

/**
 * ID-и, на які посилаються @Preview-функції екранів деталей/редагування —
 * мають існувати в [FakeDebtorRepository]/[FakeCreditorRepository] нижче,
 * інакше Preview покаже порожній стан.
 */
object PreviewIds {
    const val DEBTOR = "preview-debtor-1"
    const val CREDITOR = "preview-creditor-1"
}

private val previewNow = Clock.System.now()

private fun previewDebtors() = List(15) { i ->
    Debtor(
        id = if (i == 0) PreviewIds.DEBTOR else "preview-debtor-repeat-$i",
        fullName = "Олена Ковальчук $i",
        phone = if (i % 3 == 0) null else "+380501234567",
        email = null,
        avatarUrl = null,
        comment = if (i % 4 == 0) null else "Позика на ремонт",
        createdAt = previewNow,
        updatedAt = previewNow,
        status = if (i % 5 == 0) DebtStatus.CLOSED else DebtStatus.ACTIVE,
        syncStatus = SyncStatus.entries[i % SyncStatus.entries.size],
        currency = Currency.entries[i % Currency.entries.size],
    )
} + Debtor(
    id = "preview-debtor-2",
    fullName = "Ігор Бондаренко",
    phone = null,
    email = null,
    avatarUrl = null,
    comment = null,
    createdAt = previewNow,
    updatedAt = previewNow,
    status = DebtStatus.ACTIVE,
    syncStatus = SyncStatus.PENDING,
    currency = Currency.USD,
)

private fun previewDebtorTransactions() = listOf(
    DebtTransaction(
        id = "preview-debtor-tx-1",
        debtorId = PreviewIds.DEBTOR,
        amount = BigDecimal.parseString("-1500"),
        type = TransactionType.LEND,
        method = PaymentMethod.CASH,
        date = previewNow,
        comment = "Позика на ремонт",
        createdAt = previewNow,
        updatedAt = previewNow,
        syncStatus = SyncStatus.SYNCED,
    ),
    DebtTransaction(
        id = "preview-debtor-tx-2",
        debtorId = PreviewIds.DEBTOR,
        amount = BigDecimal.parseString("500"),
        type = TransactionType.REPAY,
        method = PaymentMethod.CARD,
        date = previewNow,
        comment = "Часткове повернення",
        createdAt = previewNow,
        updatedAt = previewNow,
        syncStatus = SyncStatus.SYNCED,
    ),
    DebtTransaction(
        id = "preview-debtor-tx-3",
        debtorId = PreviewIds.DEBTOR,
        amount = BigDecimal.parseString("-2000"),
        type = TransactionType.LEND,
        method = PaymentMethod.CASH,
        date = previewNow,
        comment = "Додаткова позика",
        createdAt = previewNow,
        updatedAt = previewNow,
        syncStatus = SyncStatus.SYNCED,
    ),
    DebtTransaction(
        id = "preview-debtor-tx-4",
        debtorId = "preview-debtor-2",
        amount = BigDecimal.parseString("-800"),
        type = TransactionType.LEND,
        method = PaymentMethod.CASH,
        date = previewNow,
        comment = null,
        createdAt = previewNow,
        updatedAt = previewNow,
        syncStatus = SyncStatus.SYNCED,
    ),
    DebtTransaction(
        id = "preview-debtor-tx-5",
        debtorId = "preview-debtor-2",
        amount = BigDecimal.parseString("300"),
        type = TransactionType.REPAY,
        method = PaymentMethod.CARD,
        date = previewNow,
        comment = "Часткове повернення",
        createdAt = previewNow,
        updatedAt = previewNow,
        syncStatus = SyncStatus.PENDING,
    ),
    DebtTransaction(
        id = "preview-debtor-tx-6",
        debtorId = "preview-debtor-repeat-1",
        amount = BigDecimal.parseString("-1200"),
        type = TransactionType.LEND,
        method = PaymentMethod.CARD,
        date = previewNow,
        comment = "Позика на навчання",
        createdAt = previewNow,
        updatedAt = previewNow,
        syncStatus = SyncStatus.SYNCED,
    ),
)

private fun previewCreditors() = List(15) { i ->
    Creditor(
        id = if (i == 0) PreviewIds.CREDITOR else "preview-creditor-repeat-$i",
        fullName = "Марія Шевченко $i",
        phone = "+380671112233",
        email = null,
        avatarUrl = null,
        comment = "Позика на авто",
        createdAt = previewNow,
        updatedAt = previewNow,
        status = DebtStatus.ACTIVE,
        syncStatus = SyncStatus.SYNCED,
        currency = Currency.EUR,
    )
} + Creditor(
    id = "preview-creditor-2",
    fullName = "Андрій Мельник",
    phone = null,
    email = null,
    avatarUrl = null,
    comment = null,
    createdAt = previewNow,
    updatedAt = previewNow,
    status = DebtStatus.CLOSED,
    syncStatus = SyncStatus.SYNCED,
    currency = Currency.PLN,
)

private fun previewCreditorTransactions() = listOf(
    CreditorTransaction(
        id = "preview-creditor-tx-1",
        creditorId = PreviewIds.CREDITOR,
        amount = BigDecimal.parseString("-3000"),
        type = MyDebtTransactionType.BORROW,
        method = PaymentMethod.CARD,
        date = previewNow,
        comment = "Позика на авто",
        createdAt = previewNow,
        updatedAt = previewNow,
        syncStatus = SyncStatus.SYNCED,
    ),
    CreditorTransaction(
        id = "preview-creditor-tx-2",
        creditorId = PreviewIds.CREDITOR,
        amount = BigDecimal.parseString("1000"),
        type = MyDebtTransactionType.RETURN,
        method = PaymentMethod.CASH,
        date = previewNow,
        comment = "Часткове повернення",
        createdAt = previewNow,
        updatedAt = previewNow,
        syncStatus = SyncStatus.SYNCED,
    ),
    CreditorTransaction(
        id = "preview-creditor-tx-3",
        creditorId = PreviewIds.CREDITOR,
        amount = BigDecimal.parseString("-500"),
        type = MyDebtTransactionType.BORROW,
        method = PaymentMethod.CASH,
        date = previewNow,
        comment = "Додатково позичив",
        createdAt = previewNow,
        updatedAt = previewNow,
        syncStatus = SyncStatus.CONFLICT,
    ),
    CreditorTransaction(
        id = "preview-creditor-tx-4",
        creditorId = "preview-creditor-2",
        amount = BigDecimal.parseString("-2200"),
        type = MyDebtTransactionType.BORROW,
        method = PaymentMethod.CARD,
        date = previewNow,
        comment = "Позика на ремонт квартири",
        createdAt = previewNow,
        updatedAt = previewNow,
        syncStatus = SyncStatus.SYNCED,
    ),
    CreditorTransaction(
        id = "preview-creditor-tx-5",
        creditorId = "preview-creditor-2",
        amount = BigDecimal.parseString("2200"),
        type = MyDebtTransactionType.RETURN,
        method = PaymentMethod.CASH,
        date = previewNow,
        comment = "Повернув повністю",
        createdAt = previewNow,
        updatedAt = previewNow,
        syncStatus = SyncStatus.SYNCED,
    ),
    CreditorTransaction(
        id = "preview-creditor-tx-6",
        creditorId = "preview-creditor-repeat-1",
        amount = BigDecimal.parseString("-750"),
        type = MyDebtTransactionType.BORROW,
        method = PaymentMethod.CARD,
        date = previewNow,
        comment = "Позика на техніку",
        createdAt = previewNow,
        updatedAt = previewNow,
        syncStatus = SyncStatus.PENDING,
    ),
)

/** In-memory реалізація [DebtorRepository] для @Preview — без Room/Supabase. */
class FakeDebtorRepository(
    seedDebtors: List<Debtor> = previewDebtors(),
    seedTransactions: List<DebtTransaction> = previewDebtorTransactions(),
) : DebtorRepository {
    private val debtors = MutableStateFlow(seedDebtors)
    private val transactions = MutableStateFlow(seedTransactions)

    override fun observeDebtors(): Flow<List<DebtorWithBalance>> =
        combine(debtors, transactions) { ds, txs ->
            ds.filterNot { it.isDeleted }.map { d ->
                DebtorWithBalance(d, txs.filter { it.debtorId == d.id }.debtorBalance())
            }
        }

    override fun observeDebtor(id: String): Flow<Debtor?> =
        debtors.map { list -> list.find { it.id == id } }

    override fun observeTransactions(debtorId: String): Flow<List<DebtTransaction>> =
        transactions.map { list -> list.filter { it.debtorId == debtorId } }

    override suspend fun getDebtor(id: String): Debtor? = debtors.value.find { it.id == id }

    override suspend fun upsertDebtor(debtor: Debtor) {
        debtors.update { list ->
            if (list.any { it.id == debtor.id }) list.map { if (it.id == debtor.id) debtor else it }
            else list + debtor
        }
    }

    override suspend fun softDeleteDebtor(id: String) {
        debtors.update { list -> list.map { if (it.id == id) it.copy(isDeleted = true) else it } }
    }

    override suspend fun addTransaction(transaction: DebtTransaction) {
        transactions.update { it + transaction }
    }

    override suspend fun deleteAllData() {
        debtors.update { emptyList() }
        transactions.update { emptyList() }
    }

    override suspend fun clearLocalCache() {
        debtors.update { emptyList() }
        transactions.update { emptyList() }
    }

    override suspend fun linkToRegisteredUser(debtorId: String): String? = null
}

/** In-memory реалізація [CreditorRepository] для @Preview — без Room/Supabase. */
class FakeCreditorRepository(
    seedCreditors: List<Creditor> = previewCreditors(),
    seedTransactions: List<CreditorTransaction> = previewCreditorTransactions(),
) : CreditorRepository {
    private val creditors = MutableStateFlow(seedCreditors)
    private val transactions = MutableStateFlow(seedTransactions)

    override fun observeCreditors(): Flow<List<CreditorWithBalance>> =
        combine(creditors, transactions) { cs, txs ->
            cs.filterNot { it.isDeleted }.map { c ->
                CreditorWithBalance(c, txs.filter { it.creditorId == c.id }.creditorBalance())
            }
        }

    override fun observeCreditor(id: String): Flow<Creditor?> =
        creditors.map { list -> list.find { it.id == id } }

    override fun observeTransactions(creditorId: String): Flow<List<CreditorTransaction>> =
        transactions.map { list -> list.filter { it.creditorId == creditorId } }

    override suspend fun getCreditor(id: String): Creditor? = creditors.value.find { it.id == id }

    override suspend fun upsertCreditor(creditor: Creditor) {
        creditors.update { list ->
            if (list.any { it.id == creditor.id }) list.map { if (it.id == creditor.id) creditor else it }
            else list + creditor
        }
    }

    override suspend fun softDeleteCreditor(id: String) {
        creditors.update { list -> list.map { if (it.id == id) it.copy(isDeleted = true) else it } }
    }

    override suspend fun addTransaction(transaction: CreditorTransaction) {
        transactions.update { it + transaction }
    }

    override suspend fun deleteAllData() {
        creditors.update { emptyList() }
        transactions.update { emptyList() }
    }

    override suspend fun clearLocalCache() {
        creditors.update { emptyList() }
        transactions.update { emptyList() }
    }

    override suspend fun linkToRegisteredUser(creditorId: String): String? = null
}

/** Local-only режим (не автентифікований) — без мережевих викликів до Supabase. */
class FakeAuthRepository : AuthRepository {
    override val isAuthenticated: StateFlow<Boolean> = MutableStateFlow(false)
    override val currentUserId: String? = null
    override val email: StateFlow<String?> = MutableStateFlow(null)
    override val displayName: StateFlow<String?> = MutableStateFlow(null)
    override val phone: StateFlow<String?> = MutableStateFlow(null)
    override val avatarUrl: StateFlow<String?> = MutableStateFlow(null)

    override suspend fun signUp(email: String, password: String): Result<Unit> = Result.success(Unit)
    override suspend fun signIn(email: String, password: String): Result<Unit> = Result.success(Unit)
    override suspend fun signOut() = Unit
    override suspend fun updateAvatar(bytes: ByteArray, fileExtension: String): Result<String> = Result.success("")
    override suspend fun updateProfile(displayName: String, phone: String?): Result<Unit> = Result.success(Unit)
}

/** Static two-device list for @Preview — revoke calls are no-ops since nothing observes them back. */
class FakeSessionRepository : SessionRepository {
    private val sessions = MutableStateFlow(
        listOf(
            DeviceSession(
                id = "preview-session-1",
                deviceName = "Pixel 8",
                platform = AppPlatform.ANDROID,
                lastSeenAt = previewNow,
                isCurrentDevice = true,
            ),
            DeviceSession(
                id = "preview-session-2",
                deviceName = "MacBook Pro",
                platform = AppPlatform.DESKTOP,
                lastSeenAt = previewNow,
                isCurrentDevice = false,
            ),
        )
    )

    override fun observeSessions(): Flow<List<DeviceSession>> = sessions
    override suspend fun revokeSession(sessionId: String): Result<Unit> = Result.success(Unit)
    override suspend fun revokeAllOtherSessions(): Result<Unit> = Result.success(Unit)
    override val revokedElsewhere: Flow<Unit> = emptyFlow()
}

class FakeSyncStatusProvider : SyncStatusProvider {
    override val status: StateFlow<SyncUiStatus> = MutableStateFlow(SyncUiStatus.Synced)
    override suspend fun refreshNow() {}
}

/** Ніколи не знаходить збіг — @Preview не робить мережевих викликів. */
class FakeProfileLookupRepository : ProfileLookupRepository {
    override suspend fun findProfileByEmail(email: String): ProfileSuggestion? = null
}

/** Порожня історія — @Preview не робить мережевих викликів до `notifications`. */
class FakeNotificationRepository : NotificationRepository {
    override suspend fun fetchSince(after: kotlin.time.Instant?): List<AppNotification> = emptyList()
    override suspend fun fetchAll(): List<AppNotification> = emptyList()
    override suspend fun unreadCount(): Int = 0
    override suspend fun markRead(id: String) {}
    override suspend fun markAllRead() {}
    override suspend fun delete(id: String) {}
}

/**
 * No-op [LocalNotifier] для @Preview — реальні реалізації чіпають платформні API сповіщень,
 * яких немає в пісочниці Android Studio Layoutlib.
 */
class NoOpLocalNotifier : LocalNotifier {
    override suspend fun requestPermission(): Boolean = true
    override fun notify(title: String, body: String, deepLink: String?) = Unit
}

/**
 * No-op [SoundPlayer] для @Preview — реальні реалізації (напр. Android [android.media.SoundPool])
 * чіпають платформні media API, яких немає в пісочниці Android Studio Layoutlib.
 */
class NoOpSoundPlayer : SoundPlayer {
    override fun play(sound: SoundEffect) = Unit
}

/** [Settings] в оперативній пам'яті — щоб @Preview не чіпав реальний платформний сховище налаштувань. */
class InMemorySettings : Settings {
    private val values = mutableMapOf<String, Any>()

    override val keys: Set<String> get() = values.keys
    override val size: Int get() = values.size
    override fun clear() = values.clear()
    override fun remove(key: String) { values.remove(key) }
    override fun hasKey(key: String): Boolean = values.containsKey(key)

    override fun putInt(key: String, value: Int) { values[key] = value }
    override fun getInt(key: String, defaultValue: Int): Int = values[key] as? Int ?: defaultValue
    override fun getIntOrNull(key: String): Int? = values[key] as? Int

    override fun putLong(key: String, value: Long) { values[key] = value }
    override fun getLong(key: String, defaultValue: Long): Long = values[key] as? Long ?: defaultValue
    override fun getLongOrNull(key: String): Long? = values[key] as? Long

    override fun putString(key: String, value: String) { values[key] = value }
    override fun getString(key: String, defaultValue: String): String = values[key] as? String ?: defaultValue
    override fun getStringOrNull(key: String): String? = values[key] as? String

    override fun putFloat(key: String, value: Float) { values[key] = value }
    override fun getFloat(key: String, defaultValue: Float): Float = values[key] as? Float ?: defaultValue
    override fun getFloatOrNull(key: String): Float? = values[key] as? Float

    override fun putDouble(key: String, value: Double) { values[key] = value }
    override fun getDouble(key: String, defaultValue: Double): Double = values[key] as? Double ?: defaultValue
    override fun getDoubleOrNull(key: String): Double? = values[key] as? Double

    override fun putBoolean(key: String, value: Boolean) { values[key] = value }
    override fun getBoolean(key: String, defaultValue: Boolean): Boolean = values[key] as? Boolean ?: defaultValue
    override fun getBooleanOrNull(key: String): Boolean? = values[key] as? Boolean
}
