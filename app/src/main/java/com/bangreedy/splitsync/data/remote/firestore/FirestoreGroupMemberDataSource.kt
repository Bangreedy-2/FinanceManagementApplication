package com.bangreedy.splitsync.data.remote.firestore

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class FirestoreGroupMemberDataSource(
    private val firestore: FirebaseFirestore
) {
    fun listenGroupMembers(
        groupId: String,
        onChange: (List<DocumentSnapshot>) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration {
        return firestore.collection("groups")
            .document(groupId)
            .collection("members")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error)
                    return@addSnapshotListener
                }
                onChange(snapshot?.documents ?: emptyList())
            }
    }
}
