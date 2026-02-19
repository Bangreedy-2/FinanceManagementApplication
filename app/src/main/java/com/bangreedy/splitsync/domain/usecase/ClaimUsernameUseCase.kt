package com.bangreedy.splitsync.domain.usecase

import com.bangreedy.splitsync.domain.repository.UserProfileRepository

class ClaimUsernameUseCase(
    private val repo: UserProfileRepository
) {
    suspend operator fun invoke(uid: String, username: String, displayName: String, email: String?) {
        repo.claimUsername(uid, username, displayName, email)
    }
}
