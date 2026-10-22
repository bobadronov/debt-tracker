package org.bigblackowl.debttracker.core.settings

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.bigblackowl.debttracker.domain.repository.HiddenContactsRepository

/** [HiddenContactsRepository] persisted in [AppSettings.hiddenContactKeys] (keys never contain a newline). */
class AppSettingsHiddenContacts(private val settings: AppSettings) : HiddenContactsRepository {

    private val _keys = MutableStateFlow(settings.hiddenContactKeys.lines().filter(String::isNotEmpty).toSet())
    override val keys: StateFlow<Set<String>> = _keys.asStateFlow()

    override fun hide(key: String) {
        _keys.update { it + key.replace('\n', ' ') }
        settings.hiddenContactKeys = _keys.value.joinToString("\n")
    }
}
