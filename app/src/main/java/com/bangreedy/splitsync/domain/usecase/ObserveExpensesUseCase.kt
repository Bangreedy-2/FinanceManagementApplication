package com.bangreedy.splitsync.domain.usecase

import com.bangreedy.splitsync.domain.model.Expense
import com.bangreedy.splitsync.domain.repository.ExpenseRepository
import kotlinx.coroutines.flow.Flow

class ObserveExpensesUseCase(
    private val repo: ExpenseRepository
) {
    operator fun invoke(groupId: String): Flow<List<Expense>> = repo.observeExpenses(groupId)
}
