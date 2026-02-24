package com.bangreedy.splitsync.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "groups")
data class GroupEntity(
    @PrimaryKey val id: String,
    val name: String,
    val createdAt: Long,
    val updatedAt: Long,
    val deleted: Boolean,
    // local-only
    val syncState: Int
)

object SyncState {
    const val SYNCED = 0
    const val DIRTY = 1
    const val CONFLICT = 2
}
