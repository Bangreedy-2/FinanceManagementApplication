package com.bangreedy.splitsync.data.sync

import com.bangreedy.splitsync.data.local.dao.ExpenseDao
import com.bangreedy.splitsync.data.local.dao.PaymentDao
import com.bangreedy.splitsync.data.local.entity.ExpenseEntity
import com.bangreedy.splitsync.data.local.entity.ExpenseSplitEntity
import com.bangreedy.splitsync.data.local.entity.PaymentEntity
import com.bangreedy.splitsync.data.local.entity.SyncState
import com.bangreedy.splitsync.data.remote.firestore.FirestoreDirectThreadDataSource
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class DirectThreadSyncManager(
    private val directThreadDS: FirestoreDirectThreadDataSource,
    private val expenseDao: ExpenseDao,
    private val paymentDao: PaymentDao
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val expenseListeners = mutableMapOf<String, ListenerRegistration>()
    private val paymentListeners = mutableMapOf<String, ListenerRegistration>()
    private var myUid: String? = null

    fun setMyUid(uid: String) {
        myUid = uid
    }

    fun onAcceptedFriendsChanged(friendUids: Set<String>) {
        val uid = myUid ?: return
        val threadIds = friendUids.map { FirestoreDirectThreadDataSource.threadId(uid, it) }.toSet()

        // Start listeners for new threads
        threadIds.forEach { tid ->
            if (!expenseListeners.containsKey(tid)) {
                expenseListeners[tid] = directThreadDS.listenDirectExpenses(
                    threadId = tid,
                    onChange = { docs -> onDirectExpensesChanged(tid, docs) },
                    onError = { /* TODO log */ }
                )
            }
            if (!paymentListeners.containsKey(tid)) {
                paymentListeners[tid] = directThreadDS.listenDirectPayments(
                    threadId = tid,
                    onChange = { docs -> onDirectPaymentsChanged(tid, docs) },
                    onError = { /* TODO log */ }
                )
            }
        }

        // Remove listeners for threads no longer needed
        (expenseListeners.keys - threadIds).forEach { tid ->
            expenseListeners.remove(tid)?.remove()
        }
        (paymentListeners.keys - threadIds).forEach { tid ->
            paymentListeners.remove(tid)?.remove()
        }
    }

    fun stop() {
        expenseListeners.values.forEach { it.remove() }
        expenseListeners.clear()
        paymentListeners.values.forEach { it.remove() }
        paymentListeners.clear()
    }

    private fun onDirectExpensesChanged(threadId: String, docs: List<DocumentSnapshot>) {
        docs.forEach { doc ->
            val expenseId = doc.id
            val payerMemberId = doc.getString("payerMemberId") ?: return@forEach
            val amountMinor = doc.getLong("amountMinor") ?: return@forEach
            val currency = doc.getString("currency") ?: "EUR"
            val note = doc.getString("note")
            val createdAt = doc.getLong("createdAt") ?: 0L
            val updatedAt = doc.getLong("updatedAt") ?: createdAt
            val deleted = doc.getBoolean("deleted") ?: false

            val splitsAny = doc.get("splits")
            val splits = parseSplits(expenseId, splitsAny)

            scope.launch {
                expenseDao.upsertExpense(
                    ExpenseEntity(
                        id = expenseId,
                        groupId = threadId,          // groupId field stores threadId for DIRECT
                        payerMemberId = payerMemberId,
                        amountMinor = amountMinor,
                        currency = currency,
                        note = note,
                        createdAt = createdAt,
                        updatedAt = updatedAt,
                        deleted = deleted,
                        syncState = SyncState.SYNCED,
                        contextType = "DIRECT",
                        contextId = threadId
                    )
                )
                expenseDao.deleteSplitsForExpense(expenseId)
                if (splits.isNotEmpty()) {
                    expenseDao.upsertSplits(splits)
                }
            }
        }
    }

    private fun onDirectPaymentsChanged(threadId: String, docs: List<DocumentSnapshot>) {
        docs.forEach { doc ->
            val paymentId = doc.id
            val fromMemberId = doc.getString("fromMemberId") ?: return@forEach
            val toMemberId = doc.getString("toMemberId") ?: return@forEach
            val amountMinor = doc.getLong("amountMinor") ?: return@forEach
            val currency = doc.getString("currency") ?: "EUR"
            val createdAt = doc.getLong("createdAt") ?: 0L
            val updatedAt = doc.getLong("updatedAt") ?: createdAt
            val deleted = doc.getBoolean("deleted") ?: false
            val mode = doc.getString("mode") ?: "ONE_CURRENCY"
            val ratesLastUpdatedAt = doc.getLong("ratesLastUpdatedAt")
            val asOfDate = doc.getString("asOfDate")
            val settlementId = doc.getString("settlementId")

            @Suppress("UNCHECKED_CAST")
            val breakdownMap = doc.get("breakdownByCurrency") as? Map<String, Long>
            val breakdownJson = breakdownMap?.let { map ->
                val obj = org.json.JSONObject()
                map.forEach { (k, v) -> obj.put(k, v) }
                obj.toString()
            }

            scope.launch {
                paymentDao.upsert(
                    PaymentEntity(
                        id = paymentId,
                        groupId = threadId,
                        fromMemberId = fromMemberId,
                        toMemberId = toMemberId,
                        amountMinor = amountMinor,
                        currency = currency,
                        createdAt = createdAt,
                        updatedAt = updatedAt,
                        deleted = deleted,
                        syncState = SyncState.SYNCED,
                        contextType = "DIRECT",
                        contextId = threadId,
                        mode = mode,
                        breakdownJson = breakdownJson,
                        ratesLastUpdatedAt = ratesLastUpdatedAt,
                        asOfDate = asOfDate,
                        settlementId = settlementId
                    )
                )
            }
        }
    }

    private fun parseSplits(expenseId: String, splitsAny: Any?): List<ExpenseSplitEntity> {
        val list = splitsAny as? List<*> ?: return emptyList()
        return list.mapNotNull { item ->
            val map = item as? Map<*, *> ?: return@mapNotNull null
            val memberId = map["memberId"] as? String ?: return@mapNotNull null
            val owed = (map["owedMinor"] as? Number)?.toLong() ?: return@mapNotNull null
            ExpenseSplitEntity(expenseId = expenseId, memberId = memberId, owedMinor = owed)
        }
    }
}

