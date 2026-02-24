package com.bangreedy.splitsync.domain.usecase

import com.bangreedy.splitsync.domain.repository.AuthRepository

class SignOutUseCase(private val repo: AuthRepository) {
    suspend operator fun invoke() = repo.signOut()
}
