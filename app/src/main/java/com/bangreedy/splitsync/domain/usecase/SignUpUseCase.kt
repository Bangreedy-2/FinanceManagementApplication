package com.bangreedy.splitsync.domain.usecase

import com.bangreedy.splitsync.domain.repository.AuthRepository

class SignUpUseCase(private val repo: AuthRepository) {
    suspend operator fun invoke(email: String, password: String) =
        repo.signUp(email.trim(), password)
}
