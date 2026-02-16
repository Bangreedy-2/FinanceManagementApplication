package com.bangreedy.splitsync.data.remote.firestore

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class FirestoreGroupDataSource(
    private val firestore: FirebaseFirestore
) {

    fun observeGroups(userId: String): Flow<List<Map<String, Any>>> = callbackFlow {

        val listener = firestore
            .collection("groups")
            .whereArrayContains("memberUserIds", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                val docs = snapshot?.documents?.map { it.data ?: emptyMap() } ?: emptyList()
                trySend(docs)
            }

        awaitClose { listener.remove() }
    }
}
