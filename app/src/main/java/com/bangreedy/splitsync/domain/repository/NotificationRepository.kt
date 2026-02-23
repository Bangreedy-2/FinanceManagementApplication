package com.bangreedy.splitsync.domain.repository

import com.bangreedy.splitsync.domain.model.AppNotification
import kotlinx.coroutines.flow.Flow

interface NotificationRepository {
    fun observeNotifications(uid: String): Flow<List<AppNotification>>
    fun observeUnreadCount(uid: String): Flow<Int>
    suspend fun markRead(uid: String, id: String)
    suspend fun markAllRead(uid: String)

    /**
     * Creates (or upserts) a notification for [ownerUid]. Source of truth is Firestore; Room is filled by sync.
     */
    suspend fun createNotification(ownerUid: String, notification: AppNotification)
}