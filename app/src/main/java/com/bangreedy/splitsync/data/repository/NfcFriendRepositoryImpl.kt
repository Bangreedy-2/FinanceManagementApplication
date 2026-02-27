package com.bangreedy.splitsync.data.repository

import com.bangreedy.splitsync.data.remote.firestore.FirestoreDirectThreadDataSource
import com.bangreedy.splitsync.data.remote.firestore.FirestoreFriendsDataSource
import com.bangreedy.splitsync.data.remote.firestore.FirestoreNfcTokenDataSource
import com.bangreedy.splitsync.domain.model.NfcInvitePayload
import com.bangreedy.splitsync.domain.repository.NfcFriendRepository
import com.bangreedy.splitsync.domain.repository.NfcRedeemResult
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class NfcFriendRepositoryImpl(
    private val tokenDS: FirestoreNfcTokenDataSource,
    private val friendsDS: FirestoreFriendsDataSource,
    private val directThreadDS: FirestoreDirectThreadDataSource,
    private val firestore: FirebaseFirestore
) : NfcFriendRepository {

    override suspend fun createToken(uid: String): NfcInvitePayload {
        val tokenId = tokenDS.createToken(uid)
        return NfcInvitePayload(uid = uid, tokenId = tokenId)
    }

    override suspend fun redeemToken(myUid: String, payload: NfcInvitePayload): NfcRedeemResult {
        // 1. Reject self-add
        if (payload.uid == myUid) {
            return NfcRedeemResult.Error("Cannot add yourself as a friend")
        }

        // 2. Validate + consume token
        try {
            tokenDS.consumeToken(payload.uid, payload.tokenId)
        } catch (e: IllegalArgumentException) {
            return NfcRedeemResult.Error(e.message ?: "Invalid token")
        }

        // 3. Resolve friend display name
        val friendDisplayName = resolveFriendDisplayName(payload.uid)

        // 4. Check if already friends
        val existingDoc = firestore.collection("users")
            .document(myUid)
            .collection("friends")
            .document(payload.uid)
            .get()
            .await()

        if (existingDoc.exists()) {
            val status = existingDoc.getString("status") ?: ""
            return when (status) {
                "accepted" -> NfcRedeemResult.AlreadyFriends(friendDisplayName)
                "pending_incoming" -> {
                    // Auto-accept since NFC tap implies mutual consent
                    friendsDS.acceptFriendRequest(myUid, payload.uid)
                    directThreadDS.ensureThread(myUid, payload.uid)
                    NfcRedeemResult.FriendRequestSent(friendDisplayName)
                }
                "pending_outgoing" -> {
                    // Other user already sent us a request via NFC, auto-accept
                    friendsDS.acceptFriendRequest(payload.uid, myUid)
                    directThreadDS.ensureThread(myUid, payload.uid)
                    NfcRedeemResult.FriendRequestSent(friendDisplayName)
                }
                else -> {
                    friendsDS.sendFriendRequest(myUid, payload.uid)
                    NfcRedeemResult.FriendRequestSent(friendDisplayName)
                }
            }
        }

        // 5. Create instant friend relationship (NFC = mutual consent)
        //    Write both as "accepted" directly
        val now = System.currentTimeMillis()
        val batch = firestore.batch()

        val myRef = firestore.collection("users")
            .document(myUid)
            .collection("friends")
            .document(payload.uid)

        val friendRef = firestore.collection("users")
            .document(payload.uid)
            .collection("friends")
            .document(myUid)

        batch.set(myRef, mapOf(
            "friendUid" to payload.uid,
            "status" to "accepted",
            "createdAt" to now,
            "updatedAt" to now
        ))

        batch.set(friendRef, mapOf(
            "friendUid" to myUid,
            "status" to "accepted",
            "createdAt" to now,
            "updatedAt" to now
        ))

        batch.commit().await()

        // 6. Ensure direct thread
        directThreadDS.ensureThread(myUid, payload.uid)

        return NfcRedeemResult.FriendRequestSent(friendDisplayName)
    }

    private suspend fun resolveFriendDisplayName(uid: String): String {
        return try {
            val doc = firestore.collection("users")
                .document(uid)
                .get()
                .await()
            doc.getString("displayName") ?: doc.getString("username") ?: uid
        } catch (_: Exception) {
            uid
        }
    }
}

