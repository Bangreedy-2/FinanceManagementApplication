package com.bangreedy.splitsync.data.remote.firestore

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.DocumentSnapshot
import kotlinx.coroutines.tasks.await

class FirestoreGroupDataSource(
    private val firestore: FirebaseFirestore
) {
    fun listenGroupsForUser(
        userId: String,
        onChange: (List<DocumentSnapshot>) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration {
        return firestore.collection("groups")
            .whereArrayContains("memberUserIds", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error)
                    return@addSnapshotListener
                }
                onChange(snapshot?.documents ?: emptyList())
            }
    }

    suspend fun upsertGroup(groupId: String, data: Map<String, Any?>) {
        firestore.collection("groups")
            .document(groupId)
            .set(data, SetOptions.merge())
            .await()
    }
}
