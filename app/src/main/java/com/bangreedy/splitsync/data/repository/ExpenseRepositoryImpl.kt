package com.bangreedy.splitsync.data.repository

import com.bangreedy.splitsync.data.local.dao.ExpenseDao
import com.bangreedy.splitsync.data.mapper.toDomain
import com.bangreedy.splitsync.data.mapper.toEntity
import com.bangreedy.splitsync.data.mapper.toSplitEntities
import com.bangreedy.splitsync.domain.model.Expense
import com.bangreedy.splitsync.domain.repository.ExpenseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ExpenseRepositoryImpl(
    private val expenseDao: ExpenseDao
) : ExpenseRepository {

    override fun observeExpenses(groupId: String): Flow<List<Expense>> =
        expenseDao.observeExpensesWithSplits(groupId)
            .map { list -> list.map { it.toDomain() } }

    override suspend fun createExpense(expense: Expense): String {
        // Safety invariants for money integrity
        require(expense.amountMinor > 0) { "Expense amount must be > 0" }
        require(expense.splits.isNotEmpty()) { "Expense must have splits" }
        require(expense.splits.sumOf { it.owedMinor } == expense.amountMinor) { "Split sum mismatch" }

        // 1) upsert expense
        expenseDao.upsertExpense(expense.toEntity())

        // 2) replace splits for this expense
        expenseDao.deleteSplitsForExpense(expense.id)
        expenseDao.upsertSplits(expense.toSplitEntities())

        return expense.id
    }
}
