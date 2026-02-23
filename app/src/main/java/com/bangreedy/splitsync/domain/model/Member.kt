package com.bangreedy.splitsync.domain.model

data class Member(
    val uid: String,
    val groupId: String,
    val displayName: String,
    val username: String,
    val email: String?,
    val photoUrl: String? = null
)
