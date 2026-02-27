package com.bangreedy.splitsync.domain.repository

import com.bangreedy.splitsync.domain.model.Friend
import kotlinx.coroutines.flow.Flow

interface FriendRepository {
    fun observeFriends(ownerUid: String): Flow<List<Friend>>
    fun observePendingIncomingCount(ownerUid: String): Flow<Int>
    fun observePendingIncoming(ownerUid: String): Flow<List<Friend>>
    suspend fun sendFriendRequest(myUid: String, inputUsernameOrEmail: String)
    suspend fun acceptFriendRequest(myUid: String, friendUid: String)
    suspend fun declineFriendRequest(myUid: String, friendUid: String)
}

