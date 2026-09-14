package org.bigblackowl.debttracker.data.remote

import com.ionspin.kotlin.bignum.decimal.BigDecimal
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Count
import io.github.jan.supabase.postgrest.rpc
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.bigblackowl.debttracker.data.remote.dto.NotificationDto
import org.bigblackowl.debttracker.domain.model.AppNotification
import org.bigblackowl.debttracker.domain.model.CorrectionReason
import org.bigblackowl.debttracker.domain.model.Currency
import org.bigblackowl.debttracker.domain.model.NotificationType
import org.bigblackowl.debttracker.domain.repository.AuthRepository
import org.bigblackowl.debttracker.domain.repository.NotificationRepository

@Serializable
private data class LinkRequestParams(@SerialName("p_request_id") val requestId: String)

@Serializable
private data class ProposeCorrectionParams(
    @SerialName("p_notification_id") val notificationId: String,
    @SerialName("p_reason") val reason: String,
    @SerialName("p_proposed_amount") val proposedAmount: Double?,
)

@Serializable
private data class CorrectionParams(@SerialName("p_correction_id") val correctionId: String)

private fun NotificationDto.toDomain() = AppNotification(
    id = id,
    type = NotificationType.valueOf(type),
    actorDisplayName = actorDisplayName,
    relatedDebtorId = relatedDebtorId,
    relatedCreditorId = relatedCreditorId,
    relatedLinkRequestId = relatedLinkRequestId,
    relatedTransactionId = relatedTransactionId,
    relatedCorrectionId = relatedCorrectionId,
    amount = amount?.let { BigDecimal.parseString(it.toString()) },
    currency = currency?.let { runCatching { Currency.valueOf(it) }.getOrNull() },
    isRead = isRead,
    createdAt = kotlin.time.Instant.parse(createdAt),
)

/**
 * [NotificationRepository] backed directly by Postgrest — deliberately no Room (notifications only
 * make sense in Account+Sync mode, same as debt mirroring itself). Network errors are swallowed
 * down to an empty result, same as [SupabaseProfileLookupRepository] — polling once every 15s
 * shouldn't crash the app or spam errors when there's no network.
 */
class SupabaseNotificationRepository(
    private val client: SupabaseClient,
    private val authRepository: AuthRepository,
) : NotificationRepository {

    private fun currentUserIdOrNull(): String? =
        authRepository.currentUserId.takeIf { authRepository.isAuthenticated.value }

    override suspend fun fetchSince(after: kotlin.time.Instant?): List<AppNotification> {
        val userId = currentUserIdOrNull() ?: return emptyList()
        return runCatching {
            client.from("notifications")
                .select {
                    filter {
                        eq("user_id", userId)
                        if (after != null) gt("created_at", after.toString())
                    }
                }
                .decodeList<NotificationDto>()
                .map { it.toDomain() }
                .sortedByDescending { it.createdAt }
        }.getOrDefault(emptyList())
    }

    override suspend fun fetchAll(): List<AppNotification> {
        val userId = currentUserIdOrNull() ?: return emptyList()
        return runCatching {
            client.from("notifications")
                .select { filter { eq("user_id", userId) } }
                .decodeList<NotificationDto>()
                .map { it.toDomain() }
                .sortedByDescending { it.createdAt }
        }.getOrDefault(emptyList())
    }

    override suspend fun unreadCount(): Int {
        val userId = currentUserIdOrNull() ?: return 0
        return runCatching {
            // Just count(), without loading the row bodies — polling once every 15s shouldn't
            // pull full DTOs just for a single number.
            client.from("notifications")
                .select(columns = Columns.list("id")) {
                    filter { eq("user_id", userId); eq("is_read", false) }
                    count(Count.EXACT)
                }
                .countOrNull()?.toInt() ?: 0
        }.getOrDefault(0)
    }

    override suspend fun markRead(id: String) {
        val userId = currentUserIdOrNull() ?: return
        runCatching {
            client.from("notifications").update({ set("is_read", true) }) {
                filter { eq("id", id); eq("user_id", userId) }
            }
        }
    }

    override suspend fun markAllRead() {
        val userId = currentUserIdOrNull() ?: return
        runCatching {
            client.from("notifications").update({ set("is_read", true) }) {
                filter { eq("user_id", userId); eq("is_read", false) }
            }
        }
    }

    override suspend fun delete(id: String) {
        val userId = currentUserIdOrNull() ?: return
        runCatching {
            client.from("notifications").delete { filter { eq("id", id); eq("user_id", userId) } }
        }
    }

    override suspend fun approveLinkRequest(requestId: String): Boolean {
        if (currentUserIdOrNull() == null) return false
        return runCatching {
            client.postgrest.rpc("approve_link_request", LinkRequestParams(requestId))
        }.isSuccess
    }

    override suspend fun rejectLinkRequest(requestId: String): Boolean {
        if (currentUserIdOrNull() == null) return false
        return runCatching {
            client.postgrest.rpc("reject_link_request", LinkRequestParams(requestId))
        }.isSuccess
    }

    override suspend fun proposeTransactionCorrection(
        notificationId: String,
        reason: CorrectionReason,
        amount: BigDecimal?,
    ): Boolean {
        if (currentUserIdOrNull() == null) return false
        return runCatching {
            client.postgrest.rpc(
                "propose_transaction_correction",
                ProposeCorrectionParams(
                    notificationId = notificationId,
                    reason = reason.wire,
                    proposedAmount = amount?.let { it.abs().toStringExpanded().toDouble() },
                ),
            )
        }.isSuccess
    }

    override suspend fun approveTransactionCorrection(correctionId: String): Boolean {
        if (currentUserIdOrNull() == null) return false
        return runCatching {
            client.postgrest.rpc("approve_transaction_correction", CorrectionParams(correctionId))
        }.isSuccess
    }

    override suspend fun rejectTransactionCorrection(correctionId: String): Boolean {
        if (currentUserIdOrNull() == null) return false
        return runCatching {
            client.postgrest.rpc("reject_transaction_correction", CorrectionParams(correctionId))
        }.isSuccess
    }
}
