package com.bangreedy.splitsync.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profiles")
data class UserProfileEntity(
    @PrimaryKey val uid: String,
    val username: String,
    val displayName: String,
    val email: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val deleted: Boolean = false,
    val syncState: Int = SyncState.SYNCED
)
