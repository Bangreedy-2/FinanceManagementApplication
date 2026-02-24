package com.bangreedy.splitsync.data.sync

import com.bangreedy.splitsync.data.local.dao.GroupDao
import com.bangreedy.splitsync.data.local.entity.GroupEntity
import com.bangreedy.splitsync.data.local.entity.SyncState
import com.bangreedy.splitsync.data.remote.firestore.FirestoreGroupDataSource
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class GroupSyncManager(
    private val remote: FirestoreGroupDataSource,
    private val groupDao: GroupDao
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var groupsListener: ListenerRegistration? = null

    fun start(userId: String, onGroupIdsChanged: (Set<String>) -> Unit) {
        groupsListener?.remove()

        groupsListener = remote.listenGroupsForUser(
            userId = userId,
            onChange = { docs ->
                val groupIds = docs.map { it.id }.toSet()
                onGroupIdsChanged(groupIds)

                docs.forEach { doc ->
                    val id = doc.id
                    val name = doc.getString("name") ?: return@forEach
                    val photoUrl = doc.getString("photoUrl")
                    val createdAt = doc.getLong("createdAt") ?: 0L
                    val updatedAt = doc.getLong("updatedAt") ?: createdAt
                    val deleted = doc.getBoolean("deleted") ?: false

                    scope.launch {
                        groupDao.upsert(
                            GroupEntity(
                                id = id,
                                name = name,
                                photoUrl = photoUrl,
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
    }


    suspend fun pushDirtyGroups(userId: String) {
        val dirty = groupDao.getDirtyGroups(SyncState.DIRTY)

        for (g in dirty) {
            val data = mapOf(
                "name" to g.name,
                "photoUrl" to g.photoUrl,
                "createdAt" to g.createdAt,
                "updatedAt" to g.updatedAt,
                "deleted" to g.deleted,
                "memberUserIds" to listOf(userId)
            )

            remote.upsertGroup(g.id, data)

            // ✅ ensure creator exists as a member document
            remote.upsertGroupMember(
                groupId = g.id,
                uid = userId,
                data = mapOf(
                    "uid" to userId,
                    "role" to "owner",
                    "joinedAt" to System.currentTimeMillis(),
                    "deleted" to false
                )
            )

            groupDao.setGroupSyncState(g.id, SyncState.SYNCED)
        }
    }


    fun stop() {
        groupsListener?.remove()
        groupsListener = null
    }
}
