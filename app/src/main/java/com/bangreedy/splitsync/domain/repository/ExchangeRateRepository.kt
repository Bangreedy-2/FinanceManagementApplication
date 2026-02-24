package com.bangreedy.splitsync.domain.repository

import com.bangreedy.splitsync.domain.model.FxRateResult
import java.time.LocalDate

interface ExchangeRateRepository {
    /**
     * Get exchange rate from base to quote currency.
     * Returns either cached or remote rate, or an error.
     *
     * @param base Currency code (e.g., "USD")
     * @param quote Currency code (e.g., "EUR")
     * @param date The date for which to get the rate. If null, uses latest.
     * @return FxRateResult (Success or Error)
     */
    suspend fun getRate(base: String, quote: String, date: LocalDate? = null): FxRateResult

    /**
     * Get multiple rates from a base to multiple quotes.
     *
     * @param base Currency code
     * @param quotes Set of currency codes to get rates for
     * @param date The date for rates. If null, uses latest.
     * @return Map of quote currency -> FxRateResult
     */
    suspend fun getRates(base: String, quotes: Set<String>, date: LocalDate? = null): Map<String, FxRateResult>
}

