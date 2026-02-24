package com.bangreedy.splitsync.data.repository

import com.bangreedy.splitsync.data.local.dao.FriendDao
import com.bangreedy.splitsync.data.local.dao.UserProfileDao
import com.bangreedy.splitsync.data.remote.firestore.FirestoreDirectThreadDataSource
import com.bangreedy.splitsync.data.remote.firestore.FirestoreFriendsDataSource
import com.bangreedy.splitsync.data.remote.firestore.FirestoreUserLookupDataSource
import com.bangreedy.splitsync.domain.model.Friend
import com.bangreedy.splitsync.domain.model.FriendStatus
import com.bangreedy.splitsync.domain.repository.FriendRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class FriendRepositoryImpl(
    private val friendDao: FriendDao,
    private val remote: FirestoreFriendsDataSource,
    private val userLookup: FirestoreUserLookupDataSource,
    private val directThreadDS: FirestoreDirectThreadDataSource,
    private val userProfileDao: UserProfileDao
) : FriendRepository {

    override fun observeFriends(ownerUid: String): Flow<List<Friend>> =
        friendDao.observeAcceptedFriends(ownerUid).map { entities ->
            entities.map { e ->
                val profile = userProfileDao.getByUid(e.friendUid)
                Friend(
                    friendUid = e.friendUid,
                    status = FriendStatus.fromString(e.status),
                    nickname = e.nickname,
                    displayName = profile?.displayName ?: e.friendUid,
                    username = profile?.username ?: "",
                    photoUrl = profile?.photoUrl,
                    createdAt = e.createdAt,
                    updatedAt = e.updatedAt
                )
            }
        }

    override fun observePendingIncomingCount(ownerUid: String): Flow<Int> =
        friendDao.observePendingIncomingCount(ownerUid)

    override fun observePendingIncoming(ownerUid: String): Flow<List<Friend>> =
        friendDao.observePendingIncoming(ownerUid).map { entities ->
            entities.map { e ->
                val profile = userProfileDao.getByUid(e.friendUid)
                Friend(
                    friendUid = e.friendUid,
                    status = FriendStatus.fromString(e.status),
                    nickname = e.nickname,
                    displayName = profile?.displayName ?: e.friendUid,
                    username = profile?.username ?: "",
                    photoUrl = profile?.photoUrl,
                    createdAt = e.createdAt,
                    updatedAt = e.updatedAt
                )
            }
        }

    override suspend fun sendFriendRequest(myUid: String, inputUsernameOrEmail: String) {
        val input = inputUsernameOrEmail.trim()
        require(input.isNotBlank()) { "Input cannot be blank" }

        // Determine if it's an email or username
        val friendUid = if (input.contains("@")) {
            userLookup.findUidByEmail(input)
        } else {
            userLookup.findUidByUsername(input)
        } ?: throw IllegalArgumentException("User not found: $input")

        require(friendUid != myUid) { "Cannot add yourself as a friend" }

        remote.sendFriendRequest(myUid, friendUid)
    }

    override suspend fun acceptFriendRequest(myUid: String, friendUid: String) {
        remote.acceptFriendRequest(myUid, friendUid)
        // Also ensure the direct thread exists
        directThreadDS.ensureThread(myUid, friendUid)
    }

    override suspend fun declineFriendRequest(myUid: String, friendUid: String) {
        remote.declineFriendRequest(myUid, friendUid)
    }
}

