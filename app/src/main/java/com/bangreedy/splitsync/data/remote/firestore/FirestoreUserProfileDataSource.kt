package com.bangreedy.splitsync.data.remote.firestore

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class FirestoreUserProfileDataSource(
    private val firestore: FirebaseFirestore
) {
    suspend fun getUserProfile(uid: String): Map<String, Any?>? {
        val snap = firestore.collection("users")
            .document(uid)
            .get()
            .await()
        if (!snap.exists()) return null
        return snap.data
    }
}
