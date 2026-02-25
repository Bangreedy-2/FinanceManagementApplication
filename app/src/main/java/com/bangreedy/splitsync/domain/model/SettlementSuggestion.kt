package com.bangreedy.splitsync.domain.model

enum class SettlementMode {
    ONE_CURRENCY,
    MULTI_CURRENCY
}

data class SettlementSuggestion(
    val mode: SettlementMode,
    val payCurrency: String,
    val payAmountMinor: Long,
    val direction: SettlementDirection,
    val breakdownByCurrency: Map<String, Long>,
    val ratesLastUpdatedAtMillis: Long? = null,
    val missingRates: Set<String> = emptySet()
)

enum class SettlementDirection {
    I_PAY_FRIEND,
    FRIEND_PAYS_ME
}

