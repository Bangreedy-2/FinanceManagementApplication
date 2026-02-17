package com.bangreedy.splitsync.data.remote.firestore

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

class FirestoreExpenseDataSource(
    private val firestore: FirebaseFirestore
) {
    fun listenExpensesForGroup(
        groupId: String,
        onChange: (List<DocumentSnapshot>) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration {
        return firestore.collection("groups")
            .document(groupId)
            .collection("expenses")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error)
                    return@addSnapshotListener
                }
                onChange(snapshot?.documents ?: emptyList())
            }
    }

    suspend fun upsertExpense(
        groupId: String,
        expenseId: String,
        data: Map<String, Any?>
    ) {
        firestore.collection("groups")
            .document(groupId)
            .collection("expenses")
            .document(expenseId)
            .set(data, SetOptions.merge())
            .await()
    }
}
