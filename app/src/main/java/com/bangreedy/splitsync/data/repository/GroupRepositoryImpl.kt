package com.bangreedy.splitsync.data.repository

import android.net.Uri
import com.bangreedy.splitsync.data.local.dao.GroupDao
import com.bangreedy.splitsync.data.local.entity.GroupEntity
import com.bangreedy.splitsync.data.local.entity.SyncState
import com.bangreedy.splitsync.data.mapper.toDomain
import com.bangreedy.splitsync.data.mapper.toEntity
import com.bangreedy.splitsync.domain.model.Group
import com.bangreedy.splitsync.domain.repository.GroupRepository
import com.bangreedy.splitsync.domain.repository.StorageRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class GroupRepositoryImpl(
    private val groupDao: GroupDao,
    private val storageRepository: StorageRepository
) : GroupRepository {

    override fun observeGroups(): Flow<List<Group>> =
        groupDao.observeGroups().map { list -> list.map { it.toDomain() } }

    override fun observeGroup(groupId: String) =
        groupDao.observeGroup(groupId).map { it?.toDomain() }

    override suspend fun createGroup(name: String): String {
        require(name.isNotBlank()) { "Group name cannot be blank" }

        val now = System.currentTimeMillis()
        val id = UUID.randomUUID().toString()

        groupDao.upsert(
            GroupEntity(
                id = id,
                name = name,
                photoUrl = null,
                createdAt = now,
                updatedAt = now,
                deleted = false,
                syncState = SyncState.DIRTY
            )
        )
        return id
    }

    override suspend fun uploadGroupPhoto(groupId: String, uri: Uri): String {
        return storageRepository.uploadGroupPhoto(groupId, uri)
    }

    override suspend fun updateGroup(group: Group) {
        // Set sync state dirty so SyncManager picks it up
        val updatedGroup = group.copy(updatedAt = System.currentTimeMillis())
        groupDao.upsert(updatedGroup.toEntity(syncState = SyncState.DIRTY))
    }
}
