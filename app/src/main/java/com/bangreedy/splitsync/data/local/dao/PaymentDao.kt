package com.bangreedy.splitsync.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.bangreedy.splitsync.data.local.entity.PaymentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PaymentDao {

    @Query("SELECT * FROM payments WHERE groupId = :groupId AND deleted = 0")
    fun observePayments(groupId: String): Flow<List<PaymentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(payment: PaymentEntity)

    @Query("SELECT * FROM payments WHERE syncState = :dirtyState AND deleted = 0")
    suspend fun getDirtyPayments(dirtyState: Int): List<PaymentEntity>

    @Query("UPDATE payments SET syncState = :newState WHERE id = :paymentId")
    suspend fun setPaymentSyncState(paymentId: String, newState: Int)
}
