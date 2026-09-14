package org.bigblackowl.debttracker.domain.model

/** Sync indicator (spec §5) — rendered only for authenticated users. */
sealed interface SyncUiStatus {
    data object Synced : SyncUiStatus
    data object Syncing : SyncUiStatus
    data class OfflinePending(val count: Int) : SyncUiStatus
}
