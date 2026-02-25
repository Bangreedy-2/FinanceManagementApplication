package com.bangreedy.splitsync.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "payments",
    indices = [Index("groupId"), Index("fromMemberId"), Index("toMemberId"), Index("contextType", "contextId")]
)
data class PaymentEntity(
    @PrimaryKey val id: String,
    val groupId: String,
    val fromMemberId: String,
    val toMemberId: String,
    val amountMinor: Long,
    val currency: String,
    val createdAt: Long,
    val updatedAt: Long,
    val deleted: Boolean,
    val syncState: Int,
    val contextType: String = "GROUP",
    val contextId: String = groupId,
    val mode: String = "ONE_CURRENCY",
    val breakdownJson: String? = null,
    val ratesLastUpdatedAt: Long? = null,
    val asOfDate: String? = null,
    val settlementId: String? = null
)
