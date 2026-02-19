package com.bangreedy.splitsync.domain.usecase

import com.bangreedy.splitsync.domain.repository.UserProfileRepository

class CheckUsernameAvailabilityUseCase(
    private val repo: UserProfileRepository
) {
    suspend operator fun invoke(username: String): Boolean =
        repo.isUsernameAvailable(username)
}
