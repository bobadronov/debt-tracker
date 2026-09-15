package org.bigblackowl.debttracker.data.sync

import dev.jordond.connectivity.Connectivity
import io.github.aakira.napier.Napier
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.annotations.SupabaseExperimental
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.realtime.selectAsFlow
import kotlinx.coroutines.CancellationException
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
import org.bigblackowl.debttracker.core.settings.AppSettings
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
import kotlin.time.Duration.Companion.milliseconds

/**
 * Offline-first sync (spec §5): push — a loop that sends PENDING rows to Supabase once every
 * 30s; pull — a reactive Realtime subscription (selectAsFlow) on all 4 tables, filtered by
 * user_id. LWW on updatedAt applies to all 4 tables, transactions included — the same id can be
 * edited (UpdateDebtTransactionUseCase, approve_transaction_correction), so a conflict is entirely
 * possible.
 *
 * Simplification relative to the original spec (WorkManager on Android): a single coroutine
 * loop across all platforms instead of a platform-specific scheduler — functionally equivalent
 * for a personal app, simpler, and doesn't need a separate expect/actual layer. Easy to swap
 * for WorkManager later without touching domain/data.
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
    private val appSettings: AppSettings,
    private val connectivity: Connectivity,
) : SyncStatusProvider {
    private val _status = MutableStateFlow<SyncUiStatus>(SyncUiStatus.Synced)
    override val status: StateFlow<SyncUiStatus> = _status.asStateFlow()

    override suspend fun refreshNow() {
        if (authRepository.currentUserId != null) pushPending()
    }

    override suspend fun refetchAll() {
        val userId = authRepository.currentUserId ?: return
        coroutineScope {
            launch { mergeDebtors(client.from("debtors").select { filter { eq("user_id", userId) } }.decodeList<DebtorDto>()) }
            launch { mergeDebtTransactions(client.from("debt_transactions").select { filter { eq("user_id", userId) } }.decodeList<DebtTransactionDto>()) }
            launch { mergeCreditors(client.from("creditors").select { filter { eq("user_id", userId) } }.decodeList<CreditorDto>()) }
            launch { mergeCreditorTransactions(client.from("creditor_transactions").select { filter { eq("user_id", userId) } }.decodeList<CreditorTransactionDto>()) }
        }
    }

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
        // A prior DIFFERENT account's rows are still cached locally (e.g. the app was force-quit
        // or a session force-expired without going through the explicit "Sign out" flow, which is
        // the only other place that clears the cache) — wipe them before pulling this account's
        // data, so they don't render mixed into this list or get pushed under this account's
        // user_id by pushPending(). `null` (never synced before — pure local-only) is deliberately
        // left alone: that's the "sign in for the first time" path, and onboarding promises this
        // local-only data gets backed up into the account being signed into, not discarded.
        val lastSyncedUserId = appSettings.lastSyncedUserId
        if (lastSyncedUserId != null && lastSyncedUserId != userId) {
            val wiped = runCatching {
                debtTransactionDao.deleteAll()
                debtorDao.deleteAll()
                creditorTransactionDao.deleteAll()
                creditorDao.deleteAll()
            }
            if (wiped.isFailure) {
                // A partial wipe would let the previous account's leftover rows render mixed
                // into this account's data or get pushed under this account's user_id — safer
                // to abandon this sync session than push/pull against an inconsistent cache.
                // The next auth-state transition (e.g. app restart) retries from a clean slate.
                Napier.e(tag = "SyncCoordinator", throwable = wiped.exceptionOrNull()) {
                    "runSyncSession: failed to wipe previous account's cache, aborting sync session"
                }
                return
            }
        }
        appSettings.lastSyncedUserId = userId
        coroutineScope {
            launch { resilient { pushLoop() } }
            launch { resilient { pullDebtors(userId) } }
            launch { resilient { pullDebtTransactions(userId) } }
            launch { resilient { pullCreditors(userId) } }
            launch { resilient { pullCreditorTransactions(userId) } }
            launch { resilient { pushOnReconnect() } }
        }
    }

    /**
     * pushLoop() already covers the steady state (retries every 30s), but on a real network
     * drop that's up to 30s of a pending edit sitting unsynced after connectivity is already
     * back. This pushes immediately on the Disconnected -> Connected edge instead of waiting
     * for the next tick. The first emission after (re)subscribing is only a baseline — it must
     * not itself trigger a push, since pushLoop() already pushes once at session start.
     */
    private suspend fun pushOnReconnect() {
        var previous: Connectivity.Status? = null
        connectivity.statusUpdates.collect { current ->
            if (current is Connectivity.Status.Connected && previous is Connectivity.Status.Disconnected) {
                pushPending()
            }
            previous = current
        }
    }

    /**
     * The Realtime pull functions are meant to hang around forever (until cancelled), but
     * supabase-kt has a known race: if the websocket drops right between the channel-status
     * check and sending the LEAVE message in unsubscribe(), it throws
     * IllegalStateException("Websocket not yet initialized"). ApplicationScope is a
     * SupervisorJob with no CoroutineExceptionHandler, so without this wrapper any unhandled
     * error here (this race, a network drop, etc.) takes down the whole app.
     * Resubscribing via a new channel is the simplest way to recover.
     *
     * pushLoop() is wrapped the same way: getPending()/upsert() outside the per-row runCatching
     * in pushPending() (e.g. the Room query itself) must not fail unhandled for the same reason.
     */
    private suspend fun resilient(block: suspend () -> Unit) {
        while (currentCoroutineContext().isActive) {
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                delay(5_000.milliseconds)
            }
        }
    }

    private suspend fun pushLoop() {
        while (currentCoroutineContext().isActive) {
            pushPending()
            delay(30_000.milliseconds)
        }
    }

    private suspend fun pushPending() {
        val userId = authRepository.currentUserId ?: return

        val pendingDebtors = debtorDao.getPending()
        val pendingDebtTx = debtTransactionDao.getPending()
        val pendingCreditors = creditorDao.getPending()
        val pendingCreditorTx = creditorTransactionDao.getPending()
        val totalPending =
            pendingDebtors.size + pendingDebtTx.size + pendingCreditors.size + pendingCreditorTx.size
        if (totalPending == 0) {
            _status.value = SyncUiStatus.Synced
            return
        }
        _status.value = SyncUiStatus.Syncing

        var failures = 0
        pendingDebtors.forEach { entity ->
            runCatching {
                client.from("debtors").upsert(entity.toDto(userId))
                // These rows already exist locally (read from getPending()) — upsert() is
                // "INSERT OR REPLACE", which deletes-then-reinserts on a PK conflict and would
                // cascade-delete this debtor's transactions via their ON DELETE CASCADE FK.
                // Only clear PENDING if nothing edited this row since the snapshot above was
                // taken — otherwise we'd overwrite a newer local edit's data with the stale
                // snapshot and wrongly mark it SYNCED, even though that edit was never pushed.
                val current = debtorDao.getById(entity.id)
                if (current != null && current.updatedAt == entity.updatedAt) {
                    debtorDao.update(entity.copy(syncStatus = SyncStatus.SYNCED))
                }
            }.onFailure { failures++ }
        }
        pendingDebtTx.forEach { entity ->
            runCatching {
                client.from("debt_transactions").upsert(entity.toDto(userId))
                val current = debtTransactionDao.getById(entity.id)
                if (current != null && current.updatedAt == entity.updatedAt) {
                    debtTransactionDao.upsert(entity.copy(syncStatus = SyncStatus.SYNCED))
                }
            }.onFailure { failures++ }
        }
        pendingCreditors.forEach { entity ->
            runCatching {
                client.from("creditors").upsert(entity.toDto(userId))
                val current = creditorDao.getById(entity.id)
                if (current != null && current.updatedAt == entity.updatedAt) {
                    creditorDao.update(entity.copy(syncStatus = SyncStatus.SYNCED))
                }
            }.onFailure { failures++ }
        }
        pendingCreditorTx.forEach { entity ->
            runCatching {
                client.from("creditor_transactions").upsert(entity.toDto(userId))
                val current = creditorTransactionDao.getById(entity.id)
                if (current != null && current.updatedAt == entity.updatedAt) {
                    creditorTransactionDao.upsert(entity.copy(syncStatus = SyncStatus.SYNCED))
                }
            }.onFailure { failures++ }
        }

        _status.value =
            if (failures > 0) SyncUiStatus.OfflinePending(failures) else SyncUiStatus.Synced
    }

    private suspend fun pullDebtors(userId: String) {
        client.from("debtors")
            .selectAsFlow(
                DebtorDto::id,
                filter = { eq("user_id", userId) }
            )
            .collectLatest { mergeDebtors(it) }
    }

    private suspend fun pullDebtTransactions(userId: String) {
        client.from("debt_transactions")
            .selectAsFlow(
                DebtTransactionDto::id,
                filter = { eq("user_id", userId) }
            )
            .collectLatest { mergeDebtTransactions(it) }
    }

    private suspend fun pullCreditors(userId: String) {
        client.from("creditors")
            .selectAsFlow(
                CreditorDto::id,
                filter = { eq("user_id", userId) }
            )
            .collectLatest { mergeCreditors(it) }
    }

    private suspend fun pullCreditorTransactions(userId: String) {
        client.from("creditor_transactions")
            .selectAsFlow(
                CreditorTransactionDto::id,
                filter = { eq("user_id", userId) }
            )
            .collectLatest { mergeCreditorTransactions(it) }
    }

    // Shared by the continuous Realtime pulls above and refetchAll()'s one-shot re-download.
    private suspend fun mergeDebtors(remoteRows: List<DebtorDto>) {
        remoteRows.forEach { dto ->
            val local = debtorDao.getById(dto.id)
            val remoteUpdatedAt = kotlin.time.Instant.parse(dto.updatedAt)
            if (local == null) {
                debtorDao.upsert(dto.toEntity())
            } else if (local.syncStatus != SyncStatus.PENDING || local.updatedAt < remoteUpdatedAt) {
                // A plain upsert() here would cascade-delete this debtor's transactions
                // (see the note in RoomDebtorRepository) since the row already exists.
                // On an exact tie, keep the not-yet-pushed local edit rather than discard it.
                debtorDao.update(dto.toEntity())
            }
        }
    }

    private suspend fun mergeDebtTransactions(remoteRows: List<DebtTransactionDto>) {
        // Transactions CAN be merged (UpdateDebtTransactionUseCase / approve_transaction_correction
        // edit the same id), so the same LWW guard as Debtor/Creditor is needed here —
        // otherwise an unconditional upsert would overwrite a not-yet-pushed local edit with a stale server version.
        remoteRows.forEach { dto ->
            val local = debtTransactionDao.getById(dto.id)
            val remoteUpdatedAt = kotlin.time.Instant.parse(dto.updatedAt)
            if (local == null || local.syncStatus != SyncStatus.PENDING || local.updatedAt < remoteUpdatedAt) {
                debtTransactionDao.upsert(dto.toEntity())
            }
        }
    }

    private suspend fun mergeCreditors(remoteRows: List<CreditorDto>) {
        remoteRows.forEach { dto ->
            val local = creditorDao.getById(dto.id)
            val remoteUpdatedAt = kotlin.time.Instant.parse(dto.updatedAt)
            if (local == null) {
                creditorDao.upsert(dto.toEntity())
            } else if (local.syncStatus != SyncStatus.PENDING || local.updatedAt < remoteUpdatedAt) {
                creditorDao.update(dto.toEntity())
            }
        }
    }

    private suspend fun mergeCreditorTransactions(remoteRows: List<CreditorTransactionDto>) {
        remoteRows.forEach { dto ->
            val local = creditorTransactionDao.getById(dto.id)
            val remoteUpdatedAt = kotlin.time.Instant.parse(dto.updatedAt)
            if (local == null || local.syncStatus != SyncStatus.PENDING || local.updatedAt < remoteUpdatedAt) {
                creditorTransactionDao.upsert(dto.toEntity())
            }
        }
    }
}
