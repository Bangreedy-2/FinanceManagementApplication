package com.bangreedy.splitsync.domain.model

data class Invite(
    val inviteId: String,
    val groupId: String,
    val groupName: String,
    val inviterUid: String,
    val inviterDisplayName: String?,
    val status: String, // pending/accepted/declined
    val createdAt: Long
)
