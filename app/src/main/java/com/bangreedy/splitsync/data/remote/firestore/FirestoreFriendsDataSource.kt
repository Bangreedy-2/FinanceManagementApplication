package com.bangreedy.splitsync.data.remote.firestore

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

class FirestoreFriendsDataSource(
    private val firestore: FirebaseFirestore
) {

    fun listenFriends(
        uid: String,
        onChange: (List<DocumentSnapshot>) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration {
        return firestore.collection("users")
            .document(uid)
            .collection("friends")
            .addSnapshotListener { snapshot, error ->
                if (error != null) { onError(error); return@addSnapshotListener }
                onChange(snapshot?.documents ?: emptyList())
            }
    }

    /**
     * Send friend request:
     *  - write sender's doc as pending_outgoing
     *  - write receiver's doc as pending_incoming
     */
    suspend fun sendFriendRequest(senderUid: String, receiverUid: String) {
        val now = System.currentTimeMillis()
        val batch = firestore.batch()

        val senderRef = firestore.collection("users")
            .document(senderUid)
            .collection("friends")
            .document(receiverUid)

        val receiverRef = firestore.collection("users")
            .document(receiverUid)
            .collection("friends")
            .document(senderUid)

        batch.set(senderRef, mapOf(
            "friendUid" to receiverUid,
            "status" to "pending_outgoing",
            "createdAt" to now,
            "updatedAt" to now
        ))

        batch.set(receiverRef, mapOf(
            "friendUid" to senderUid,
            "status" to "pending_incoming",
            "createdAt" to now,
            "updatedAt" to now
        ))

        batch.commit().await()
    }

    /**
     * Accept friend request: update both to accepted.
     */
    suspend fun acceptFriendRequest(myUid: String, friendUid: String) {
        val now = System.currentTimeMillis()
        val batch = firestore.batch()

        val myRef = firestore.collection("users")
            .document(myUid)
            .collection("friends")
            .document(friendUid)

        val friendRef = firestore.collection("users")
            .document(friendUid)
            .collection("friends")
            .document(myUid)

        batch.update(myRef, mapOf("status" to "accepted", "updatedAt" to now))
        batch.update(friendRef, mapOf("status" to "accepted", "updatedAt" to now))

        batch.commit().await()
    }

    /**
     * Decline friend request: delete both docs.
     */
    suspend fun declineFriendRequest(myUid: String, friendUid: String) {
        val batch = firestore.batch()

        val myRef = firestore.collection("users")
            .document(myUid)
            .collection("friends")
            .document(friendUid)

        val friendRef = firestore.collection("users")
            .document(friendUid)
            .collection("friends")
            .document(myUid)

        batch.delete(myRef)
        batch.delete(friendRef)

        batch.commit().await()
    }
}

