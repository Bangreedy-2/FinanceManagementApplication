package com.bangreedy.splitsync.domain.model

data class Member(
    val id: String,
    val groupId: String,
    val displayName: String,
    val userId: String? = null,
    val email: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val deleted: Boolean = false
)
