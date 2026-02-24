package com.bangreedy.splitsync.data.sync

import com.bangreedy.splitsync.data.local.dao.PaymentDao
import com.bangreedy.splitsync.data.local.entity.PaymentEntity
import com.bangreedy.splitsync.data.local.entity.SyncState
import com.bangreedy.splitsync.data.remote.firestore.FirestoreGroupDataSource
import com.bangreedy.splitsync.data.remote.firestore.FirestorePaymentDataSource
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class PaymentSyncManager(
    private val paymentDataSource: FirestorePaymentDataSource,
    private val paymentDao: PaymentDao
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val paymentListeners = mutableMapOf<String, ListenerRegistration>()

    fun onGroupsChanged(groupIds: Set<String>) {
        groupIds.forEach { groupId ->
            if (paymentListeners.containsKey(groupId)) return@forEach
            val reg = paymentDataSource.listenPaymentsForGroup(
                groupId = groupId,
                onChange = { docs -> onPaymentsChanged(groupId, docs) },
                onError = { /* TODO log */ }
            )
            paymentListeners[groupId] = reg
        }

        val toRemove = paymentListeners.keys - groupIds
        toRemove.forEach { gid ->
            paymentListeners.remove(gid)?.remove()
        }
    }

    fun stop() {
        paymentListeners.values.forEach { it.remove() }
        paymentListeners.clear()
    }


    suspend fun pushDirtyPayments() {
        val dirty = paymentDao.getDirtyPayments(SyncState.DIRTY)

        for (p in dirty) {
            val data = mapOf(
                "fromMemberId" to p.fromMemberId,
                "toMemberId" to p.toMemberId,
                "amountMinor" to p.amountMinor,
                "currency" to p.currency,
                "createdAt" to p.createdAt,
                "updatedAt" to p.updatedAt,
                "deleted" to p.deleted
            )

            paymentDataSource.upsertPayment(
                groupId = p.groupId,
                paymentId = p.id,
                data = data
            )

            paymentDao.setPaymentSyncState(p.id, SyncState.SYNCED)
        }
    }

    private fun onPaymentsChanged(groupId: String, docs: List<DocumentSnapshot>) {
        docs.forEach { doc ->
            val paymentId = doc.id

            val fromMemberId = doc.getString("fromMemberId") ?: return@forEach
            val toMemberId = doc.getString("toMemberId") ?: return@forEach
            val amountMinor = doc.getLong("amountMinor") ?: return@forEach
            val currency = doc.getString("currency") ?: "EUR"
            val createdAt = doc.getLong("createdAt") ?: 0L
            val updatedAt = doc.getLong("updatedAt") ?: createdAt
            val deleted = doc.getBoolean("deleted") ?: false

            scope.launch {
                paymentDao.upsert(
                    PaymentEntity(
                        id = paymentId,
                        groupId = groupId,
                        fromMemberId = fromMemberId,
                        toMemberId = toMemberId,
                        amountMinor = amountMinor,
                        currency = currency,
                        createdAt = createdAt,
                        updatedAt = updatedAt,
                        deleted = deleted,
                        syncState = SyncState.SYNCED,
                        contextType = "GROUP",
                        contextId = groupId
                    )
                )
            }
        }
    }
}
