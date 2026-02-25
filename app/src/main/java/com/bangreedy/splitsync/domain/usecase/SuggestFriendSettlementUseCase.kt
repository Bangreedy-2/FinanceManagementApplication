package com.bangreedy.splitsync.domain.usecase

import com.bangreedy.splitsync.domain.model.FxRateResult
import com.bangreedy.splitsync.domain.model.SettlementDirection
import com.bangreedy.splitsync.domain.model.SettlementMode
import com.bangreedy.splitsync.domain.model.SettlementSuggestion
import com.bangreedy.splitsync.domain.repository.ExchangeRateRepository
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.abs

/**
 * Produces settlement suggestions for a friend pairwise balance.
 *
 * netByCurrency: positive = friend owes me, negative = I owe friend
 */
class SuggestFriendSettlementUseCase(
    private val exchangeRateRepo: ExchangeRateRepository
) {
    /**
     * Returns one-currency suggestions for each non-zero bucket,
     * plus one multi-currency suggestion in defaultCurrency.
     */
    suspend operator fun invoke(
        netByCurrency: Map<String, Long>,
        defaultCurrency: String
    ): FriendSettlementSuggestions {
        val oneCurrency = buildOneCurrencySuggestions(netByCurrency)
        val multi = buildMultiCurrencySuggestion(netByCurrency, defaultCurrency)
        return FriendSettlementSuggestions(
            oneCurrency = oneCurrency,
            multiCurrency = multi
        )
    }

    private fun buildOneCurrencySuggestions(
        netByCurrency: Map<String, Long>
    ): Map<String, SettlementSuggestion> {
        val result = mutableMapOf<String, SettlementSuggestion>()
        for ((currency, net) in netByCurrency) {
            if (net == 0L) continue
            val direction = if (net < 0) SettlementDirection.I_PAY_FRIEND
                            else SettlementDirection.FRIEND_PAYS_ME
            result[currency] = SettlementSuggestion(
                mode = SettlementMode.ONE_CURRENCY,
                payCurrency = currency,
                payAmountMinor = abs(net),
                direction = direction,
                breakdownByCurrency = mapOf(currency to abs(net))
            )
        }
        return result
    }

    private suspend fun buildMultiCurrencySuggestion(
        netByCurrency: Map<String, Long>,
        defaultCurrency: String
    ): SettlementSuggestion? {
        if (netByCurrency.all { it.value == 0L }) return null

        // Convert all buckets to defaultCurrency to get total exposure
        var totalDefaultMinor = 0L
        var latestFetchedAt: Long? = null
        val missingRates = mutableSetOf<String>()

        for ((currency, net) in netByCurrency) {
            if (net == 0L) continue
            if (currency.equals(defaultCurrency, ignoreCase = true)) {
                totalDefaultMinor += net
                continue
            }

            val rateResult = exchangeRateRepo.getRate(currency, defaultCurrency, null)
            when (rateResult) {
                is FxRateResult.Success -> {
                    val rate = rateResult.rate
                    val converted = BigDecimal(abs(net))
                        .multiply(BigDecimal(rate.rate))
                        .setScale(0, RoundingMode.HALF_UP)
                        .toLong()
                    totalDefaultMinor += if (net > 0) converted else -converted

                    val fetchedMs = rate.fetchedAt.epochSecond * 1000L
                    if (fetchedMs > 0) {
                        latestFetchedAt = maxOf(latestFetchedAt ?: 0L, fetchedMs)
                    }
                }
                is FxRateResult.Error -> {
                    missingRates.add(currency.uppercase())
                }
            }
        }

        if (totalDefaultMinor == 0L && missingRates.isEmpty()) return null

        val direction = if (totalDefaultMinor < 0) SettlementDirection.I_PAY_FRIEND
                        else SettlementDirection.FRIEND_PAYS_ME
        val payAmount = abs(totalDefaultMinor)

        // Build breakdown: distribute payment across debt buckets using greedy algorithm
        val breakdown = computeGreedyBreakdown(
            netByCurrency = netByCurrency,
            payAmountMinor = payAmount,
            payCurrency = defaultCurrency,
            direction = direction
        )

        return SettlementSuggestion(
            mode = SettlementMode.MULTI_CURRENCY,
            payCurrency = defaultCurrency,
            payAmountMinor = payAmount,
            direction = direction,
            breakdownByCurrency = breakdown,
            ratesLastUpdatedAtMillis = latestFetchedAt,
            missingRates = missingRates
        )
    }

    /**
     * Greedy breakdown: given a payment of [payAmountMinor] in [payCurrency],
     * distribute it across debt buckets by converting and reducing each one.
     *
     * "Debt buckets" are the ones with the appropriate sign:
     * - If I_PAY_FRIEND: buckets where net < 0 (I owe)
     * - If FRIEND_PAYS_ME: buckets where net > 0 (friend owes)
     */
    private suspend fun computeGreedyBreakdown(
        netByCurrency: Map<String, Long>,
        payAmountMinor: Long,
        payCurrency: String,
        direction: SettlementDirection
    ): Map<String, Long> {
        val breakdown = mutableMapOf<String, Long>()
        var remainingPay = payAmountMinor

        // Select buckets of correct sign, sorted by abs value descending
        val debtBuckets = netByCurrency
            .filter { (_, net) ->
                if (direction == SettlementDirection.I_PAY_FRIEND) net < 0 else net > 0
            }
            .entries
            .sortedByDescending { abs(it.value) }

        for ((currency, net) in debtBuckets) {
            if (remainingPay <= 0) break
            val bucketAbs = abs(net)

            if (currency.equals(payCurrency, ignoreCase = true)) {
                val take = minOf(remainingPay, bucketAbs)
                breakdown[currency] = take
                remainingPay -= take
            } else {
                // Convert bucket value to payCurrency
                val rateResult = exchangeRateRepo.getRate(currency, payCurrency, null)
                if (rateResult is FxRateResult.Success) {
                    val rate = rateResult.rate.rate
                    val bucketInPayCurrency = BigDecimal(bucketAbs)
                        .multiply(BigDecimal(rate))
                        .setScale(0, RoundingMode.HALF_UP)
                        .toLong()

                    val takePay = minOf(remainingPay, bucketInPayCurrency)

                    // Convert back to original currency
                    val takeInOriginal = if (rate > 0) {
                        BigDecimal(takePay)
                            .divide(BigDecimal(rate), 0, RoundingMode.HALF_UP)
                            .toLong()
                    } else bucketAbs

                    breakdown[currency] = minOf(takeInOriginal, bucketAbs)
                    remainingPay -= takePay
                }
                // If rate fails, skip this bucket
            }
        }

        return breakdown
    }
}

data class FriendSettlementSuggestions(
    val oneCurrency: Map<String, SettlementSuggestion>,
    val multiCurrency: SettlementSuggestion?
)



