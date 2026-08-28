package org.bigblackowl.debttracker.data.remote

import com.ionspin.kotlin.bignum.decimal.BigDecimal
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Count
import org.bigblackowl.debttracker.data.remote.dto.NotificationDto
import org.bigblackowl.debttracker.domain.model.AppNotification
import org.bigblackowl.debttracker.domain.model.Currency
import org.bigblackowl.debttracker.domain.model.NotificationType
import org.bigblackowl.debttracker.domain.repository.AuthRepository
import org.bigblackowl.debttracker.domain.repository.NotificationRepository

private fun NotificationDto.toDomain() = AppNotification(
    id = id,
    type = NotificationType.valueOf(type),
    actorDisplayName = actorDisplayName,
    relatedDebtorId = relatedDebtorId,
    relatedCreditorId = relatedCreditorId,
    amount = amount?.let { BigDecimal.parseString(it.toString()) },
    currency = currency?.let { runCatching { Currency.valueOf(it) }.getOrNull() },
    isRead = isRead,
    createdAt = kotlin.time.Instant.parse(createdAt),
)

/**
 * [NotificationRepository] backed directly by Postgrest — deliberately no Room (сповіщення мають
 * сенс лише в Account+Sync режимі, як і саме дзеркалювання боргів). Мережеві помилки ковтаються
 * до порожнього результату, як і [SupabaseProfileLookupRepository] — опитування раз на 15с не
 * повинно валити застосунок чи спамити помилками при відсутності мережі.
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
            // Лише count(), без завантаження тіла рядків — раз на 15с опитування не повинно
            // тягнути повні DTO заради самого лише числа.
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
}
