package com.bangreedy.splitsync.domain.model

data class NotificationPrefs(
    val pushEnabled: Boolean = true,
    val emailEnabled: Boolean = true,
    val invitePush: Boolean = true,
    val inviteEmail: Boolean = true,
    val settlementPush: Boolean = true,
    val settlementEmail: Boolean = true
)

data class UserProfile(
    val uid: String,
    val username: String,
    val displayName: String,
    val email: String?,
    val photoUrl: String? = null,
    val defaultCurrency: String = "USD",
    val notificationPrefs: NotificationPrefs = NotificationPrefs()
)
