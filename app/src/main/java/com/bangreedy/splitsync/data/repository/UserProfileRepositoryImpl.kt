package com.bangreedy.splitsync.data.repository

import com.bangreedy.splitsync.data.remote.firestore.FirestoreUserDataSource
import com.bangreedy.splitsync.data.remote.firestore.FirestoreUserLookupDataSource
import com.bangreedy.splitsync.domain.model.NotificationPrefs
import com.bangreedy.splitsync.domain.model.UserProfile
import com.bangreedy.splitsync.domain.repository.UserProfileRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class UserProfileRepositoryImpl(
    private val ds: FirestoreUserDataSource,
    private val lookupDS: FirestoreUserLookupDataSource
) : UserProfileRepository {

    override fun observeMyProfile(uid: String): Flow<UserProfile?> = callbackFlow {
        val reg = ds.listenUserProfile(
            uid = uid,
            onChange = { snap ->
                if (snap == null || !snap.exists()) {
                    trySend(null).isSuccess
                    return@listenUserProfile
                }
                val prefs = (snap.get("notificationPrefs") as? Map<String, Boolean>) ?: emptyMap()
                val profile = UserProfile(
                    uid = uid,
                    username = snap.getString("username") ?: "",
                    displayName = snap.getString("displayName") ?: "",
                    email = snap.getString("email"),
                    photoUrl = snap.getString("photoUrl"),
                    defaultCurrency = snap.getString("defaultCurrency") ?: "USD",
                    notificationPrefs = NotificationPrefs(
                        pushEnabled = prefs["pushEnabled"] ?: true,
                        emailEnabled = prefs["emailEnabled"] ?: true,
                        invitePush = prefs["invitePush"] ?: true,
                        inviteEmail = prefs["inviteEmail"] ?: true,
                        settlementPush = prefs["settlementPush"] ?: true,
                        settlementEmail = prefs["settlementEmail"] ?: true
                    )
                )
                trySend(profile).isSuccess
            },
            onError = { e -> close(e) }
        )
        awaitClose { reg.remove() }
    }

    override suspend fun claimUsername(uid: String, username: String, displayName: String, email: String?) {
        validate(username, displayName)
        ds.claimUsernameAndUpsertProfile(uid, username, displayName, email)
    }

    override suspend fun isUsernameAvailable(username: String): Boolean {
        val u = username.trim().lowercase()
        if (u.isBlank()) return false
        val owner = lookupDS.findUidByUsername(u)
        return owner == null
    }

    override suspend fun updateProfile(uid: String, displayName: String?, photoUrl: String?) {
        val updates = mutableMapOf<String, Any?>()
        if (displayName != null) updates["displayName"] = displayName
        if (photoUrl != null) updates["photoUrl"] = photoUrl
        updates["updatedAt"] = System.currentTimeMillis()
        if (updates.isNotEmpty()) {
            ds.updateProfile(uid, updates)
        }
    }

    override suspend fun updateDefaultCurrency(uid: String, currencyCode: String) {
        ds.updateProfile(
            uid,
            mapOf(
                "defaultCurrency" to currencyCode,
                "updatedAt" to System.currentTimeMillis()
            )
        )
    }

    override suspend fun updateNotificationPrefs(uid: String, prefs: NotificationPrefs) {
        val map = mapOf(
            "pushEnabled" to prefs.pushEnabled,
            "emailEnabled" to prefs.emailEnabled,
            "invitePush" to prefs.invitePush,
            "inviteEmail" to prefs.inviteEmail,
            "settlementPush" to prefs.settlementPush,
            "settlementEmail" to prefs.settlementEmail
        )
        ds.updateProfile(
            uid,
            mapOf(
                "notificationPrefs" to map,
                "updatedAt" to System.currentTimeMillis()
            )
        )
    }

    private fun validate(username: String, displayName: String) {
        val u = username.trim()
        require(u.length in 3..20) { "Username must be 3-20 chars" }
        require(u.all { it.isLetterOrDigit() || it == '_' }) { "Username: letters/digits/_ only" }
        require(displayName.trim().isNotBlank()) { "Display name required" }
    }
}
