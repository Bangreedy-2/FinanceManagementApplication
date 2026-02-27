package com.bangreedy.splitsync.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.bangreedy.splitsync.data.local.entity.FxRateEntity

@Dao
interface FxRateDao {

    @Query("SELECT * FROM fx_rates WHERE base = :base AND quote = :quote AND rateDate = :rateDate LIMIT 1")
    suspend fun getRate(base: String, quote: String, rateDate: String): FxRateEntity?

    @Query("""
        SELECT * FROM fx_rates 
        WHERE base = :base AND quote = :quote AND rateDate <= :rateDate 
        ORDER BY rateDate DESC 
        LIMIT 1
    """)
    suspend fun getLatestRateBeforeOrOn(base: String, quote: String, rateDate: String): FxRateEntity?

    @Query("SELECT MAX(fetchedAt) FROM fx_rates WHERE base = :base AND rateDate = :rateDate")
    suspend fun getLastFetchedAt(base: String, rateDate: String): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(rate: FxRateEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(rates: List<FxRateEntity>)

    @Query("DELETE FROM fx_rates WHERE rateDate < :rateDate")
    suspend fun deleteOlderThan(rateDate: String)

    @Query("DELETE FROM fx_rates")
    suspend fun deleteAll()
}

