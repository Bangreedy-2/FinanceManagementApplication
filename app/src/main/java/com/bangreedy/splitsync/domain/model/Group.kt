package com.bangreedy.splitsync.domain.model

data class Group(
    val id: String,
    val name: String,
    val photoUrl: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val deleted: Boolean = false
)