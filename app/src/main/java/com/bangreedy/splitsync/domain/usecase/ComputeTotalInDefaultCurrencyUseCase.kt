package com.bangreedy.splitsync.domain.usecase

import com.bangreedy.splitsync.domain.model.FxRateResult
import com.bangreedy.splitsync.domain.model.TotalInDefault
import com.bangreedy.splitsync.domain.repository.ExchangeRateRepository
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import kotlin.math.abs
import kotlin.math.sign

class ComputeTotalInDefaultCurrencyUseCase(
    private val exchangeRateRepo: ExchangeRateRepository
) {
    suspend operator fun invoke(
        netByCurrency: Map<String, Long>,
        defaultCurrency: String,
        asOf: LocalDate? = null
    ): TotalInDefault {
        if (netByCurrency.isEmpty()) {
            return TotalInDefault(
                amountMinor = 0L,
                currency = defaultCurrency,
                isApprox = false
            )
        }

        var totalMinor = 0L
        var anyApprox = false
        var latestCachedFetchedAt: Long? = null
        val missing = mutableSetOf<String>()

        for ((currency, netMinor) in netByCurrency) {
            if (currency.equals(defaultCurrency, ignoreCase = true)) {
                totalMinor += netMinor
                continue
            }

            val absMinor = abs(netMinor)
            val signVal = netMinor.sign

            if (absMinor == 0L) continue

            val rateResult = exchangeRateRepo.getRate(currency, defaultCurrency, asOf)

            when (rateResult) {
                is FxRateResult.Success -> {
                    val rate = rateResult.rate
                    val convertedAbs = BigDecimal(absMinor)
                        .multiply(BigDecimal(rate.rate))
                        .setScale(0, RoundingMode.HALF_UP)
                        .toLong()

                    totalMinor += signVal * convertedAbs
                    anyApprox = true

                    // Track fetchedAt using epochSecond (API 24 safe)
                    val fetchedMs = rate.fetchedAt.epochSecond * 1000L
                    if (fetchedMs > 0) {
                        latestCachedFetchedAt = when (latestCachedFetchedAt) {
                            null -> fetchedMs
                            else -> maxOf(latestCachedFetchedAt, fetchedMs)
                        }
                    }
                }
                is FxRateResult.Error -> {
                    missing.add(currency.uppercase())
                }
            }
        }

        return TotalInDefault(
            amountMinor = totalMinor,
            currency = defaultCurrency,
            isApprox = anyApprox,
            lastUpdatedAtMillis = latestCachedFetchedAt,
            missingCurrencies = missing
        )
    }
}


