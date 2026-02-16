package com.bangreedy.splitsync.data.repository

import com.bangreedy.splitsync.data.local.dao.GroupDao
import com.bangreedy.splitsync.data.local.entity.GroupEntity
import com.bangreedy.splitsync.data.local.entity.SyncState
import com.bangreedy.splitsync.data.mapper.toDomain
import com.bangreedy.splitsync.domain.model.Group
import com.bangreedy.splitsync.domain.repository.GroupRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class GroupRepositoryImpl(
    private val groupDao: GroupDao
) : GroupRepository {

    override fun observeGroups(): Flow<List<Group>> =
        groupDao.observeGroups().map { list -> list.map { it.toDomain() } }

    override suspend fun createGroup(name: String): String {
        require(name.isNotBlank()) { "Group name cannot be blank" }

        val now = System.currentTimeMillis()
        val id = UUID.randomUUID().toString()

        groupDao.upsert(
            GroupEntity(
                id = id,
                name = name,
                createdAt = now,
                updatedAt = now,
                deleted = false,
                syncState = SyncState.DIRTY
            )
        )
        return id
    }
}
