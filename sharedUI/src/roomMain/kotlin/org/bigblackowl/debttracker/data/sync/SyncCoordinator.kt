package org.bigblackowl.debttracker.data.sync

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.annotations.SupabaseExperimental
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.filter.FilterOperation
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import io.github.jan.supabase.realtime.selectAsFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import org.bigblackowl.debttracker.data.local.dao.CreditorDao
import org.bigblackowl.debttracker.data.local.dao.CreditorTransactionDao
import org.bigblackowl.debttracker.data.local.dao.DebtTransactionDao
import org.bigblackowl.debttracker.data.local.dao.DebtorDao
import org.bigblackowl.debttracker.data.remote.dto.CreditorDto
import org.bigblackowl.debttracker.data.remote.dto.CreditorTransactionDto
import org.bigblackowl.debttracker.data.remote.dto.DebtTransactionDto
import org.bigblackowl.debttracker.data.remote.dto.DebtorDto
import org.bigblackowl.debttracker.domain.model.SyncStatus
import org.bigblackowl.debttracker.domain.model.SyncUiStatus
import org.bigblackowl.debttracker.domain.repository.AuthRepository
import org.bigblackowl.debttracker.domain.sync.SyncStatusProvider

/**
 * Offline-first sync (спек §5): push — цикл раз на 30с відправляє PENDING-рядки
 * у Supabase; pull — реактивна Realtime-підписка (selectAsFlow) на всі 4 таблиці,
 * відфільтрована по user_id. LWW за updatedAt застосовується тільки до
 * Debtor/Creditor-метаданих; транзакції не мерджаться (просто upsert по id),
 * оскільки конфлікт на рівні одного запису з різним id неможливий.
 *
 * Спрощення відносно оригінального опису (WorkManager на Android): єдиний
 * coroutine-цикл на всіх платформах замість платформо-специфічного планувальника
 * — функціонально еквівалентно для персонального застосунку, простіше й не
 * потребує окремого expect/actual шару. Легко замінити на WorkManager пізніше,
 * не чіпаючи домен/дані.
 */
@OptIn(SupabaseExperimental::class)
class SyncCoordinator(
    private val client: SupabaseClient,
    private val authRepository: AuthRepository,
    private val debtorDao: DebtorDao,
    private val debtTransactionDao: DebtTransactionDao,
    private val creditorDao: CreditorDao,
    private val creditorTransactionDao: CreditorTransactionDao,
    private val scope: CoroutineScope,
) : SyncStatusProvider {
    private val _status = MutableStateFlow<SyncUiStatus>(SyncUiStatus.Synced)
    override val status: StateFlow<SyncUiStatus> = _status.asStateFlow()

    fun start() {
        scope.launch {
            authRepository.isAuthenticated.collectLatest { authenticated ->
                if (authenticated) {
                    runSyncSession()
                } else {
                    _status.value = SyncUiStatus.Synced
                }
            }
        }
    }

    private suspend fun runSyncSession() {
        val userId = authRepository.currentUserId ?: return
        coroutineScope {
            launch { pushLoop() }
            launch { pullDebtors(userId) }
            launch { pullDebtTransactions(userId) }
            launch { pullCreditors(userId) }
            launch { pullCreditorTransactions(userId) }
        }
    }

    private suspend fun pushLoop() {
        while (currentCoroutineContext().isActive) {
            pushPending()
            delay(30_000)
        }
    }

    private suspend fun pushPending() {
        val userId = authRepository.currentUserId ?: return

        val pendingDebtors = debtorDao.getPending()
        val pendingDebtTx = debtTransactionDao.getPending()
        val pendingCreditors = creditorDao.getPending()
        val pendingCreditorTx = creditorTransactionDao.getPending()
        val totalPending = pendingDebtors.size + pendingDebtTx.size + pendingCreditors.size + pendingCreditorTx.size
        if (totalPending == 0) {
            _status.value = SyncUiStatus.Synced
            return
        }
        _status.value = SyncUiStatus.Syncing

        var failures = 0
        pendingDebtors.forEach { entity ->
            runCatching {
                client.from("debtors").upsert(entity.toDto(userId))
                debtorDao.upsert(entity.copy(syncStatus = SyncStatus.SYNCED))
            }.onFailure { failures++ }
        }
        pendingDebtTx.forEach { entity ->
            runCatching {
                client.from("debt_transactions").upsert(entity.toDto(userId))
                debtTransactionDao.upsert(entity.copy(syncStatus = SyncStatus.SYNCED))
            }.onFailure { failures++ }
        }
        pendingCreditors.forEach { entity ->
            runCatching {
                client.from("creditors").upsert(entity.toDto(userId))
                creditorDao.upsert(entity.copy(syncStatus = SyncStatus.SYNCED))
            }.onFailure { failures++ }
        }
        pendingCreditorTx.forEach { entity ->
            runCatching {
                client.from("creditor_transactions").upsert(entity.toDto(userId))
                creditorTransactionDao.upsert(entity.copy(syncStatus = SyncStatus.SYNCED))
            }.onFailure { failures++ }
        }

        _status.value = if (failures > 0) SyncUiStatus.OfflinePending(failures) else SyncUiStatus.Synced
    }

    private suspend fun pullDebtors(userId: String) {
        client.from("debtors")
            .selectAsFlow(DebtorDto::id, filter = FilterOperation("user_id", FilterOperator.EQ, userId))
            .collectLatest { remoteRows ->
                remoteRows.forEach { dto ->
                    val local = debtorDao.getById(dto.id)
                    val remoteUpdatedAt = Instant.parse(dto.updatedAt)
                    if (local == null || local.syncStatus != SyncStatus.PENDING || local.updatedAt <= remoteUpdatedAt) {
                        debtorDao.upsert(dto.toEntity())
                    }
                }
            }
    }

    private suspend fun pullDebtTransactions(userId: String) {
        client.from("debt_transactions")
            .selectAsFlow(DebtTransactionDto::id, filter = FilterOperation("user_id", FilterOperator.EQ, userId))
            .collectLatest { remoteRows ->
                // Транзакції не мерджаться (спек §5) — прямий upsert по id.
                remoteRows.forEach { dto -> debtTransactionDao.upsert(dto.toEntity()) }
            }
    }

    private suspend fun pullCreditors(userId: String) {
        client.from("creditors")
            .selectAsFlow(CreditorDto::id, filter = FilterOperation("user_id", FilterOperator.EQ, userId))
            .collectLatest { remoteRows ->
                remoteRows.forEach { dto ->
                    val local = creditorDao.getById(dto.id)
                    val remoteUpdatedAt = Instant.parse(dto.updatedAt)
                    if (local == null || local.syncStatus != SyncStatus.PENDING || local.updatedAt <= remoteUpdatedAt) {
                        creditorDao.upsert(dto.toEntity())
                    }
                }
            }
    }

    private suspend fun pullCreditorTransactions(userId: String) {
        client.from("creditor_transactions")
            .selectAsFlow(CreditorTransactionDto::id, filter = FilterOperation("user_id", FilterOperator.EQ, userId))
            .collectLatest { remoteRows ->
                remoteRows.forEach { dto -> creditorTransactionDao.upsert(dto.toEntity()) }
            }
    }
}
