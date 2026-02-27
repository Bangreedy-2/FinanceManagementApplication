package com.bangreedy.splitsync.domain.model

data class TotalInDefault(
    val amountMinor: Long,
    val currency: String,
    val isApprox: Boolean,
    val lastUpdatedAtMillis: Long? = null,
    val missingCurrencies: Set<String> = emptySet()
)

