package com.bangreedy.splitsync.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.bangreedy.splitsync.data.local.entity.FriendEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FriendDao {

    @Query("SELECT * FROM friends WHERE ownerUid = :ownerUid AND status = 'accepted' AND deleted = 0")
    fun observeAcceptedFriends(ownerUid: String): Flow<List<FriendEntity>>

    @Query("SELECT * FROM friends WHERE ownerUid = :ownerUid AND deleted = 0")
    fun observeAllFriends(ownerUid: String): Flow<List<FriendEntity>>

    @Query("SELECT COUNT(*) FROM friends WHERE ownerUid = :ownerUid AND status = 'pending_incoming' AND deleted = 0")
    fun observePendingIncomingCount(ownerUid: String): Flow<Int>

    @Query("SELECT * FROM friends WHERE ownerUid = :ownerUid AND status = 'pending_incoming' AND deleted = 0")
    fun observePendingIncoming(ownerUid: String): Flow<List<FriendEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(friend: FriendEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(friends: List<FriendEntity>)

    @Query("SELECT friendUid FROM friends WHERE ownerUid = :ownerUid AND status = 'accepted' AND deleted = 0")
    suspend fun getAcceptedFriendUids(ownerUid: String): List<String>

    @Query("DELETE FROM friends WHERE ownerUid = :ownerUid")
    suspend fun deleteAllForOwner(ownerUid: String)
}

