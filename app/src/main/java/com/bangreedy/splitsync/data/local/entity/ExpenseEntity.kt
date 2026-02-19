package com.bangreedy.splitsync.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "expenses",
    indices = [Index("groupId"), Index("payerMemberId")]
)
data class ExpenseEntity(
    @PrimaryKey val id: String,
    val groupId: String,
    val payerMemberId: String, // uid, no FK
    val amountMinor: Long,
    val currency: String,
    val note: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val deleted: Boolean = false,
    val syncState: Int = SyncState.SYNCED
)
