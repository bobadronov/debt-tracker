package org.bigblackowl.debttracker.ui.screens.settings.data

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.bigblackowl.debttracker.domain.usecase.ClearAppCacheUseCase
import org.bigblackowl.debttracker.domain.usecase.DeleteAllDataUseCase

/** Owns [SettingsDataScreen]'s destructive data operations — cache clear and full local wipe. */
class SettingsDataViewModel(
    private val deleteAllData: DeleteAllDataUseCase,
    private val clearAppCache: ClearAppCacheUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsDataState())
    val state: StateFlow<SettingsDataState> = _state.asStateFlow()

    fun onIntent(intent: SettingsDataIntent) {
        when (intent) {
            SettingsDataIntent.DeleteAllData -> viewModelScope.launch {
                _state.update { it.copy(deleteError = false) }
                deleteAllData()
                    .onSuccess { _state.update { it.copy(deleteDone = true) } }
                    .onFailure { _state.update { it.copy(deleteError = true) } }
            }

            SettingsDataIntent.ClearAppCache -> viewModelScope.launch {
                _state.update { it.copy(cacheCleared = false, cacheClearError = false) }
                clearAppCache()
                    .onSuccess { _state.update { it.copy(cacheCleared = true) } }
                    .onFailure { _state.update { it.copy(cacheClearError = true) } }
            }
        }
    }
}
