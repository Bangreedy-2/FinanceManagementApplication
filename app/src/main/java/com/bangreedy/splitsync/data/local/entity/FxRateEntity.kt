package com.bangreedy.splitsync.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "fx_rates",
    indices = [
        Index("base", "quote", "rateDate"),
        Index("base", "rateDate")
    ]
)
data class FxRateEntity(
    @PrimaryKey(autoGenerate = false)
    val id: String = "", // base_quote_date format for easy lookup
    val base: String,
    val quote: String,
    val rate: Double,
    val rateDate: String, // YYYY-MM-DD
    val fetchedAt: Long, // Epoch millis
    val provider: String = "fawazahmed0-exchange-api"
)

/**
 * Create a unique ID from components.
 */
fun createFxRateEntityId(base: String, quote: String, rateDate: String): String =
    "${base.uppercase()}_${quote.uppercase()}_$rateDate"

