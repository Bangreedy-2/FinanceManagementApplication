package com.bangreedy.splitsync.domain.repository

import com.bangreedy.splitsync.domain.model.Expense
import kotlinx.coroutines.flow.Flow

interface ExpenseRepository {
    fun observeExpenses(groupId: String): Flow<List<Expense>>
    suspend fun createExpense(expense: Expense): String
}
