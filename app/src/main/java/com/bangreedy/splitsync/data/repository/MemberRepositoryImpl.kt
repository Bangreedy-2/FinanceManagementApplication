package com.bangreedy.splitsync.data.repository

import com.bangreedy.splitsync.data.local.dao.MemberDao
import com.bangreedy.splitsync.data.local.entity.MemberEntity
import com.bangreedy.splitsync.data.local.entity.SyncState
import com.bangreedy.splitsync.data.mapper.toDomain
import com.bangreedy.splitsync.domain.model.Member
import com.bangreedy.splitsync.domain.repository.MemberRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class MemberRepositoryImpl(
    private val memberDao: MemberDao
) : MemberRepository {

    override fun observeMembers(groupId: String): Flow<List<Member>> =
        memberDao.observeMembers(groupId).map { it.map { e -> e.toDomain() } }

    override suspend fun addMember(groupId: String, displayName: String, email: String?): String {
        require(groupId.isNotBlank()) { "groupId required" }
        require(displayName.isNotBlank()) { "Member name cannot be blank" }

        val now = System.currentTimeMillis()
        val id = UUID.randomUUID().toString()

        memberDao.upsert(
            MemberEntity(
                id = id,
                groupId = groupId,
                displayName = displayName,
                userId = null,
                email = email,
                createdAt = now,
                updatedAt = now,
                deleted = false,
                syncState = SyncState.DIRTY
            )
        )
        return id
    }
}
