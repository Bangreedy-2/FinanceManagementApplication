package com.bangreedy.splitsync.domain.usecase

import com.bangreedy.splitsync.domain.repository.AuthRepository
import com.bangreedy.splitsync.domain.repository.AuthState
import kotlinx.coroutines.flow.Flow

class ObserveAuthStateUseCase(
    private val repo: AuthRepository
) {
    operator fun invoke(): Flow<AuthState> = repo.authState
}
