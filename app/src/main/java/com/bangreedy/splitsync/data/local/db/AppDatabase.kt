package com.bangreedy.splitsync.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.bangreedy.splitsync.data.local.dao.GroupDao
import com.bangreedy.splitsync.data.local.dao.MemberDao
import com.bangreedy.splitsync.data.local.entity.GroupEntity
import com.bangreedy.splitsync.data.local.entity.MemberEntity

@Database(
    entities = [GroupEntity::class, MemberEntity::class],
    version = 2,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun groupDao(): GroupDao
    abstract fun memberDao(): MemberDao
}
