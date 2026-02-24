package com.bangreedy.splitsync.domain.model

data class Expense(
    val id: String,
    val groupId: String,
    val payerMemberId: String,
    val amountMinor: Long,
    val currency: String,
    val note: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val deleted: Boolean = false,
    val splits: List<SplitShare>,
    val contextType: String = "GROUP",
    val contextId: String = groupId
)
