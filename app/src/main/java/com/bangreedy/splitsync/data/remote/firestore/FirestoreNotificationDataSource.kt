package com.bangreedy.splitsync.data.remote.firestore

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

class FirestoreNotificationDataSource(
    private val firestore: FirebaseFirestore
) {
    fun listenNotifications(
        uid: String,
        onChange: (List<DocumentSnapshot>) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration {
        return firestore.collection("users")
            .document(uid)
            .collection("notifications")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error)
                    return@addSnapshotListener
                }
                onChange(snapshot?.documents ?: emptyList())
            }
    }

    suspend fun upsertNotification(
        ownerUid: String,
        notificationId: String,
        data: Map<String, Any?>
    ) {
        firestore.collection("users")
            .document(ownerUid)
            .collection("notifications")
            .document(notificationId)
            .set(data, SetOptions.merge())
            .await()
    }

    suspend fun markRead(ownerUid: String, notificationId: String) {
        firestore.collection("users")
            .document(ownerUid)
            .collection("notifications")
            .document(notificationId)
            .set(mapOf("read" to true), SetOptions.merge())
            .await()
    }
}