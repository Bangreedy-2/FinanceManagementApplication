package com.bangreedy.splitsync.data.remote.firestore

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

class FirestorePaymentDataSource(
    private val firestore: FirebaseFirestore
) {
    fun listenPaymentsForGroup(
        groupId: String,
        onChange: (List<DocumentSnapshot>) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration {
        return firestore.collection("groups")
            .document(groupId)
            .collection("payments")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error)
                    return@addSnapshotListener
                }
                onChange(snapshot?.documents ?: emptyList())
            }
    }

    suspend fun upsertPayment(
        groupId: String,
        paymentId: String,
        data: Map<String, Any?>
    ) {
        firestore.collection("groups")
            .document(groupId)
            .collection("payments")
            .document(paymentId)
            .set(data, SetOptions.merge())
            .await()
    }
}
