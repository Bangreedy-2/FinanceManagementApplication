package com.bangreedy.splitsync.data.sync

import com.bangreedy.splitsync.data.local.dao.NotificationDao
import com.bangreedy.splitsync.data.local.entity.NotificationEntity
import com.bangreedy.splitsync.data.local.entity.SyncState
import com.bangreedy.splitsync.data.remote.firestore.FirestoreNotificationDataSource
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class NotificationSyncManager(
    private val remote: FirestoreNotificationDataSource,
    private val dao: NotificationDao
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var listener: ListenerRegistration? = null

    fun start(uid: String) {
        listener?.remove()
        listener = remote.listenNotifications(
            uid = uid,
            onChange = { docs -> onChanged(uid, docs) },
            onError = { /* log if you want */ }
        )
    }

    fun stop() {
        listener?.remove()
        listener = null
    }

    private fun onChanged(ownerUid: String, docs: List<DocumentSnapshot>) {
        val entities = docs.map { d ->
            NotificationEntity(
                id = d.id,
                ownerUid = ownerUid,
                type = d.getString("type") ?: "unknown",
                title = d.getString("title") ?: "",
                body = d.getString("body") ?: "",
                groupId = d.getString("groupId"),
                actorUid = d.getString("actorUid"),
                createdAt = d.getLong("createdAt") ?: 0L,
                read = d.getBoolean("read") ?: false,
                deleted = d.getBoolean("deleted") ?: false,
                syncState = SyncState.SYNCED
            )
        }

        scope.launch {
            dao.upsertAll(entities)
        }
    }
}