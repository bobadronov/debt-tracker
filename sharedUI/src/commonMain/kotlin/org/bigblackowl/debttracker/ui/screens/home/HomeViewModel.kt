package org.bigblackowl.debttracker.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import org.bigblackowl.debttracker.domain.repository.AuthRepository
import org.bigblackowl.debttracker.domain.sync.SyncStatusProvider

/** Combines auth state + sync status for [HomeScreen]'s top bar (спек §5) — sync badge only shows once signed in. */
class HomeViewModel(
    authRepository: AuthRepository,
    syncStatusProvider: SyncStatusProvider,
) : ViewModel() {

    val state: StateFlow<HomeState> = combine(
        authRepository.isAuthenticated, syncStatusProvider.status,
    ) { isAuthenticated, syncStatus ->
        HomeState(isAuthenticated, syncStatus)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        HomeState(authRepository.isAuthenticated.value, syncStatusProvider.status.value),
    )
}
