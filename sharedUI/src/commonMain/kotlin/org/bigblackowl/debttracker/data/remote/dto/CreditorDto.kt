package org.bigblackowl.debttracker.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Wire format for the Supabase `creditors` table — snake_case columns, mapped to/from [org.bigblackowl.debttracker.domain.model.Creditor] in `RemoteMapper`. */
@Serializable
data class CreditorDto(
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
    @SerialName("mirror_debtor_id") val mirrorDebtorId: String? = null,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String,
)

/** Wire format for the Supabase `creditor_transactions` table, mapped to/from [org.bigblackowl.debttracker.domain.model.CreditorTransaction]. */
@Serializable
data class CreditorTransactionDto(
    val id: String,
    @SerialName("creditor_id") val creditorId: String,
    @SerialName("user_id") val userId: String,
    val amount: Double,
    val method: String,
    @SerialName("transaction_date") val transactionDate: String,
    val comment: String? = null,
    @SerialName("is_deleted") val isDeleted: Boolean,
    @SerialName("mirror_transaction_id") val mirrorTransactionId: String? = null,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String,
)
