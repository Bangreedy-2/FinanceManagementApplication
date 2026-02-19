package com.bangreedy.splitsync.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
@Entity(
    tableName = "expense_splits",
    primaryKeys = ["expenseId", "memberId"],
    foreignKeys = [
        ForeignKey(
            entity = ExpenseEntity::class,
            parentColumns = ["id"],
            childColumns = ["expenseId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("expenseId"),
        Index("memberId")
    ]
)
data class ExpenseSplitEntity(
    val expenseId: String,
    val memberId: String, // uid, no FK
    val owedMinor: Long
)

