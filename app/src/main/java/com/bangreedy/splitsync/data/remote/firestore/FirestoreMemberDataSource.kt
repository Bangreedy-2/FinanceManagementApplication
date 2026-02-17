package com.bangreedy.splitsync.data.remote.firestore

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.DocumentSnapshot
import kotlinx.coroutines.tasks.await

class FirestoreMemberDataSource(
    private val firestore: FirebaseFirestore
) {
    fun listenMembers(
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

    suspend fun upsertMember(groupId: String, memberId: String, data: Map<String, Any?>) {
        try {
            Log.d("RemoteMember", "upsertMember group=$groupId member=$memberId dataKeys=${data.keys}")

            firestore.collection("groups")
                .document(groupId)
                .collection("members")
                .document(memberId)
                .set(data, SetOptions.merge())
                .await()

            Log.d("RemoteMember", "upsertMember OK group=$groupId member=$memberId")
        } catch (e: Exception) {
            Log.e("RemoteMember", "upsertMember FAILED group=$groupId member=$memberId", e)
            throw e
        }
    }
}
