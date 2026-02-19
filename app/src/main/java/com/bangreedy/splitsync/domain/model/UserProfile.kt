package com.bangreedy.splitsync.domain.model

data class UserProfile(
    val uid: String,
    val username: String,
    val displayName: String,
    val email: String?
)
