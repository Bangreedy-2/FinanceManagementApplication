package com.bangreedy.splitsync.domain.usecase

import com.bangreedy.splitsync.domain.repository.InviteRepository

class SendInviteUseCase(private val repo: InviteRepository) {
    suspend operator fun invoke(
        groupId: String,
        groupName: String,
        inviterUid: String,
        inviterDisplayName: String?,
        input: String
    ) = repo.sendInviteByUsernameOrEmail(groupId, groupName, inviterUid, inviterDisplayName, input)
}
