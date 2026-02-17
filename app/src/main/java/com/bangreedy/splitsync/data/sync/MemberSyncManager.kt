package com.bangreedy.splitsync.data.sync

import com.bangreedy.splitsync.data.local.dao.MemberDao
import com.bangreedy.splitsync.data.local.entity.MemberEntity
import com.bangreedy.splitsync.data.local.entity.SyncState
import com.bangreedy.splitsync.data.remote.firestore.FirestoreMemberDataSource
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import android.util.Log

class MemberSyncManager(
    private val remote: FirestoreMemberDataSource,
    private val memberDao: MemberDao
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val listeners = mutableMapOf<String, ListenerRegistration>() // groupId -> reg

    fun startForGroups(groupIds: Set<String>) {
        // add new listeners
        groupIds.forEach { groupId ->
            if (listeners.containsKey(groupId)) return@forEach

            val reg = remote.listenMembers(
                groupId = groupId,
                onChange = { docs ->
                    docs.forEach { doc ->
                        val id = doc.id
                        val displayName = doc.getString("displayName") ?: return@forEach
                        val email = doc.getString("email")
                        val remoteUserId = doc.getString("userId")
                        val createdAt = doc.getLong("createdAt") ?: 0L
                        val updatedAt = doc.getLong("updatedAt") ?: createdAt
                        val deleted = doc.getBoolean("deleted") ?: false

                        scope.launch {
                            memberDao.upsert(
                                MemberEntity(
                                    id = id,
                                    groupId = groupId,
                                    displayName = displayName,
                                    userId = remoteUserId,
                                    email = email,
                                    createdAt = createdAt,
                                    updatedAt = updatedAt,
                                    deleted = deleted,
                                    syncState = SyncState.SYNCED
                                )
                            )
                        }
                    }
                },
                onError = { /* TODO: log */ }
            )

            listeners[groupId] = reg
        }

        // remove old listeners
        val toRemove = listeners.keys - groupIds
        toRemove.forEach { gid ->
            listeners.remove(gid)?.remove()
        }
    }

    suspend fun pushDirtyMembers() {
        val dirty = memberDao.getDirtyMembers(SyncState.DIRTY)
        Log.d("MemberSync", "Dirty members=${dirty.size}")

        for (m in dirty) {
            val data = mapOf(
                "displayName" to m.displayName,
                "email" to m.email,
                "userId" to m.userId,
                "createdAt" to m.createdAt,
                "updatedAt" to m.updatedAt,
                "deleted" to m.deleted
            )

            try {
                Log.d("MemberSync", "Pushing member=${m.id} group=${m.groupId}")
                remote.upsertMember(m.groupId, m.id, data)
                memberDao.setMemberSyncState(m.id, SyncState.SYNCED)
                Log.d("MemberSync", "Pushed OK member=${m.id}")
            } catch (e: Exception) {
                Log.e("MemberSync", "Push FAILED member=${m.id} group=${m.groupId}", e)
            }
        }
    }

    fun stop() {
        listeners.values.forEach { it.remove() }
        listeners.clear()
    }
}
