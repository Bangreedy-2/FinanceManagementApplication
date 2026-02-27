package com.bangreedy.splitsync.domain.model

data class DebtBucket(
    val contextType: String, // GROUP or DIRECT
    val contextId: String,   // groupId or threadId
    val currency: String,
    val netMinor: Long,      // + friend owes me, - I owe friend
    val label: String        // "Group: Trip" or "Direct"
)

data class SettlementLine(
    val contextType: String,
    val contextId: String,
    val currency: String,
    val amountMinor: Long,   // absolute amount being settled IN THAT CURRENCY
    val fromUid: String,
    val toUid: String
)

data class FxLock(
    val fromCurrency: String,
    val toCurrency: String,
    val rate: Double,
    val asOfDate: String,
    val provider: String,
    val fetchedAtMillis: Long
)

data class SettlementPlan(
    val settlementId: String,
    val friendUid: String,
    val lines: List<SettlementLine>,
    val payCurrency: String,
    val payAmountMinor: Long,
    val payDirection: SettlementDirection,
    val fxLocks: List<FxLock>,
    val createdAt: Long
)

