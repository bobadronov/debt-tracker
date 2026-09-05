package org.bigblackowl.debttracker.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Wire format for the Supabase `notifications` table, mapped to/from [org.bigblackowl.debttracker.domain.model.AppNotification]. */
@Serializable
data class NotificationDto(
    val id: String,
    val type: String,
    @SerialName("actor_display_name") val actorDisplayName: String? = null,
    @SerialName("related_debtor_id") val relatedDebtorId: String? = null,
    @SerialName("related_creditor_id") val relatedCreditorId: String? = null,
    @SerialName("related_link_request_id") val relatedLinkRequestId: String? = null,
    val amount: Double? = null,
    val currency: String? = null,
    @SerialName("is_read") val isRead: Boolean = false,
    @SerialName("created_at") val createdAt: String,
)
