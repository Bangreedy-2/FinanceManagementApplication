package com.bangreedy.splitsync.data.sync

import com.bangreedy.splitsync.data.local.dao.GroupDao
import com.bangreedy.splitsync.data.local.entity.GroupEntity
import com.bangreedy.splitsync.data.local.entity.SyncState
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await


class GroupSyncManager(
    private val firestore: FirebaseFirestore,
    private val groupDao: GroupDao
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    suspend fun pushDirtyGroups(userId: String) {
        val dirty = groupDao.getDirtyGroups(SyncState.DIRTY)

        for (g in dirty) {
            val data = mapOf(
                "name" to g.name,
                "createdAt" to g.createdAt,
                "updatedAt" to g.updatedAt,
                "deleted" to g.deleted,
                "memberUserIds" to listOf(userId) // IMPORTANT for your query
            )

            firestore.collection("groups")
                .document(g.id)
                .set(data, SetOptions.merge())
                .await()

            groupDao.setGroupSyncState(g.id, SyncState.SYNCED)
        }
    }

    fun start(userId: String) {
        scope.launch {
            firestore.collection("groups")
                .whereArrayContains("memberUserIds", userId)
                .addSnapshotListener { snapshot, _ ->
                    snapshot?.documents?.forEach { doc ->
                        val id = doc.id
                        val name = doc.getString("name") ?: return@forEach
                        val createdAt = doc.getLong("createdAt") ?: 0L
                        val updatedAt = doc.getLong("updatedAt") ?: 0L
                        val deleted = doc.getBoolean("deleted") ?: false

                        scope.launch {
                            groupDao.upsert(
                                GroupEntity(
                                    id = id,
                                    name = name,
                                    createdAt = createdAt,
                                    updatedAt = updatedAt,
                                    deleted = deleted,
                                    syncState = SyncState.SYNCED
                                )
                            )
                        }
                    }
                }
        }
    }
}
