package com.bangreedy.splitsync.domain.usecase

import com.bangreedy.splitsync.domain.repository.AuthRepository

class SignInUseCase(private val repo: AuthRepository) {
    suspend operator fun invoke(email: String, password: String) =
        repo.signIn(email.trim(), password)
}
