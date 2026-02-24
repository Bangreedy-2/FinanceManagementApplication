package com.bangreedy.splitsync.data.sync

import com.bangreedy.splitsync.data.local.dao.ExpenseDao
import com.bangreedy.splitsync.data.local.entity.ExpenseEntity
import com.bangreedy.splitsync.data.local.entity.ExpenseSplitEntity
import com.bangreedy.splitsync.data.local.entity.SyncState
import com.bangreedy.splitsync.data.remote.firestore.FirestoreExpenseDataSource
import com.bangreedy.splitsync.data.remote.firestore.FirestoreGroupDataSource
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class ExpenseSyncManager(
    private val groupDataSource: FirestoreGroupDataSource,
    private val expenseDataSource: FirestoreExpenseDataSource,
    private val expenseDao: ExpenseDao
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var groupsListener: ListenerRegistration? = null
    private val expenseListeners = mutableMapOf<String, ListenerRegistration>() // groupId -> listener

    fun start(userId: String) {
        // Avoid duplicate listeners if start() called again
        if (groupsListener != null) return

        groupsListener = groupDataSource.listenGroupsForUser(
            userId = userId,
            onChange = { groupDocs ->
                val groupIds = groupDocs.map { it.id }.toSet()

                // Start expense listener per group
                groupIds.forEach { groupId ->
                    if (expenseListeners.containsKey(groupId)) return@forEach

                    val reg = expenseDataSource.listenExpensesForGroup(
                        groupId = groupId,
                        onChange = { expenseDocs ->
                            onExpensesChanged(groupId, expenseDocs)
                        },
                        onError = { /* log if you want */ }
                    )
                    expenseListeners[groupId] = reg
                }

                // Remove listeners for groups no longer present
                val toRemove = expenseListeners.keys - groupIds
                toRemove.forEach { gid ->
                    expenseListeners.remove(gid)?.remove()
                }
            },
            onError = { /* log if you want */ }
        )
    }

    /**
     * Room DIRTY -> Firestore
     * Uses expenseDataSource.upsertExpense(...) instead of firestore directly.
     */
    suspend fun pushDirtyExpenses() {
        val dirtyExpenses = expenseDao.getDirtyExpenses(SyncState.DIRTY)

        for (e in dirtyExpenses) {
            val splits = expenseDao.getSplitsForExpense(e.id)

            val data = mapOf(
                "payerMemberId" to e.payerMemberId,
                "amountMinor" to e.amountMinor,
                "currency" to e.currency,
                "note" to e.note,
                "createdAt" to e.createdAt,
                "updatedAt" to e.updatedAt,
                "deleted" to e.deleted,
                "splits" to splits.map { s ->
                    mapOf(
                        "memberId" to s.memberId,
                        "owedMinor" to s.owedMinor
                    )
                }
            )

            expenseDataSource.upsertExpense(
                groupId = e.groupId,
                expenseId = e.id,
                data = data
            )

            expenseDao.setExpenseSyncState(e.id, SyncState.SYNCED)
        }
    }

    private fun onExpensesChanged(groupId: String, expenseDocs: List<DocumentSnapshot>) {
        expenseDocs.forEach { doc ->
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
                        groupId = groupId,
                        payerMemberId = payerMemberId,
                        amountMinor = amountMinor,
                        currency = currency,
                        note = note,
                        createdAt = createdAt,
                        updatedAt = updatedAt,
                        deleted = deleted,
                        syncState = SyncState.SYNCED
                    )
                )

                expenseDao.deleteSplitsForExpense(expenseId)
                if (splits.isNotEmpty()) {
                    expenseDao.upsertSplits(splits)
                }
            }
        }
    }

    private fun parseSplits(expenseId: String, splitsAny: Any?): List<ExpenseSplitEntity> {
        val list = splitsAny as? List<*> ?: return emptyList()
        val out = ArrayList<ExpenseSplitEntity>(list.size)

        for (item in list) {
            val map = item as? Map<*, *> ?: continue
            val memberId = map["memberId"] as? String ?: continue
            val owed = (map["owedMinor"] as? Number)?.toLong() ?: continue
            out += ExpenseSplitEntity(
                expenseId = expenseId,
                memberId = memberId,
                owedMinor = owed
            )
        }
        return out
    }
}
