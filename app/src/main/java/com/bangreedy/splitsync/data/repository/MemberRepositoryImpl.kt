package com.bangreedy.splitsync.data.repository

import com.bangreedy.splitsync.data.local.dao.GroupMemberDao
import com.bangreedy.splitsync.data.local.dao.MemberJoinedRow
import com.bangreedy.splitsync.data.mapper.toDomain
import com.bangreedy.splitsync.domain.model.Member
import com.bangreedy.splitsync.domain.repository.MemberRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class MemberRepositoryImpl(
    private val groupMemberDao: GroupMemberDao
) : MemberRepository {

    override fun observeMembers(groupId: String): Flow<List<Member>> =
        groupMemberDao.observeMembersUi(groupId)
            .map { rows -> rows.map(MemberJoinedRow::toDomain) }
}
