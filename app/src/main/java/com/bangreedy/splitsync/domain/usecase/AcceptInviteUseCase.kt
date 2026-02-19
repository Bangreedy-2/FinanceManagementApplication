package com.bangreedy.splitsync.domain.usecase

import com.bangreedy.splitsync.domain.repository.InviteRepository

class AcceptInviteUseCase(private val repo: InviteRepository) {
    suspend operator fun invoke(myUid: String, inviteId: String, groupId: String) =
        repo.acceptInvite(myUid, inviteId, groupId)
}
