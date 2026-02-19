package com.bangreedy.splitsync.data.remote.firestore

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class FirestoreUserLookupDataSource(
    private val firestore: FirebaseFirestore
) {
    suspend fun findUidByUsername(username: String): String? {
        val key = username.trim().lowercase()
        if (key.isBlank()) return null

        val snap = firestore.collection("usernames")
            .document(key)
            .get()
            .await()

        return snap.getString("uid")
    }

    suspend fun findUidByEmail(email: String): String? {
        val e = email.trim().lowercase()
        if (e.isBlank()) return null

        val snap = firestore.collection("users")
            .whereEqualTo("emailLower", e)
            .limit(1)
            .get()
            .await()

        return snap.documents.firstOrNull()?.id
    }
}
