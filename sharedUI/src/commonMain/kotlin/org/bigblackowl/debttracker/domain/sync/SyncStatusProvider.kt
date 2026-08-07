package org.bigblackowl.debttracker.domain.sync

import kotlinx.coroutines.flow.StateFlow
import org.bigblackowl.debttracker.domain.model.SyncUiStatus

/**
 * Абстракція над [org.bigblackowl.debttracker.data.sync.SyncCoordinator] (roomMain,
 * недоступний з commonMain) — щоб HomeScreen міг показати індикатор
 * синхронізації (спек §5) без залежності від Room-типів на Web.
 */
interface SyncStatusProvider {
    val status: StateFlow<SyncUiStatus>
}
