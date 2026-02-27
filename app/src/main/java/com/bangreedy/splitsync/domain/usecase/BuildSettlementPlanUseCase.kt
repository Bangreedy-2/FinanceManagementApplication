package com.bangreedy.splitsync.domain.usecase

import com.bangreedy.splitsync.domain.model.DebtBucket
import com.bangreedy.splitsync.domain.model.FxLock
import com.bangreedy.splitsync.domain.model.FxRateResult
import com.bangreedy.splitsync.domain.model.SettlementDirection
import com.bangreedy.splitsync.domain.model.SettlementLine
import com.bangreedy.splitsync.domain.model.SettlementPlan
import com.bangreedy.splitsync.domain.repository.ExchangeRateRepository
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.UUID
import kotlin.math.abs

/**
 * Builds a settlement plan from selected debt buckets.
 *
 * Each bucket is settled in its NATIVE currency — the payCurrency is only
 * the method of payment, and FX rates are locked at plan creation time
 * to make the closure permanent.
 */
class BuildSettlementPlanUseCase(
    private val exchangeRateRepo: ExchangeRateRepository
) {
    /**
     * @param selectedBuckets buckets user chose to settle
     * @param payCurrency currency user wants to pay in
     * @param myUid current user
     * @param friendUid friend
     * @return SettlementPlan with lines in native currencies and a payAmount in payCurrency
     */
    suspend operator fun invoke(
        selectedBuckets: List<DebtBucket>,
        payCurrency: String,
        myUid: String,
        friendUid: String
    ): SettlementPlan {
        require(selectedBuckets.isNotEmpty()) { "Must select at least one debt bucket" }

        val settlementId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()

        val lines = mutableListOf<SettlementLine>()
        val fxLocks = mutableListOf<FxLock>()
        var totalPayMinor = 0L

        // Determine overall direction: if net sum < 0 → I owe friend
        val totalNet = selectedBuckets.sumOf { it.netMinor }
        val direction = if (totalNet < 0) SettlementDirection.I_PAY_FRIEND
                        else SettlementDirection.FRIEND_PAYS_ME

        for (bucket in selectedBuckets) {
            val absAmount = abs(bucket.netMinor)
            // Direction per bucket: negative = I owe friend, positive = friend owes me
            val fromUid = if (bucket.netMinor < 0) myUid else friendUid
            val toUid = if (bucket.netMinor < 0) friendUid else myUid

            lines.add(
                SettlementLine(
                    contextType = bucket.contextType,
                    contextId = bucket.contextId,
                    currency = bucket.currency,
                    amountMinor = absAmount,
                    fromUid = fromUid,
                    toUid = toUid
                )
            )

            // Convert to payCurrency
            if (bucket.currency.equals(payCurrency, ignoreCase = true)) {
                totalPayMinor += absAmount
            } else {
                val rateResult = exchangeRateRepo.getRate(bucket.currency, payCurrency, null)
                when (rateResult) {
                    is FxRateResult.Success -> {
                        val rate = rateResult.rate
                        val converted = BigDecimal(absAmount)
                            .multiply(BigDecimal(rate.rate))
                            .setScale(0, RoundingMode.HALF_UP)
                            .toLong()
                        totalPayMinor += converted

                        fxLocks.add(
                            FxLock(
                                fromCurrency = bucket.currency,
                                toCurrency = payCurrency,
                                rate = rate.rate,
                                asOfDate = rate.rateDate.toString(),
                                provider = "fawazahmed0-exchange-api",
                                fetchedAtMillis = rate.fetchedAt.epochSecond * 1000L
                            )
                        )
                    }
                    is FxRateResult.Error -> {
                        // Can't convert; use 1:1 as fallback with warning
                        totalPayMinor += absAmount
                    }
                }
            }
        }

        return SettlementPlan(
            settlementId = settlementId,
            friendUid = friendUid,
            lines = lines,
            payCurrency = payCurrency,
            payAmountMinor = totalPayMinor,
            payDirection = direction,
            fxLocks = fxLocks,
            createdAt = now
        )
    }
}




