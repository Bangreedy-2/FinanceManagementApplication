package com.bangreedy.splitsync.data.sync

import com.bangreedy.splitsync.data.local.dao.FriendDao
import com.bangreedy.splitsync.data.local.entity.FriendEntity
import com.bangreedy.splitsync.data.local.entity.SyncState
import com.bangreedy.splitsync.data.remote.firestore.FirestoreFriendsDataSource
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class FriendSyncManager(
    private val remote: FirestoreFriendsDataSource,
    private val friendDao: FriendDao,
    private val userProfileSyncManager: UserProfileSyncManager
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var listener: ListenerRegistration? = null
    private var myUid: String? = null

    /** Callback when accepted friends list changes (for DirectThreadSyncManager) */
    var onAcceptedFriendsChanged: ((Set<String>) -> Unit)? = null

    fun start(uid: String) {
        if (listener != null) return
        myUid = uid
        listener = remote.listenFriends(
            uid = uid,
            onChange = { docs -> onFriendsChanged(uid, docs) },
            onError = { /* TODO log */ }
        )
    }

    fun stop() {
        listener?.remove()
        listener = null
    }

    private fun onFriendsChanged(ownerUid: String, docs: List<DocumentSnapshot>) {
        val entities = docs.mapNotNull { doc ->
            val friendUid = doc.getString("friendUid") ?: return@mapNotNull null
            val status = doc.getString("status") ?: return@mapNotNull null
            val nickname = doc.getString("nickname")
            val createdAt = doc.getLong("createdAt") ?: 0L
            val updatedAt = doc.getLong("updatedAt") ?: createdAt

            FriendEntity(
                ownerUid = ownerUid,
                friendUid = friendUid,
                status = status,
                nickname = nickname,
                createdAt = createdAt,
                updatedAt = updatedAt,
                deleted = false,
                syncState = SyncState.SYNCED
            )
        }

        scope.launch {
            friendDao.deleteAllForOwner(ownerUid)
            if (entities.isNotEmpty()) {
                friendDao.upsertAll(entities)
            }

            // Sync profiles for ALL friends (so their photos/names are available locally)
            val allFriendUids = entities.map { it.friendUid }
            if (allFriendUids.isNotEmpty()) {
                userProfileSyncManager.refreshUids(allFriendUids)
            }

            // Notify about accepted friends so DirectThreadSyncManager can start listeners
            val acceptedUids = entities
                .filter { it.status == "accepted" }
                .map { it.friendUid }
                .toSet()
            onAcceptedFriendsChanged?.invoke(acceptedUids)
        }
    }
}



