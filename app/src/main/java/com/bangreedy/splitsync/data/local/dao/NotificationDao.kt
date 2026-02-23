package com.bangreedy.splitsync.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.bangreedy.splitsync.data.local.entity.NotificationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationDao {

    @Query("""
        SELECT * FROM notifications
        WHERE ownerUid = :uid AND deleted = 0
        ORDER BY createdAt DESC
    """)
    fun observeNotifications(uid: String): Flow<List<NotificationEntity>>

    @Query("""
        SELECT COUNT(*) FROM notifications
        WHERE ownerUid = :uid AND deleted = 0 AND `read` = 0
    """)
    fun observeUnreadCount(uid: String): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<NotificationEntity>)

    @Query("UPDATE notifications SET `read` = 1 WHERE ownerUid = :uid AND id = :id")
    suspend fun markRead(uid: String, id: String)

    @Query("UPDATE notifications SET `read` = 1 WHERE ownerUid = :uid")
    suspend fun markAllRead(uid: String)
}