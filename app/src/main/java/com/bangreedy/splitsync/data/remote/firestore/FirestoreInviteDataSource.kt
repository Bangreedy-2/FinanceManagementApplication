package com.bangreedy.splitsync.data.remote.firestore

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.tasks.await
import java.util.UUID

class FirestoreInviteDataSource(
    private val firestore: FirebaseFirestore
) {
    fun listenInvites(
        myUid: String,
        onChange: (List<DocumentSnapshot>) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration {
        return firestore.collection("users")
            .document(myUid)
            .collection("invites")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error)
                    return@addSnapshotListener
                }
                onChange(snapshot?.documents ?: emptyList())
            }
    }

    suspend fun sendInvite(
        targetUid: String,
        groupId: String,
        groupName: String,
        inviterUid: String,
        inviterDisplayName: String?
    ): String {
        val inviteId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()

        val data = mapOf(
            "inviteId" to inviteId,
            "groupId" to groupId,
            "groupName" to groupName,
            "inviterUid" to inviterUid,
            "inviterDisplayName" to inviterDisplayName,
            "status" to "pending", // pending | accepted | declined
            "createdAt" to now,
            "updatedAt" to now
        )

        firestore.collection("users")
            .document(targetUid)
            .collection("invites")
            .document(inviteId)
            .set(data)
            .await()

        return inviteId
    }

    suspend fun acceptInvite(myUid: String, inviteId: String, groupId: String) {
        val inviteRef = firestore.collection("users")
            .document(myUid)
            .collection("invites")
            .document(inviteId)

        val groupRef = firestore.collection("groups")
            .document(groupId)

        val memberRef = groupRef.collection("members").document(myUid)

        val now = System.currentTimeMillis()

        firestore.runBatch { batch ->
            batch.update(groupRef, "memberUserIds", FieldValue.arrayUnion(myUid))

            // optional but recommended membership doc for later roles/display
            batch.set(memberRef, mapOf("uid" to myUid, "joinedAt" to now, "role" to "member"))

            batch.update(inviteRef, mapOf("status" to "accepted", "updatedAt" to now))
        }.await()
    }

    suspend fun declineInvite(myUid: String, inviteId: String) {
        val now = System.currentTimeMillis()
        firestore.collection("users")
            .document(myUid)
            .collection("invites")
            .document(inviteId)
            .update(mapOf("status" to "declined", "updatedAt" to now))
            .await()
    }
}
