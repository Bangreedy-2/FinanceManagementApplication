package com.bangreedy.splitsync.data.remote.firestore

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.UUID

data class NfcTokenDoc(
    val tokenId: String,
    val issuedAt: Long,
    val expiresAt: Long,
    val used: Boolean
)

class FirestoreNfcTokenDataSource(
    private val firestore: FirebaseFirestore
) {
    companion object {
        private const val TOKEN_TTL_MILLIS = 5 * 60 * 1000L // 5 minutes
    }

    /**
     * Create an ephemeral NFC token under users/{uid}/nfc_tokens/{tokenId}.
     * Returns the generated tokenId.
     */
    suspend fun createToken(uid: String): String {
        val tokenId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()

        val data = mapOf(
            "tokenId" to tokenId,
            "issuedAt" to now,
            "expiresAt" to (now + TOKEN_TTL_MILLIS),
            "used" to false
        )

        firestore.collection("users")
            .document(uid)
            .collection("nfc_tokens")
            .document(tokenId)
            .set(data)
            .await()

        return tokenId
    }

    /**
     * Read and consume an NFC token. Validates existence, expiry, and used status.
     * Marks the token as used atomically via a Firestore transaction.
     * @throws IllegalArgumentException if token is invalid, expired, or already used.
     */
    suspend fun consumeToken(ownerUid: String, tokenId: String) {
        val docRef = firestore.collection("users")
            .document(ownerUid)
            .collection("nfc_tokens")
            .document(tokenId)

        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(docRef)

            if (!snapshot.exists()) {
                throw IllegalArgumentException("Token not found")
            }

            val expiresAt = snapshot.getLong("expiresAt") ?: 0L
            val used = snapshot.getBoolean("used") ?: false

            if (used) {
                throw IllegalArgumentException("Token already used")
            }

            if (System.currentTimeMillis() > expiresAt) {
                throw IllegalArgumentException("Token expired")
            }

            transaction.update(docRef, "used", true)
        }.await()
    }

    /**
     * Read a token without consuming it (for validation checks).
     */
    suspend fun readToken(ownerUid: String, tokenId: String): NfcTokenDoc? {
        val snapshot = firestore.collection("users")
            .document(ownerUid)
            .collection("nfc_tokens")
            .document(tokenId)
            .get()
            .await()

        if (!snapshot.exists()) return null

        return NfcTokenDoc(
            tokenId = snapshot.getString("tokenId") ?: tokenId,
            issuedAt = snapshot.getLong("issuedAt") ?: 0L,
            expiresAt = snapshot.getLong("expiresAt") ?: 0L,
            used = snapshot.getBoolean("used") ?: false
        )
    }
}

