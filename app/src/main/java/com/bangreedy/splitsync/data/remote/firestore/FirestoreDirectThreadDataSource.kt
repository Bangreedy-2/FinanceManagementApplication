package com.bangreedy.splitsync.data.remote.firestore

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

class FirestoreDirectThreadDataSource(
    private val firestore: FirebaseFirestore
) {

    companion object {
        /** Deterministic thread ID for a pair of users. */
        fun threadId(uidA: String, uidB: String): String {
            return if (uidA < uidB) "${uidA}_${uidB}" else "${uidB}_${uidA}"
        }
    }

    /**
     * Create or ensure the direct thread doc exists.
     */
    suspend fun ensureThread(uidA: String, uidB: String): String {
        val tid = threadId(uidA, uidB)
        val ref = firestore.collection("directThreads").document(tid)
        val snap = ref.get().await()
        if (!snap.exists()) {
            val sorted = listOf(uidA, uidB).sorted()
            ref.set(mapOf(
                "userIds" to sorted,
                "createdAt" to System.currentTimeMillis(),
                "updatedAt" to System.currentTimeMillis(),
                "deleted" to false
            )).await()
        }
        return tid
    }

    fun listenDirectExpenses(
        threadId: String,
        onChange: (List<DocumentSnapshot>) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration {
        return firestore.collection("directThreads")
            .document(threadId)
            .collection("expenses")
            .addSnapshotListener { snapshot, error ->
                if (error != null) { onError(error); return@addSnapshotListener }
                onChange(snapshot?.documents ?: emptyList())
            }
    }

    fun listenDirectPayments(
        threadId: String,
        onChange: (List<DocumentSnapshot>) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration {
        return firestore.collection("directThreads")
            .document(threadId)
            .collection("payments")
            .addSnapshotListener { snapshot, error ->
                if (error != null) { onError(error); return@addSnapshotListener }
                onChange(snapshot?.documents ?: emptyList())
            }
    }

    suspend fun upsertDirectExpense(
        threadId: String,
        expenseId: String,
        data: Map<String, Any?>
    ) {
        firestore.collection("directThreads")
            .document(threadId)
            .collection("expenses")
            .document(expenseId)
            .set(data, SetOptions.merge())
            .await()
    }

    suspend fun upsertDirectPayment(
        threadId: String,
        paymentId: String,
        data: Map<String, Any?>
    ) {
        firestore.collection("directThreads")
            .document(threadId)
            .collection("payments")
            .document(paymentId)
            .set(data, SetOptions.merge())
            .await()
    }
}

