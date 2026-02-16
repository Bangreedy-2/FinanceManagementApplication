package com.bangreedy.splitsync.data.remote.dto

data class FirestoreMember(
    val id: String = "",
    val groupId: String = "",
    val displayName: String = "",
    val userId: String? = null,
    val email: String? = null,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val deleted: Boolean = false
)