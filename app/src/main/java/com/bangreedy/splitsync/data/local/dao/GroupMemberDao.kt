package com.bangreedy.splitsync.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.bangreedy.splitsync.data.local.entity.GroupMemberEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GroupMemberDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(member: GroupMemberEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(members: List<GroupMemberEntity>)

    @Query("SELECT * FROM group_members WHERE syncState = :dirtyState AND deleted = 0")
    suspend fun getDirtyGroupMembers(dirtyState: Int): List<GroupMemberEntity>

    @Query("UPDATE group_members SET syncState = :newState WHERE groupId = :groupId AND uid = :uid")
    suspend fun setGroupMemberSyncState(groupId: String, uid: String, newState: Int)


    @Transaction
    @Query("""
    SELECT 
        gm.uid as uid,
        gm.groupId as groupId,
        up.displayName as displayName,
        up.username as username,
        up.email as email,
        up.photoUrl as photoUrl
    FROM group_members gm
    JOIN user_profiles up ON up.uid = gm.uid
    WHERE gm.groupId = :groupId AND gm.deleted = 0
    ORDER BY up.displayName COLLATE NOCASE ASC
""")
    fun observeMembersUi(groupId: String): Flow<List<MemberJoinedRow>>
    @Query("SELECT uid FROM group_members WHERE groupId = :groupId AND deleted = 0")
    suspend fun getMemberUidsOnce(groupId: String): List<String>

}
