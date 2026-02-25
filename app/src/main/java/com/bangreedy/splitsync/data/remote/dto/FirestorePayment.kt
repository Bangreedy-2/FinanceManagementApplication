package com.bangreedy.splitsync.data.remote.dto

data class FirestorePayment(
    val id: String = "",
    val groupId: String = "",
    val fromMemberId: String = "",
    val toMemberId: String = "",

    val amountMinor: Long = 0L,
    val currency: String = "EUR",

    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val deleted: Boolean = false,

    val mode: String = "ONE_CURRENCY",
    val breakdownByCurrency: Map<String, Long>? = null,
    val ratesLastUpdatedAt: Long? = null,
    val asOfDate: String? = null,
    val settlementId: String? = null
)
