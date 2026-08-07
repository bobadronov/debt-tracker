package org.bigblackowl.debttracker.domain.model

/** Індикатор синхронізації (спек §5) — рендериться тільки для авторизованих користувачів. */
sealed interface SyncUiStatus {
    data object Synced : SyncUiStatus
    data object Syncing : SyncUiStatus
    data class OfflinePending(val count: Int) : SyncUiStatus
}
