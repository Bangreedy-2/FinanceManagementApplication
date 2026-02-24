package com.bangreedy.splitsync.domain.usecase

import com.bangreedy.splitsync.domain.model.Expense
import com.bangreedy.splitsync.domain.repository.ExpenseRepository
import com.bangreedy.splitsync.domain.rules.EqualSplitCalculator
import java.util.UUID

class CreateExpenseEqualSplitUseCase(
    private val expenseRepo: ExpenseRepository
) {
    /**
     * Creates an expense with equal split among participants.
     * payerMemberId must be included in participantIds (typical Splitwise behavior).
     */
    suspend operator fun invoke(
        groupId: String,
        payerMemberId: String,
        amountMinor: Long,
        currency: String,
        note: String?,
        participantIds: List<String>
    ): String {
        require(groupId.isNotBlank()) { "groupId required" }
        require(payerMemberId.isNotBlank()) { "payerMemberId required" }
        require(currency.isNotBlank()) { "currency required" }
        require(amountMinor > 0) { "amountMinor must be > 0" }

        val participants = participantIds.distinct()
        require(participants.isNotEmpty()) { "participants required" }
        require(payerMemberId in participants) { "payer must be included in participants" }

        val splits = EqualSplitCalculator.split(amountMinor, participants)
        // Extra safety:
        check(splits.sumOf { it.owedMinor } == amountMinor) { "Split sum mismatch" }

        val now = System.currentTimeMillis()
        val expense = Expense(
            id = UUID.randomUUID().toString(),
            groupId = groupId,
            payerMemberId = payerMemberId,
            amountMinor = amountMinor,
            currency = currency,
            note = note?.takeIf { it.isNotBlank() },
            createdAt = now,
            updatedAt = now,
            deleted = false,
            splits = splits
        )

        return expenseRepo.createExpense(expense)
    }
}
