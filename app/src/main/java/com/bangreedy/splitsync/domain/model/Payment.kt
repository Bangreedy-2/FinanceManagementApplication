package com.bangreedy.splitsync.domain.model

data class Payment(
    val id: String,
    val groupId: String,
    val fromMemberId: String,
    val toMemberId: String,
    val amountMinor: Long,
    val currency: String,
    val createdAt: Long,
    val updatedAt: Long,
    val deleted: Boolean = false,
    val contextType: String = "GROUP",
    val contextId: String = groupId,
    val mode: String = "ONE_CURRENCY",
    val breakdownByCurrency: Map<String, Long>? = null,
    val ratesLastUpdatedAt: Long? = null,
    val asOfDate: String? = null,
    val settlementId: String? = null
)
