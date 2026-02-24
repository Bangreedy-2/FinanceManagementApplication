package com.bangreedy.splitsync.data.remote.exchange

import java.time.LocalDate

/**
 * Data model for rates returned from the exchange rate API.
 */
data class RemoteBaseRates(
    val base: String,
    val date: LocalDate,
    val rates: Map<String, Double>
)

/**
 * Remote data source for fetching currency exchange rates.
 * Implementations should handle fallback mechanisms and retries.
 */
interface FxRemoteDataSource {
    /**
     * Get exchange rates for a specific base currency.
     *
     * @param base Currency code (e.g., "USD")
     * @param date The date for which to get rates. If null, uses latest.
     * @return RemoteBaseRates containing the base, date, and rates
     * @throws Exception if fetching fails after retries
     */
    suspend fun getRatesForBase(base: String, date: LocalDate? = null): RemoteBaseRates

    /**
     * List all available currencies.
     *
     * @param date Optional date parameter (some APIs support this)
     * @return Map of currency code -> currency name
     * @throws Exception if fetching fails after retries
     */
    suspend fun listCurrencies(date: LocalDate? = null): Map<String, String>
}

