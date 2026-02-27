package com.bangreedy.splitsync.data.local.dao

data class MemberJoinedRow(
    val uid: String,
    val groupId: String,
    val displayName: String,
    val username: String,
    val email: String?,
    val photoUrl: String?
)
