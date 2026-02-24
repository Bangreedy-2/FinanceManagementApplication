package com.bangreedy.splitsync.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.bangreedy.splitsync.data.local.entity.GroupEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GroupDao {

    @Query("SELECT * FROM `groups` WHERE deleted = 0 ORDER BY updatedAt DESC")
    fun observeGroups(): Flow<List<GroupEntity>>

    @Query("SELECT * FROM `groups` WHERE id = :groupId LIMIT 1")
    fun observeGroup(groupId: String): Flow<GroupEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(group: GroupEntity)

    @Query("SELECT * FROM `groups` WHERE syncState = :dirtyState AND deleted = 0")
    suspend fun getDirtyGroups(dirtyState: Int): List<GroupEntity>

    @Query("UPDATE `groups` SET syncState = :newState WHERE id = :groupId")
    suspend fun setGroupSyncState(groupId: String, newState: Int)

}
