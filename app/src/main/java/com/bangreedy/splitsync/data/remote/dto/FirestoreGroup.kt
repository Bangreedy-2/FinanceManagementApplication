package com.bangreedy.splitsync.data.remote.dto

data class FirestoreGroup(
    val id: String = "",
    val name: String = "",
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val deleted: Boolean = false,
    val memberUserIds: List<String> = emptyList()
)