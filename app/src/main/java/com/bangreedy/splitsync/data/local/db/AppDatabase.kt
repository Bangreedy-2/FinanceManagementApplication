package com.bangreedy.splitsync.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.bangreedy.splitsync.data.local.dao.ExpenseDao
import com.bangreedy.splitsync.data.local.dao.GroupDao
import com.bangreedy.splitsync.data.local.dao.GroupMemberDao
import com.bangreedy.splitsync.data.local.dao.PaymentDao
import com.bangreedy.splitsync.data.local.dao.UserProfileDao
import com.bangreedy.splitsync.data.local.dao.FxRateDao
import com.bangreedy.splitsync.data.local.entity.ExpenseEntity
import com.bangreedy.splitsync.data.local.entity.ExpenseSplitEntity
import com.bangreedy.splitsync.data.local.entity.GroupEntity
import com.bangreedy.splitsync.data.local.entity.GroupMemberEntity
import com.bangreedy.splitsync.data.local.entity.PaymentEntity
import com.bangreedy.splitsync.data.local.entity.UserProfileEntity
import com.bangreedy.splitsync.data.local.entity.NotificationEntity
import com.bangreedy.splitsync.data.local.entity.FxRateEntity
import com.bangreedy.splitsync.data.local.dao.NotificationDao

@Database(
    entities = [
        GroupEntity::class,
        GroupMemberEntity::class,
        UserProfileEntity::class,
        ExpenseEntity::class,
        ExpenseSplitEntity::class,
        PaymentEntity::class,
        NotificationEntity::class,
        FxRateEntity::class
    ],
    version = 10,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun groupDao(): GroupDao
    abstract fun groupMemberDao(): GroupMemberDao
    abstract fun userProfileDao(): UserProfileDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun paymentDao(): PaymentDao
    abstract fun notificationDao(): NotificationDao
    abstract fun fxRateDao(): FxRateDao
}