package com.bangreedy.splitsync.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "members")
data class MemberEntity(
    @PrimaryKey val id: String,
    val groupId: String,
    val displayName: String,
    val userId: String?,
    val email: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val deleted: Boolean,
    val syncState: Int
)
