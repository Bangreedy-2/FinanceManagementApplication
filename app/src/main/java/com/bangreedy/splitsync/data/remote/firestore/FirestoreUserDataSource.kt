package com.bangreedy.splitsync.data.remote.firestore

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.tasks.await

class FirestoreUserDataSource(
    private val firestore: FirebaseFirestore
) {
    fun listenUserProfile(
        uid: String,
        onChange: (DocumentSnapshot?) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration {
        return firestore.collection("users")
            .document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error)
                    return@addSnapshotListener
                }
                onChange(snapshot)
            }
    }

    /**
     * Claims username and writes user profile in a transaction to guarantee uniqueness.
     * - If usernames/{usernameLower} exists -> fail.
     * - Else create it and set users/{uid}.
     */
    suspend fun claimUsernameAndUpsertProfile(
        uid: String,
        username: String,
        displayName: String,
        email: String?
    ) {
        val usernameLower = username.trim().lowercase()
        require(usernameLower.isNotBlank()) { "Username required" }

        val usernamesRef = firestore.collection("usernames").document(usernameLower)
        val userRef = firestore.collection("users").document(uid)

        firestore.runTransaction { tx ->

            val existingUsernameSnap = tx.get(usernamesRef)
            if (existingUsernameSnap.exists()) {
                throw IllegalStateException("Username already taken")
            }

            val currentUserSnap = tx.get(userRef)

            val normalizedEmail = email?.trim()?.lowercase()

            val now = System.currentTimeMillis()
            val createdAt = currentUserSnap.getLong("createdAt") ?: now

            val defaultCurrency = currentUserSnap.getString("defaultCurrency") ?: "USD"
            val notificationPrefs = currentUserSnap.get("notificationPrefs") ?: mapOf(
                "pushEnabled" to true,
                "emailEnabled" to true,
                "invitePush" to true,
                "inviteEmail" to true,
                "settlementPush" to true,
                "settlementEmail" to true
            )

            tx.set(usernamesRef, mapOf("uid" to uid))

            tx.set(
                userRef,
                mapOf(
                    "uid" to uid,
                    "username" to username.trim(),
                    "usernameLower" to usernameLower,
                    "displayName" to displayName.trim(),
                    "email" to normalizedEmail,
                    "photoUrl" to currentUserSnap.getString("photoUrl"),
                    "defaultCurrency" to defaultCurrency,
                    "notificationPrefs" to notificationPrefs,
                    "createdAt" to createdAt,
                    "updatedAt" to now
                )
            )
        }.await()
    }

    suspend fun updateProfile(uid: String, updates: Map<String, Any?>) {
        firestore.collection("users").document(uid).update(updates).await()
    }
}
