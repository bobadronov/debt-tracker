package org.bigblackowl.debttracker.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Wire format for the Supabase `debtors` table — snake_case columns, mapped to/from [org.bigblackowl.debttracker.domain.model.Debtor] in `RemoteMapper`. */
@Serializable
data class DebtorDto(
    val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("full_name") val fullName: String,
    val phone: String? = null,
    val email: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    val comment: String? = null,
    val status: String,
    val currency: String = "UAH",
    @SerialName("is_deleted") val isDeleted: Boolean,
    @SerialName("linked_user_id") val linkedUserId: String? = null,
    @SerialName("mirror_creditor_id") val mirrorCreditorId: String? = null,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String,
)

/** Wire format for the Supabase `debt_transactions` table, mapped to/from [org.bigblackowl.debttracker.domain.model.DebtTransaction]. */
@Serializable
data class DebtTransactionDto(
    val id: String,
    @SerialName("debtor_id") val debtorId: String,
    @SerialName("user_id") val userId: String,
    // numeric(14,2) → Double (саме так PostgREST серіалізує numeric у JSON).
    // Конвертація в/з BigDecimal — у мапері; для сум із 2 знаками після коми
    // в межах numeric(14,2) Double не втрачає точності.
    val amount: Double,
    val method: String,
    @SerialName("transaction_date") val transactionDate: String,
    val comment: String? = null,
    @SerialName("is_deleted") val isDeleted: Boolean,
    @SerialName("mirror_transaction_id") val mirrorTransactionId: String? = null,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String,
)
