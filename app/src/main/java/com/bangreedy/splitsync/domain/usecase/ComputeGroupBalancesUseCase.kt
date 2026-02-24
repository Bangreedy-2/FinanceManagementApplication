package com.bangreedy.splitsync.domain.usecase

import com.bangreedy.splitsync.domain.model.Expense
import com.bangreedy.splitsync.domain.model.Payment

class ComputeGroupBalancesUseCase {

    /**
     * Returns memberId -> balanceMinor.
     * Positive means others owe them. Negative means they owe others.
     *
     * This version uses expenses only (payments/settlements added later).
     */
    operator fun invoke(
        expenses: List<Expense>,
        payments: List<Payment>
    ): Map<String, Long>
{
        val balances = mutableMapOf<String, Long>()

        fun add(memberId: String, delta: Long) {
            balances[memberId] = (balances[memberId] ?: 0L) + delta
        }

        for (e in expenses) {
            if (e.deleted) continue

            add(e.payerMemberId, e.amountMinor)

            for (s in e.splits) {
                // owedMinor should be >= 0
                add(s.memberId, -s.owedMinor)
            }

            // Debug invariant: sums to 0 for each expense
            // payer +amount, participants -sum(owed) => should net 0
        }
        for (p in payments) {
            if (p.deleted) continue
            add(p.fromMemberId, p.amountMinor)
            add(p.toMemberId, -p.amountMinor)
        }


    return balances
    }
}
