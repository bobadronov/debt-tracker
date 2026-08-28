package org.bigblackowl.debttracker.ui.screens.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.bigblackowl.debttracker.core.notifications.NotificationsPoller
import org.bigblackowl.debttracker.domain.model.AppNotification
import org.bigblackowl.debttracker.domain.repository.NotificationRepository

/**
 * Опитування — не Flow-репозиторій (спек §7, [NotificationRepository] — суто suspend fetch-и), тож
 * екран освіжається вручну: одразу при відкритті й щоразу, коли [NotificationsPoller.unreadCount]
 * змінюється (тобто раз на 15с, поки триває фоновий polling) — без власного окремого таймера.
 */
class NotificationsViewModel(
    private val notificationRepository: NotificationRepository,
    notificationsPoller: NotificationsPoller,
) : ViewModel() {

    private val _state = MutableStateFlow(NotificationsState())
    val state: StateFlow<NotificationsState> = _state.asStateFlow()

    private val effectsChannel = Channel<NotificationsEffect>()
    val effects = effectsChannel.receiveAsFlow()

    init {
        viewModelScope.launch {
            notificationsPoller.unreadCount.collect { refresh() }
        }
    }

    fun onIntent(intent: NotificationsIntent) {
        when (intent) {
            is NotificationsIntent.Open -> viewModelScope.launch {
                if (!intent.notification.isRead) notificationRepository.markRead(intent.notification.id)
                refresh()
                intent.notification.relatedDebtorId?.let { effectsChannel.send(NotificationsEffect.NavigateToDebtor(it)) }
                intent.notification.relatedCreditorId?.let { effectsChannel.send(NotificationsEffect.NavigateToCreditor(it)) }
            }

            is NotificationsIntent.Delete -> viewModelScope.launch {
                notificationRepository.delete(intent.id)
                refresh()
            }

            NotificationsIntent.MarkAllRead -> viewModelScope.launch {
                notificationRepository.markAllRead()
                refresh()
            }
        }
    }

    private suspend fun refresh() {
        val notifications: List<AppNotification> = notificationRepository.fetchAll()
        _state.update { it.copy(isLoading = false, notifications = notifications) }
    }
}
