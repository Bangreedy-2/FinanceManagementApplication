package com.bangreedy.splitsync.data.local.dao

import androidx.room.Embedded
import androidx.room.Relation
import com.bangreedy.splitsync.data.local.entity.ExpenseEntity
import com.bangreedy.splitsync.data.local.entity.ExpenseSplitEntity

data class ExpenseWithSplits(
    @Embedded val expense: ExpenseEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "expenseId"
    )
    val splits: List<ExpenseSplitEntity>
)
