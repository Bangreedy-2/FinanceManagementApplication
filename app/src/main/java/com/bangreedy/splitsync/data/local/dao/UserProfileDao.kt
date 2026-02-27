package com.bangreedy.splitsync.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.bangreedy.splitsync.data.local.entity.UserProfileEntity

@Dao
interface UserProfileDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(profile: UserProfileEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(profiles: List<UserProfileEntity>)

    @Query("SELECT * FROM user_profiles WHERE syncState = :dirtyState AND deleted = 0")
    suspend fun getDirtyProfiles(dirtyState: Int): List<UserProfileEntity>

    @Query("UPDATE user_profiles SET syncState = :newState WHERE uid = :uid")
    suspend fun setProfileSyncState(uid: String, newState: Int)

    @Query("SELECT * FROM user_profiles WHERE uid = :uid LIMIT 1")
    suspend fun getByUid(uid: String): UserProfileEntity?

    @Query("SELECT * FROM user_profiles WHERE uid IN (:uids)")
    suspend fun getByUids(uids: List<String>): List<UserProfileEntity>
}
