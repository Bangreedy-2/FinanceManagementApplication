package com.bangreedy.splitsync.data.local.entity

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "friends",
    primaryKeys = ["ownerUid", "friendUid"],
    indices = [Index("ownerUid"), Index("friendUid"), Index("status")]
)
data class FriendEntity(
    val ownerUid: String,
    val friendUid: String,
    val status: String,         // "pending_outgoing", "pending_incoming", "accepted", "blocked"
    val nickname: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val deleted: Boolean = false,
    val syncState: Int = SyncState.SYNCED
)

