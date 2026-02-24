package com.bangreedy.splitsync.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.bangreedy.splitsync.data.local.entity.MemberEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MemberDao {

    @Query("SELECT * FROM members WHERE groupId = :groupId AND deleted = 0 ORDER BY displayName ASC")
    fun observeMembers(groupId: String): Flow<List<MemberEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(member: MemberEntity)

    @Query("SELECT * FROM members WHERE syncState = :dirtyState AND deleted = 0")
    suspend fun getDirtyMembers(dirtyState: Int): List<com.bangreedy.splitsync.data.local.entity.MemberEntity>

    @Query("UPDATE members SET syncState = :newState WHERE id = :memberId")
    suspend fun setMemberSyncState(memberId: String, newState: Int)

}
