package com.bangreedy.splitsync.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.bangreedy.splitsync.data.local.entity.ExpenseEntity
import com.bangreedy.splitsync.data.local.entity.ExpenseSplitEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {

    @Transaction
    @Query("SELECT * FROM expenses WHERE groupId = :groupId AND deleted = 0 ORDER BY createdAt DESC")
    fun observeExpensesWithSplits(groupId: String): Flow<List<ExpenseWithSplits>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertExpense(expense: ExpenseEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSplits(splits: List<ExpenseSplitEntity>)

    @Query("DELETE FROM expense_splits WHERE expenseId = :expenseId")
    suspend fun deleteSplitsForExpense(expenseId: String)

    @Query("SELECT * FROM expenses WHERE syncState = :dirtyState AND deleted = 0")
    suspend fun getDirtyExpenses(dirtyState: Int): List<com.bangreedy.splitsync.data.local.entity.ExpenseEntity>

    @Query("UPDATE expenses SET syncState = :newState WHERE id = :expenseId")
    suspend fun setExpenseSyncState(expenseId: String, newState: Int)

    @Query("SELECT * FROM expense_splits WHERE expenseId = :expenseId")
    suspend fun getSplitsForExpense(expenseId: String): List<com.bangreedy.splitsync.data.local.entity.ExpenseSplitEntity>

    @Query("""
    SELECT COUNT(*)
    FROM expenses e
    LEFT JOIN user_profiles up ON up.uid = e.payerMemberId
    WHERE e.deleted = 0
      AND e.payerMemberId IS NOT NULL
      AND e.payerMemberId != ''
      AND up.uid IS NULL
""")
    suspend fun countExpensesWithMissingPayerProfiles(): Int

    @Query("""
    SELECT COUNT(*)
    FROM expense_splits es
    LEFT JOIN user_profiles up ON up.uid = es.memberId
    WHERE up.uid IS NULL
""")
    suspend fun countSplitsWithMissingProfiles(): Int

    /** All non-deleted expenses across all contexts (groups + direct). Used by friend activity. */
    @Transaction
    @Query("SELECT * FROM expenses WHERE deleted = 0 ORDER BY createdAt DESC")
    fun observeAllExpensesWithSplits(): Flow<List<ExpenseWithSplits>>

    /** Expenses for a specific context (DIRECT thread or GROUP). */
    @Transaction
    @Query("SELECT * FROM expenses WHERE contextType = :contextType AND contextId = :contextId AND deleted = 0 ORDER BY createdAt DESC")
    fun observeExpensesByContext(contextType: String, contextId: String): Flow<List<ExpenseWithSplits>>
}

