package com.bangreedy.splitsync.data.repository

import com.bangreedy.splitsync.data.local.dao.NotificationDao
import com.bangreedy.splitsync.data.remote.firestore.FirestoreNotificationDataSource
import com.bangreedy.splitsync.domain.model.AppNotification
import com.bangreedy.splitsync.domain.repository.NotificationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class NotificationRepositoryImpl(
    private val dao: NotificationDao,
    private val remote: FirestoreNotificationDataSource
) : NotificationRepository {

    override fun observeNotifications(uid: String): Flow<List<AppNotification>> =
        dao.observeNotifications(uid).map { list ->
            list.map {
                AppNotification(
                    id = it.id,
                    type = it.type,
                    title = it.title,
                    body = it.body,
                    groupId = it.groupId,
                    actorUid = it.actorUid,
                    createdAt = it.createdAt,
                    read = it.read
                )
            }
        }

    override fun observeUnreadCount(uid: String): Flow<Int> =
        dao.observeUnreadCount(uid)

    override suspend fun markRead(uid: String, id: String) {
        dao.markRead(uid, id)
        remote.markRead(uid, id) // keeps Firestore consistent
    }

    override suspend fun markAllRead(uid: String) {
        dao.markAllRead(uid)
        // optional: batch update Firestore later (MVP skip)
    }

    override suspend fun createNotification(ownerUid: String, notification: AppNotification) {
        val data = mapOf(
            "type" to notification.type,
            "title" to notification.title,
            "body" to notification.body,
            "groupId" to notification.groupId,
            "actorUid" to notification.actorUid,
            "createdAt" to notification.createdAt,
            "read" to notification.read,
            "deleted" to false
        )
        remote.upsertNotification(
            ownerUid = ownerUid,
            notificationId = notification.id,
            data = data
        )
    }
}