package org.bigblackowl.debttracker.ui.screens.home

import org.bigblackowl.debttracker.domain.model.SyncUiStatus

data class HomeState(
    val isAuthenticated: Boolean,
    val syncStatus: SyncUiStatus,
    val unreadNotifications: Int = 0,
)
