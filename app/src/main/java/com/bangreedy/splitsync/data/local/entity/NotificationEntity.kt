package com.bangreedy.splitsync.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "notifications",
    indices = [
        Index("ownerUid"),
        Index("createdAt"),
        Index("read")
    ]
)
data class NotificationEntity(
    @PrimaryKey val id: String,
    val ownerUid: String,          // the user who sees it
    val type: String,              // invite_received, invite_accepted, settlement_recorded, etc.
    val title: String,
    val body: String,
    val groupId: String? = null,
    val actorUid: String? = null,
    val createdAt: Long,
    val read: Boolean = false,
    val deleted: Boolean = false,
    val syncState: Int = SyncState.SYNCED
)