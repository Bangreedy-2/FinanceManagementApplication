package com.bangreedy.splitsync.domain.model

import java.time.Instant
import java.time.LocalDate

/**
 * Exchange rate between two currencies on a specific date.
 */
data class FxRate(
    val base: String,
    val quote: String,
    val rateDate: LocalDate,
    val rate: Double,
    val fetchedAt: Instant
)

/**
 * Result of a currency conversion with metadata.
 */
data class ConversionResult(
    val convertedMinor: Long,
    val rate: Double,
    val rateDate: LocalDate,
    val fetchedAt: Instant,
    val source: FxSource
)

enum class FxSource {
    Remote, Cache
}

/**
 * Wrapper for rate fetch results.
 * Either a successful FxRate or an error message.
 */
sealed class FxRateResult {
    data class Success(val rate: FxRate) : FxRateResult()
    data class Error(val message: String) : FxRateResult()
}

