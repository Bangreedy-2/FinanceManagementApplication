package com.bangreedy.splitsync.data.repository

import com.bangreedy.splitsync.data.local.dao.ExpenseDao
import com.bangreedy.splitsync.data.local.entity.ExpenseEntity
import com.bangreedy.splitsync.data.local.entity.ExpenseSplitEntity
import com.bangreedy.splitsync.data.local.entity.SyncState
import com.bangreedy.splitsync.data.remote.firestore.FirestoreDirectThreadDataSource
import com.bangreedy.splitsync.data.local.dao.PaymentDao
import com.bangreedy.splitsync.data.local.entity.PaymentEntity
import com.bangreedy.splitsync.domain.repository.DirectThreadRepository
import java.util.UUID

class DirectThreadRepositoryImpl(
    private val directThreadDS: FirestoreDirectThreadDataSource,
    private val expenseDao: ExpenseDao,
    private val paymentDao: PaymentDao
) : DirectThreadRepository {

    override suspend fun ensureThread(uidA: String, uidB: String): String {
        return directThreadDS.ensureThread(uidA, uidB)
    }

    override suspend fun createDirectExpense(
        threadId: String,
        payerUid: String,
        amountMinor: Long,
        currency: String,
        note: String?,
        splitUids: List<String>
    ): String {
        require(amountMinor > 0) { "Amount must be > 0" }
        require(splitUids.isNotEmpty()) { "Must have at least one split participant" }

        val expenseId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val perPerson = amountMinor / splitUids.size
        val remainder = amountMinor - (perPerson * splitUids.size)

        val splits = splitUids.mapIndexed { index, uid ->
            val owed = if (index == 0) perPerson + remainder else perPerson
            ExpenseSplitEntity(expenseId = expenseId, memberId = uid, owedMinor = owed)
        }

        val entity = ExpenseEntity(
            id = expenseId,
            groupId = threadId,
            payerMemberId = payerUid,
            amountMinor = amountMinor,
            currency = currency,
            note = note,
            createdAt = now,
            updatedAt = now,
            deleted = false,
            syncState = SyncState.SYNCED,
            contextType = "DIRECT",
            contextId = threadId
        )

        // Write to Room
        expenseDao.upsertExpense(entity)
        expenseDao.deleteSplitsForExpense(expenseId)
        expenseDao.upsertSplits(splits)

        // Write to Firestore
        val data = mapOf(
            "payerMemberId" to payerUid,
            "amountMinor" to amountMinor,
            "currency" to currency,
            "note" to note,
            "createdAt" to now,
            "updatedAt" to now,
            "deleted" to false,
            "splits" to splits.map { s ->
                mapOf("memberId" to s.memberId, "owedMinor" to s.owedMinor)
            }
        )
        directThreadDS.upsertDirectExpense(threadId, expenseId, data)

        return expenseId
    }

    override suspend fun createDirectPayment(
        threadId: String,
        fromUid: String,
        toUid: String,
        amountMinor: Long,
        currency: String
    ): String {
        require(fromUid != toUid) { "Cannot pay yourself" }
        require(amountMinor > 0) { "Amount must be > 0" }

        val paymentId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()

        val entity = PaymentEntity(
            id = paymentId,
            groupId = threadId,
            fromMemberId = fromUid,
            toMemberId = toUid,
            amountMinor = amountMinor,
            currency = currency,
            createdAt = now,
            updatedAt = now,
            deleted = false,
            syncState = SyncState.SYNCED,
            contextType = "DIRECT",
            contextId = threadId
        )

        // Write to Room
        paymentDao.upsert(entity)

        // Write to Firestore
        val data = mapOf(
            "fromMemberId" to fromUid,
            "toMemberId" to toUid,
            "amountMinor" to amountMinor,
            "currency" to currency,
            "createdAt" to now,
            "updatedAt" to now,
            "deleted" to false
        )
        directThreadDS.upsertDirectPayment(threadId, paymentId, data)

        return paymentId
    }
}

