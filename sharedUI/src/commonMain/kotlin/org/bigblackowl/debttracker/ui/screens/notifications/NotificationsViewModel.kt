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
import org.bigblackowl.debttracker.core.i18n.resolveStrings
import org.bigblackowl.debttracker.core.notifications.NotificationsPoller
import org.bigblackowl.debttracker.core.settings.AppSettings
import org.bigblackowl.debttracker.domain.model.AppNotification
import org.bigblackowl.debttracker.domain.repository.NotificationRepository

/**
 * Polling — not a Flow repository (spec §7, [NotificationRepository] is purely suspend fetches), so
 * the screen refreshes manually: immediately on open and every time [NotificationsPoller.unreadCount]
 * changes (i.e. once every 15s while background polling is running) — without its own separate timer.
 */
class NotificationsViewModel(
    private val notificationRepository: NotificationRepository,
    notificationsPoller: NotificationsPoller,
    private val appSettings: AppSettings,
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

            is NotificationsIntent.ApproveLinkRequest -> viewModelScope.launch {
                if (notificationRepository.approveLinkRequest(intent.requestId)) {
                    notificationRepository.delete(intent.notificationId)
                    refresh()
                } else {
                    effectsChannel.send(NotificationsEffect.Error(resolveStrings(appSettings.locale).notifications.actionError))
                }
            }

            is NotificationsIntent.RejectLinkRequest -> viewModelScope.launch {
                if (notificationRepository.rejectLinkRequest(intent.requestId)) {
                    notificationRepository.delete(intent.notificationId)
                    refresh()
                } else {
                    effectsChannel.send(NotificationsEffect.Error(resolveStrings(appSettings.locale).notifications.actionError))
                }
            }

            is NotificationsIntent.OpenCorrectionDialog ->
                _state.update { it.copy(correctionDialogFor = intent.notification) }

            NotificationsIntent.DismissCorrectionDialog ->
                _state.update { it.copy(correctionDialogFor = null) }

            is NotificationsIntent.SubmitCorrection -> viewModelScope.launch {
                _state.update { it.copy(correctionDialogFor = null) }
                if (notificationRepository.proposeTransactionCorrection(intent.notificationId, intent.reason, intent.amount)) {
                    notificationRepository.delete(intent.notificationId)
                    refresh()
                } else {
                    effectsChannel.send(NotificationsEffect.Error(resolveStrings(appSettings.locale).notifications.actionError))
                }
            }

            is NotificationsIntent.ApproveCorrection -> viewModelScope.launch {
                if (notificationRepository.approveTransactionCorrection(intent.correctionId)) {
                    notificationRepository.delete(intent.notificationId)
                    refresh()
                } else {
                    effectsChannel.send(NotificationsEffect.Error(resolveStrings(appSettings.locale).notifications.actionError))
                }
            }

            is NotificationsIntent.RejectCorrection -> viewModelScope.launch {
                if (notificationRepository.rejectTransactionCorrection(intent.correctionId)) {
                    notificationRepository.delete(intent.notificationId)
                    refresh()
                } else {
                    effectsChannel.send(NotificationsEffect.Error(resolveStrings(appSettings.locale).notifications.actionError))
                }
            }
        }
    }

    private suspend fun refresh() {
        val notifications: List<AppNotification> = notificationRepository.fetchAll()
        _state.update { it.copy(isLoading = false, notifications = notifications) }
    }
}
