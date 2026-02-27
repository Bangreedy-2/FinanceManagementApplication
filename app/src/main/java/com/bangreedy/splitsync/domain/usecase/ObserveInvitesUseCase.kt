package com.bangreedy.splitsync.domain.usecase

import com.bangreedy.splitsync.domain.model.Invite
import com.bangreedy.splitsync.domain.repository.InviteRepository
import kotlinx.coroutines.flow.Flow

class ObserveInvitesUseCase(private val repo: InviteRepository) {
    operator fun invoke(myUid: String): Flow<List<Invite>> = repo.observeMyInvites(myUid)
}
