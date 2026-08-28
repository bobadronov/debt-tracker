package org.bigblackowl.debttracker.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import org.bigblackowl.debttracker.core.notifications.NotificationsPoller
import org.bigblackowl.debttracker.domain.repository.AuthRepository
import org.bigblackowl.debttracker.domain.sync.SyncStatusProvider

/** Combines auth state + sync status + unread notifications for [HomeScreen]'s top bar (спек §5, §7) — sync badge/bell only show once signed in. */
class HomeViewModel(
    authRepository: AuthRepository,
    syncStatusProvider: SyncStatusProvider,
    notificationsPoller: NotificationsPoller,
) : ViewModel() {

    val state: StateFlow<HomeState> = combine(
        authRepository.isAuthenticated, syncStatusProvider.status, notificationsPoller.unreadCount,
    ) { isAuthenticated, syncStatus, unreadNotifications ->
        HomeState(isAuthenticated, syncStatus, unreadNotifications)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        HomeState(authRepository.isAuthenticated.value, syncStatusProvider.status.value, notificationsPoller.unreadCount.value),
    )
}
