package com.bangreedy.splitsync.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "expenses",
    indices = [Index("groupId"), Index("payerMemberId"), Index("contextType", "contextId")]
)
data class ExpenseEntity(
    @PrimaryKey val id: String,
    val groupId: String,
    val payerMemberId: String,
    val amountMinor: Long,
    val currency: String,
    val note: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val deleted: Boolean = false,
    val syncState: Int = SyncState.SYNCED,
    val contextType: String = "GROUP",
    val contextId: String = groupId
)
