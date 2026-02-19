package com.bangreedy.splitsync.domain.usecase

import com.bangreedy.splitsync.domain.model.UserProfile
import com.bangreedy.splitsync.domain.repository.UserProfileRepository
import kotlinx.coroutines.flow.Flow

class ObserveMyProfileUseCase(
    private val repo: UserProfileRepository
) {
    operator fun invoke(uid: String): Flow<UserProfile?> = repo.observeMyProfile(uid)
}
