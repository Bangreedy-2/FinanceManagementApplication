package com.bangreedy.splitsync.data.sync

import com.bangreedy.splitsync.data.local.dao.GroupMemberDao
import com.bangreedy.splitsync.data.local.entity.GroupMemberEntity
import com.bangreedy.splitsync.data.local.entity.SyncState
import com.bangreedy.splitsync.data.remote.firestore.FirestoreGroupMemberDataSource
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class GroupMemberSyncManager(
    private val remote: FirestoreGroupMemberDataSource,
    private val groupMemberDao: GroupMemberDao
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val memberListeners = mutableMapOf<String, ListenerRegistration>() // groupId -> reg

    fun onGroupsChanged(groupIds: Set<String>) {
        // start new listeners
        groupIds.forEach { groupId ->
            if (memberListeners.containsKey(groupId)) return@forEach

            val reg = remote.listenGroupMembers(
                groupId = groupId,
                onChange = { docs -> onMembersChanged(groupId, docs) },
                onError = { /* TODO log */ }
            )
            memberListeners[groupId] = reg
        }

        // remove stale listeners
        val toRemove = memberListeners.keys - groupIds
        toRemove.forEach { gid ->
            memberListeners.remove(gid)?.remove()
        }
    }

    fun stop() {
        memberListeners.values.forEach { it.remove() }
        memberListeners.clear()
    }

    private fun onMembersChanged(groupId: String, docs: List<DocumentSnapshot>) {
        val entities = docs.mapNotNull { doc ->
            val uid = doc.getString("uid") ?: doc.id
            val role = doc.getString("role") ?: "member"
            val joinedAt = doc.getLong("joinedAt") ?: 0L
            val deleted = doc.getBoolean("deleted") ?: false

            GroupMemberEntity(
                groupId = groupId,
                uid = uid,
                role = role,
                joinedAt = joinedAt,
                deleted = deleted,
                syncState = SyncState.SYNCED
            )
        }

        scope.launch {
            groupMemberDao.upsertAll(entities)
        }
    }
}
