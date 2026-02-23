package com.bangreedy.splitsync.domain.model

data class AppNotification(
    val id: String,
    val type: String,
    val title: String,
    val body: String,
    val groupId: String?,
    val actorUid: String?,
    val createdAt: Long,
    val read: Boolean
)