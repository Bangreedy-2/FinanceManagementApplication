package com.bangreedy.splitsync.data.repository

import com.bangreedy.splitsync.data.remote.firestore.auth.FirebaseAuthDataSource
import com.bangreedy.splitsync.domain.repository.AuthRepository
import com.bangreedy.splitsync.domain.repository.AuthState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AuthRepositoryImpl(
    private val ds: FirebaseAuthDataSource
) : AuthRepository {

    override val authState: Flow<AuthState> =
        ds.observeUser().map { user ->
            if (user == null) AuthState.SignedOut
            else AuthState.SignedIn(userId = user.uid, email = user.email)
        }

    override suspend fun signUp(email: String, password: String) {
        validate(email, password)
        ds.signUp(email, password)
    }

    override suspend fun signIn(email: String, password: String) {
        validate(email, password)
        ds.signIn(email, password)
    }

    override suspend fun signOut() {
        ds.signOut()
    }

    private fun validate(email: String, password: String) {
        require(email.isNotBlank()) { "Email required" }
        require(password.length >= 6) { "Password must be at least 6 characters" }
    }
}
