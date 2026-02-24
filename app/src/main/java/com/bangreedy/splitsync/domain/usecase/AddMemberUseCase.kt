package com.bangreedy.splitsync.domain.usecase

import com.bangreedy.splitsync.domain.repository.MemberRepository

class AddMemberUseCase(
    private val repo: MemberRepository
) {
    suspend operator fun invoke(groupId: String, displayName: String, email: String? = null): String =
        repo.addMember(groupId, displayName.trim(), email?.trim())
}
