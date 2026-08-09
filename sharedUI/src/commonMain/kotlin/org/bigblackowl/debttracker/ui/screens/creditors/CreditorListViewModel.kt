package org.bigblackowl.debttracker.ui.screens.creditors

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
import org.bigblackowl.debttracker.domain.usecase.creditor.DeleteCreditorUseCase
import org.bigblackowl.debttracker.domain.usecase.creditor.ObserveCreditorsUseCase

/** Combines the live creditor list with search/sort/filter state into [CreditorListState]. */
class CreditorListViewModel(
    observeCreditors: ObserveCreditorsUseCase,
    private val deleteCreditor: DeleteCreditorUseCase,
    private val syncStatusProvider: SyncStatusProvider,
    private val appSettings: AppSettings,
    private val soundPlayer: SoundPlayer,
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val sortOrder = MutableStateFlow(CreditorSortOrder.NAME_ASC)
    private val statusFilter = MutableStateFlow(CreditorStatusFilter.ACTIVE)
    private val isRefreshing = MutableStateFlow(false)
    private val effectsChannel = Channel<CreditorListEffect>()
    val effects = effectsChannel.receiveAsFlow()

    val state: StateFlow<CreditorListState> = combine(
        observeCreditors(), query, sortOrder, statusFilter, isRefreshing,
    ) { creditors, currentQuery, currentSort, currentFilter, refreshing ->
        val byStatus = when (currentFilter) {
            CreditorStatusFilter.ALL -> creditors
            CreditorStatusFilter.ACTIVE -> creditors.filter { it.creditor.status == DebtStatus.ACTIVE }
            CreditorStatusFilter.CLOSED -> creditors.filter { it.creditor.status == DebtStatus.CLOSED }
        }
        val filtered = if (currentQuery.isBlank()) {
            byStatus
        } else {
            byStatus.filter { it.creditor.fullName.contains(currentQuery, ignoreCase = true) }
        }
        val sorted = when (currentSort) {
            CreditorSortOrder.NAME_ASC -> filtered.sortedBy { it.creditor.fullName.lowercase() }
            CreditorSortOrder.BALANCE_DESC -> filtered.sortedWith { a, b -> b.balance.compareTo(a.balance) }
            CreditorSortOrder.RECENT -> filtered.sortedByDescending { it.creditor.updatedAt }
        }
        CreditorListState(
            isLoading = false,
            isRefreshing = refreshing,
            query = currentQuery,
            sortOrder = currentSort,
            statusFilter = currentFilter,
            creditors = sorted,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CreditorListState())

    fun onIntent(intent: CreditorListIntent) {
        when (intent) {
            is CreditorListIntent.Search -> query.value = intent.query
            is CreditorListIntent.ChangeSort -> sortOrder.value = intent.order
            is CreditorListIntent.ChangeStatusFilter -> statusFilter.value = intent.filter
            is CreditorListIntent.Delete -> viewModelScope.launch {
                runCatching { deleteCreditor(intent.creditorId) }
                    .onSuccess { if (appSettings.soundEnabled) soundPlayer.play(SoundEffect.DELETE) }
                    .onFailure { effectsChannel.send(CreditorListEffect.Error(resolveStrings(appSettings.locale).deleteError)) }
            }
            CreditorListIntent.Refresh -> viewModelScope.launch {
                isRefreshing.value = true
                runCatching { syncStatusProvider.refreshNow() }
                delay(300)
                isRefreshing.value = false
            }
        }
    }
}
