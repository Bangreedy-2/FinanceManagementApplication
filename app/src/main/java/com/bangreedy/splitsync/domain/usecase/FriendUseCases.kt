package com.bangreedy.splitsync.domain.usecase

import com.bangreedy.splitsync.domain.model.Friend
import com.bangreedy.splitsync.domain.repository.FriendRepository
import kotlinx.coroutines.flow.Flow

class ObserveFriendsUseCase(private val repo: FriendRepository) {
    operator fun invoke(ownerUid: String): Flow<List<Friend>> =
        repo.observeFriends(ownerUid)
}

class ObservePendingFriendCountUseCase(private val repo: FriendRepository) {
    operator fun invoke(ownerUid: String): Flow<Int> =
        repo.observePendingIncomingCount(ownerUid)
}

class ObservePendingFriendsUseCase(private val repo: FriendRepository) {
    operator fun invoke(ownerUid: String): Flow<List<Friend>> =
        repo.observePendingIncoming(ownerUid)
}

class SendFriendRequestUseCase(private val repo: FriendRepository) {
    suspend operator fun invoke(myUid: String, input: String) =
        repo.sendFriendRequest(myUid, input)
}

class AcceptFriendRequestUseCase(private val repo: FriendRepository) {
    suspend operator fun invoke(myUid: String, friendUid: String) =
        repo.acceptFriendRequest(myUid, friendUid)
}

class DeclineFriendRequestUseCase(private val repo: FriendRepository) {
    suspend operator fun invoke(myUid: String, friendUid: String) =
        repo.declineFriendRequest(myUid, friendUid)
}

