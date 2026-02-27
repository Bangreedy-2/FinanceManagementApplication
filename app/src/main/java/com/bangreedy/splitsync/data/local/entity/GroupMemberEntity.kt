package com.bangreedy.splitsync.data.local.entity

import androidx.room.Entity

@Entity(
    tableName = "group_members",
    primaryKeys = ["groupId", "uid"]
)
data class GroupMemberEntity(
    val groupId: String,
    val uid: String,
    val role: String = "member",
    val joinedAt: Long = 0L,
    val deleted: Boolean = false,
    val syncState: Int = SyncState.SYNCED
)
