package com.bangreedy.splitsync.domain.usecase

import com.bangreedy.splitsync.domain.model.FriendActivity
import com.bangreedy.splitsync.domain.repository.FriendActivityRepository
import kotlinx.coroutines.flow.Flow

class ObserveFriendActivityUseCase(private val repo: FriendActivityRepository) {
    operator fun invoke(myUid: String, friendUid: String): Flow<FriendActivity> =
        repo.observeFriendActivity(myUid, friendUid)
}

