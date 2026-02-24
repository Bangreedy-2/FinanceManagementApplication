package com.bangreedy.splitsync.domain.model

data class Payment(
    val id: String,
    val groupId: String,
    val fromMemberId: String,
    val toMemberId: String,
    val amountMinor: Long,
    val currency: String,
    val createdAt: Long,
    val updatedAt: Long,
    val deleted: Boolean = false
)
