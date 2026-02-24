package com.bangreedy.splitsync.domain.usecase

import com.bangreedy.splitsync.domain.model.Member
import com.bangreedy.splitsync.domain.repository.MemberRepository
import kotlinx.coroutines.flow.Flow

class ObserveMembersUseCase(
    private val repo: MemberRepository
) {
    operator fun invoke(groupId: String): Flow<List<Member>> = repo.observeMembers(groupId)
}
