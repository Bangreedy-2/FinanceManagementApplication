package com.bangreedy.splitsync.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.bangreedy.splitsync.data.local.dao.ExpenseDao
import com.bangreedy.splitsync.data.local.dao.GroupDao
import com.bangreedy.splitsync.data.local.dao.MemberDao
import com.bangreedy.splitsync.data.local.dao.PaymentDao
import com.bangreedy.splitsync.data.local.entity.ExpenseEntity
import com.bangreedy.splitsync.data.local.entity.ExpenseSplitEntity
import com.bangreedy.splitsync.data.local.entity.GroupEntity
import com.bangreedy.splitsync.data.local.entity.MemberEntity
import com.bangreedy.splitsync.data.local.entity.PaymentEntity

@Database(
    entities = [
        GroupEntity::class,
        MemberEntity::class,
        ExpenseEntity::class,
        ExpenseSplitEntity::class,
        PaymentEntity::class
    ],
    version = 4,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun groupDao(): GroupDao
    abstract fun memberDao(): MemberDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun paymentDao(): PaymentDao

}
