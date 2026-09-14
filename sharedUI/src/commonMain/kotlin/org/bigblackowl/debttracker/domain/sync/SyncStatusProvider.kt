package org.bigblackowl.debttracker.domain.sync

import kotlinx.coroutines.flow.StateFlow
import org.bigblackowl.debttracker.domain.model.SyncUiStatus

/**
 * Abstraction over [org.bigblackowl.debttracker.data.sync.SyncCoordinator] (roomMain,
 * unavailable from commonMain) — so HomeScreen can show a sync indicator
 * (spec §5) without depending on Room types on Web.
 */
interface SyncStatusProvider {
    val status: StateFlow<SyncUiStatus>

    /** Pull-to-refresh hook: pushes any PENDING local writes immediately instead of waiting for the next cycle. */
    suspend fun refreshNow()

    /**
     * One-shot re-download of every debtor/creditor/transaction row from Supabase into the local
     * cache — used by "Clear app cache" (Settings → Data) to repopulate immediately after wiping
     * Room, since the ongoing Realtime pull only re-emits on the *next* remote change, not on our
     * own local delete. A no-op where there's no local cache to repopulate (Web).
     */
    suspend fun refetchAll()
}
