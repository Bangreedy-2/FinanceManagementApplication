package com.bangreedy.splitsync.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profiles")
data class UserProfileEntity(
    @PrimaryKey val uid: String,
    val username: String,
    val displayName: String,
    val email: String?,
    val photoUrl: String? = null,
    val defaultCurrency: String = "USD",
    val notifyPush: Boolean = true,
    val notifyEmail: Boolean = true,
    val notifyInvitePush: Boolean = true,
    val notifyInviteEmail: Boolean = true,
    val notifySettlementPush: Boolean = true,
    val notifySettlementEmail: Boolean = true,
    val createdAt: Long,
    val updatedAt: Long,
    val deleted: Boolean = false,
    val syncState: Int = SyncState.SYNCED
)
