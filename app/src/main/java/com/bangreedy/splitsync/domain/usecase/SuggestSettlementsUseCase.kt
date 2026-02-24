package com.bangreedy.splitsync.domain.usecase

import com.bangreedy.splitsync.domain.model.Settlement
import kotlin.math.abs

class SuggestSettlementsUseCase {

    /**
     * Input: memberId -> balanceMinor
     *  >0 means member should receive money
     *  <0 means member should pay money
     */
    operator fun invoke(balances: Map<String, Long>): List<Settlement> {
        val creditors = balances
            .filterValues { it > 0L }
            .map { it.key to it.value }
            .sortedByDescending { it.second }
            .toMutableList()

        val debtors = balances
            .filterValues { it < 0L }
            .map { it.key to abs(it.value) } // amount they must pay
            .sortedByDescending { it.second }
            .toMutableList()

        val result = mutableListOf<Settlement>()

        var i = 0 // debtors
        var j = 0 // creditors

        while (i < debtors.size && j < creditors.size) {
            val (debtorId, debtorAmt) = debtors[i]
            val (creditorId, creditorAmt) = creditors[j]

            val pay = minOf(debtorAmt, creditorAmt)
            if (pay > 0) {
                result += Settlement(
                    fromMemberId = debtorId,
                    toMemberId = creditorId,
                    amountMinor = pay
                )
            }

            val newDebtorAmt = debtorAmt - pay
            val newCreditorAmt = creditorAmt - pay

            debtors[i] = debtorId to newDebtorAmt
            creditors[j] = creditorId to newCreditorAmt

            if (debtors[i].second == 0L) i++
            if (creditors[j].second == 0L) j++
        }

        return result
    }
}
