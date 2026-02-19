package com.bangreedy.splitsync.domain.usecase

import com.bangreedy.splitsync.domain.repository.InviteRepository

class DeclineInviteUseCase(private val repo: InviteRepository) {
    suspend operator fun invoke(myUid: String, inviteId: String) =
        repo.declineInvite(myUid, inviteId)
}
