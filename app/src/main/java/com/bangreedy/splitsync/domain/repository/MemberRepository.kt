package com.bangreedy.splitsync.domain.repository

import com.bangreedy.splitsync.domain.model.Member
import kotlinx.coroutines.flow.Flow

interface MemberRepository {
    fun observeMembers(groupId: String): Flow<List<Member>>
    suspend fun addMember(groupId: String, displayName: String, email: String? = null): String
}
