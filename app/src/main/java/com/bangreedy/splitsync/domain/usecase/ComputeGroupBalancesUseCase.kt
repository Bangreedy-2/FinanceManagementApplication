package com.bangreedy.splitsync.domain.usecase

import com.bangreedy.splitsync.domain.model.Expense
import com.bangreedy.splitsync.domain.model.Payment

/**
 * Multi-currency aware group balance computation.
 *
 * Returns memberId -> (currency -> balanceMinor).
 * Positive means others owe them. Negative means they owe others.
 *
 * Also provides a flat single-currency view for backward compat.
 */
class ComputeGroupBalancesUseCase {

    data class MultiCurrencyBalances(
        /** memberId -> currency -> signed minor */
        val byCurrency: Map<String, Map<String, Long>>,
        /** Pairwise: (memberA, memberB) -> currency -> signed minor (A's perspective) */
        val pairwise: Map<Pair<String, String>, Map<String, Long>>
    )

    /**
     * Compute multi-currency balances for a group.
     */
    fun computeMultiCurrency(
        expenses: List<Expense>,
        payments: List<Payment>
    ): MultiCurrencyBalances {
        // memberId -> currency -> balance
        val balances = mutableMapOf<String, MutableMap<String, Long>>()
        // (orderedPair) -> currency -> signed amount (from A's perspective, A < B lexically)
        val pairwise = mutableMapOf<Pair<String, String>, MutableMap<String, Long>>()

        fun addBalance(memberId: String, currency: String, delta: Long) {
            balances.getOrPut(memberId) { mutableMapOf() }
                .let { it[currency] = (it[currency] ?: 0L) + delta }
        }

        fun addPairwise(a: String, b: String, currency: String, deltaForA: Long) {
            if (a == b) return
            val key = if (a < b) Pair(a, b) else Pair(b, a)
            val sign = if (a < b) deltaForA else -deltaForA
            pairwise.getOrPut(key) { mutableMapOf() }
                .let { it[currency] = (it[currency] ?: 0L) + sign }
        }

        for (e in expenses) {
            if (e.deleted) continue
            val currency = e.currency

            addBalance(e.payerMemberId, currency, e.amountMinor)

            for (s in e.splits) {
                addBalance(s.memberId, currency, -s.owedMinor)

                // Pairwise: payer paid for this member
                if (s.memberId != e.payerMemberId) {
                    // Payer is owed by this member
                    addPairwise(e.payerMemberId, s.memberId, currency, s.owedMinor)
                }
            }
        }

        for (p in payments) {
            if (p.deleted) continue
            val currency = p.currency

            addBalance(p.fromMemberId, currency, p.amountMinor)
            addBalance(p.toMemberId, currency, -p.amountMinor)

            // Pairwise: fromMember paid toMember
            addPairwise(p.fromMemberId, p.toMemberId, currency, p.amountMinor)
        }

        return MultiCurrencyBalances(
            byCurrency = balances,
            pairwise = pairwise
        )
    }

    /**
     * Legacy single-currency view (sums all currencies as if 1:1).
     * Used by old SuggestSettlementsUseCase — will be deprecated.
     */
    operator fun invoke(
        expenses: List<Expense>,
        payments: List<Payment>
    ): Map<String, Long> {
        val balances = mutableMapOf<String, Long>()

        fun add(memberId: String, delta: Long) {
            balances[memberId] = (balances[memberId] ?: 0L) + delta
        }

        for (e in expenses) {
            if (e.deleted) continue
            add(e.payerMemberId, e.amountMinor)
            for (s in e.splits) {
                add(s.memberId, -s.owedMinor)
            }
        }
        for (p in payments) {
            if (p.deleted) continue
            add(p.fromMemberId, p.amountMinor)
            add(p.toMemberId, -p.amountMinor)
        }

        return balances
    }
}
