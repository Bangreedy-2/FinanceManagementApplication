package com.bangreedy.splitsync.domain.repository

import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val authState: Flow<AuthState>

    suspend fun signUp(email: String, password: String)
    suspend fun signIn(email: String, password: String)
    suspend fun signOut()
}

sealed class AuthState {
    data object SignedOut : AuthState()
    data class SignedIn(val userId: String, val email: String?) : AuthState()
}
