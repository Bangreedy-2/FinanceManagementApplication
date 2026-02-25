package com.bangreedy.splitsync.data.repository

import com.bangreedy.splitsync.data.local.dao.PaymentDao
import com.bangreedy.splitsync.data.local.entity.PaymentEntity
import com.bangreedy.splitsync.data.local.entity.SyncState
import com.bangreedy.splitsync.domain.model.Payment
import com.bangreedy.splitsync.domain.repository.PaymentRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONObject

class PaymentRepositoryImpl(
    private val dao: PaymentDao
) : PaymentRepository {

    override fun observePayments(groupId: String): Flow<List<Payment>> =
        dao.observePayments(groupId).map { list ->
            list.map { it.toDomain() }
        }

    override suspend fun createPayment(payment: Payment): String {
        dao.upsert(payment.toEntity(syncState = SyncState.DIRTY))
        return payment.id
    }
}

private fun PaymentEntity.toDomain(): Payment =
    Payment(
        id = id,
        groupId = groupId,
        fromMemberId = fromMemberId,
        toMemberId = toMemberId,
        amountMinor = amountMinor,
        currency = currency,
        createdAt = createdAt,
        updatedAt = updatedAt,
        deleted = deleted,
        contextType = contextType,
        contextId = contextId,
        mode = mode,
        breakdownByCurrency = breakdownJson?.let { parseBreakdownJson(it) },
        ratesLastUpdatedAt = ratesLastUpdatedAt,
        asOfDate = asOfDate,
        settlementId = settlementId
    )

private fun Payment.toEntity(syncState: Int): PaymentEntity =
    PaymentEntity(
        id = id,
        groupId = groupId,
        fromMemberId = fromMemberId,
        toMemberId = toMemberId,
        amountMinor = amountMinor,
        currency = currency,
        createdAt = createdAt,
        updatedAt = updatedAt,
        deleted = deleted,
        syncState = syncState,
        contextType = contextType,
        contextId = contextId,
        mode = mode,
        breakdownJson = breakdownByCurrency?.let { toBreakdownJson(it) },
        ratesLastUpdatedAt = ratesLastUpdatedAt,
        asOfDate = asOfDate,
        settlementId = settlementId
    )

private fun parseBreakdownJson(json: String): Map<String, Long> {
    return try {
        val obj = JSONObject(json)
        val map = mutableMapOf<String, Long>()
        val keys: Iterator<String> = obj.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            map[key] = obj.getLong(key)
        }
        map
    } catch (_: Throwable) { emptyMap() }
}

private fun toBreakdownJson(map: Map<String, Long>): String {
    val obj = JSONObject()
    map.forEach { (k, v) -> obj.put(k, v) }
    return obj.toString()
}

