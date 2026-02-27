package com.bangreedy.splitsync.domain.repository

interface DirectThreadRepository {
    suspend fun ensureThread(uidA: String, uidB: String): String
    suspend fun createDirectExpense(
        threadId: String,
        payerUid: String,
        amountMinor: Long,
        currency: String,
        note: String?,
        splitUids: List<String>
    ): String

    suspend fun createDirectPayment(
        threadId: String,
        fromUid: String,
        toUid: String,
        amountMinor: Long,
        currency: String,
        mode: String = "ONE_CURRENCY",
        breakdownByCurrency: Map<String, Long>? = null,
        ratesLastUpdatedAt: Long? = null,
        asOfDate: String? = null,
        settlementId: String? = null
    ): String
}

