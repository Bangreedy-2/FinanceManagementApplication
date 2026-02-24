package com.bangreedy.splitsync.domain.usecase

import com.bangreedy.splitsync.domain.model.ConversionResult
import com.bangreedy.splitsync.domain.model.FxRateResult
import com.bangreedy.splitsync.domain.model.FxSource
import com.bangreedy.splitsync.domain.repository.ExchangeRateRepository
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import kotlin.math.roundToLong

/**
 * Convert an amount from one currency to another using exchange rates.
 */
class ConvertMoneyUseCase(
    private val exchangeRateRepository: ExchangeRateRepository
) {
    suspend operator fun invoke(
        amountMinor: Long,
        fromCurrency: String,
        toCurrency: String,
        asOfDate: LocalDate? = null
    ): Result<ConversionResult> {
        if (fromCurrency.equals(toCurrency, ignoreCase = true)) {
            // Same currency, no conversion needed
            return Result.success(
                ConversionResult(
                    convertedMinor = amountMinor,
                    rate = 1.0,
                    rateDate = asOfDate ?: LocalDate.now(),
                    fetchedAt = Instant.now(),
                    source = FxSource.Cache
                )
            )
        }

        val rateResult = exchangeRateRepository.getRate(fromCurrency, toCurrency, asOfDate)
        return when (rateResult) {
            is FxRateResult.Success -> {
                val rate = rateResult.rate
                val convertedMinor = (BigDecimal(amountMinor) * BigDecimal(rate.rate)).toLong()
                Result.success(
                    ConversionResult(
                        convertedMinor = convertedMinor,
                        rate = rate.rate,
                        rateDate = rate.rateDate,
                        fetchedAt = rate.fetchedAt,
                        source = FxSource.Remote
                    )
                )
            }
            is FxRateResult.Error -> Result.failure(Exception(rateResult.message))
        }
    }
}

/**
 * Convert multiple amounts in different currencies to a single target currency.
 */
class ConvertMultiCurrencyTotalsUseCase(
    private val exchangeRateRepository: ExchangeRateRepository
) {
    suspend operator fun invoke(
        amountsByCurrency: Map<String, Long>,
        toCurrency: String,
        asOfDate: LocalDate? = null
    ): Result<Map<String, ConversionResult>> {
        val results = mutableMapOf<String, ConversionResult>()

        for ((currency, amountMinor) in amountsByCurrency) {
            if (currency.equals(toCurrency, ignoreCase = true)) {
                results[currency] = ConversionResult(
                    convertedMinor = amountMinor,
                    rate = 1.0,
                    rateDate = asOfDate ?: LocalDate.now(),
                    fetchedAt = Instant.now(),
                    source = FxSource.Cache
                )
            } else {
                val rateResult = exchangeRateRepository.getRate(currency, toCurrency, asOfDate)
                when (rateResult) {
                    is FxRateResult.Success -> {
                        val rate = rateResult.rate
                        val convertedMinor = (BigDecimal(amountMinor) * BigDecimal(rate.rate)).toLong()
                        results[currency] = ConversionResult(
                            convertedMinor = convertedMinor,
                            rate = rate.rate,
                            rateDate = rate.rateDate,
                            fetchedAt = rate.fetchedAt,
                            source = FxSource.Remote
                        )
                    }
                    is FxRateResult.Error -> {
                        return Result.failure(Exception("Failed to convert $currency: ${rateResult.message}"))
                    }
                }
            }
        }

        return Result.success(results)
    }
}


