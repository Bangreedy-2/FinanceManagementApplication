package com.bangreedy.splitsync.domain.rules

import com.bangreedy.splitsync.domain.model.SplitShare

object EqualSplitCalculator {

    /**
     * Splits amountMinor across participants equally (in minor units),
     * distributing remainder (+1) to the first N participants deterministically.
     *
     * Guarantees: sum(owedMinor) == amountMinor
     */
    fun split(amountMinor: Long, participantIds: List<String>): List<SplitShare> {
        require(amountMinor > 0) { "amountMinor must be > 0" }
        require(participantIds.isNotEmpty()) { "participants cannot be empty" }

        val unique = participantIds.distinct()
        require(unique.size == participantIds.size) { "participants contain duplicates" }

        val n = unique.size
        val base = amountMinor / n
        val remainder = (amountMinor % n).toInt() // 0..n-1

        return unique.mapIndexed { index, memberId ->
            val extra = if (index < remainder) 1L else 0L
            SplitShare(memberId = memberId, owedMinor = base + extra)
        }
    }
}
