package org.bigblackowl.debttracker.ui.screens.debtors

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.bigblackowl.debttracker.core.i18n.resolveStrings
import org.bigblackowl.debttracker.core.settings.AppSettings
import org.bigblackowl.debttracker.core.sound.SoundEffect
import org.bigblackowl.debttracker.core.sound.SoundPlayer
import org.bigblackowl.debttracker.domain.model.DebtStatus
import org.bigblackowl.debttracker.domain.sync.SyncStatusProvider
import org.bigblackowl.debttracker.domain.usecase.debtor.DeleteDebtorUseCase
import org.bigblackowl.debttracker.domain.usecase.debtor.ObserveDebtorsUseCase

/** Combines the live debtor list with search/sort/filter state into [DebtorListState]. */
class DebtorListViewModel(
    observeDebtors: ObserveDebtorsUseCase,
    private val deleteDebtor: DeleteDebtorUseCase,
    private val syncStatusProvider: SyncStatusProvider,
    private val appSettings: AppSettings,
    private val soundPlayer: SoundPlayer,
) : ViewModel() {


    private val query = MutableStateFlow("")
    private val sortOrder = MutableStateFlow(DebtorSortOrder.NAME_ASC)
    private val statusFilter = MutableStateFlow(DebtorStatusFilter.ACTIVE)
    private val isRefreshing = MutableStateFlow(false)
    private val effectsChannel = Channel<DebtorListEffect>()
    val effects = effectsChannel.receiveAsFlow()

    val state: StateFlow<DebtorListState> = combine(
        observeDebtors(), query, sortOrder, statusFilter, isRefreshing,
    ) { debtors, currentQuery, currentSort, currentFilter, refreshing ->
        val byStatus = when (currentFilter) {
            DebtorStatusFilter.ALL -> debtors
            DebtorStatusFilter.ACTIVE -> debtors.filter { it.debtor.status == DebtStatus.ACTIVE }
            DebtorStatusFilter.CLOSED -> debtors.filter { it.debtor.status == DebtStatus.CLOSED }
        }
        val filtered = if (currentQuery.isBlank()) {
            byStatus
        } else {
            byStatus.filter { it.debtor.fullName.contains(currentQuery, ignoreCase = true) }
        }
        val sorted = when (currentSort) {
            DebtorSortOrder.NAME_ASC -> filtered.sortedBy { it.debtor.fullName.lowercase() }
            DebtorSortOrder.BALANCE_DESC -> filtered.sortedWith { a, b -> b.balance.compareTo(a.balance) }
            DebtorSortOrder.RECENT -> filtered.sortedByDescending { it.debtor.updatedAt }
        }
        DebtorListState(
            isLoading = false,
            isRefreshing = refreshing,
            query = currentQuery,
            sortOrder = currentSort,
            statusFilter = currentFilter,
            debtors = sorted,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DebtorListState())

    fun onIntent(intent: DebtorListIntent) {
        when (intent) {
            is DebtorListIntent.Search -> query.value = intent.query
            is DebtorListIntent.ChangeSort -> sortOrder.value = intent.order
            is DebtorListIntent.ChangeStatusFilter -> statusFilter.value = intent.filter
            is DebtorListIntent.Delete -> viewModelScope.launch {
                runCatching { deleteDebtor(intent.debtorId) }
                    .onSuccess { if (appSettings.soundEnabled) soundPlayer.play(SoundEffect.DELETE) }
                    .onFailure { effectsChannel.send(DebtorListEffect.Error(resolveStrings(appSettings.locale).deleteError)) }
            }
            DebtorListIntent.Refresh -> viewModelScope.launch {
                isRefreshing.value = true
                // The list is already Flow-driven and live — this just flushes pending writes to
                // the server sooner and gives the pull gesture a visible, deliberate result.
                runCatching { syncStatusProvider.refreshNow() }
                delay(300)
                isRefreshing.value = false
            }
        }
    }
}
