package com.bangreedy.splitsync.domain.repository

import com.bangreedy.splitsync.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow

interface UserProfileRepository {
    fun observeMyProfile(uid: String): Flow<UserProfile?>
    suspend fun claimUsername(uid: String, username: String, displayName: String, email: String?)
    suspend fun isUsernameAvailable(username: String): Boolean
}
