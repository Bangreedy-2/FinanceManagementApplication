package com.bangreedy.splitsync.data.sync

import com.bangreedy.splitsync.data.local.dao.GroupMemberDao
import com.bangreedy.splitsync.data.local.dao.UserProfileDao
import com.bangreedy.splitsync.data.local.entity.SyncState
import com.bangreedy.splitsync.data.local.entity.UserProfileEntity
import com.bangreedy.splitsync.data.remote.firestore.FirestoreUserProfileDataSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class UserProfileSyncManager(
    private val remote: FirestoreUserProfileDataSource,
    private val groupMemberDao: GroupMemberDao,
    private val userProfileDao: UserProfileDao
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun onGroupsChanged(groupIds: Set<String>) {
        scope.launch {
            val uids = groupIds
                .flatMap { gid -> groupMemberDao.getMemberUidsOnce(gid) }
                .distinct()

            uids.forEach { uid ->
                val data = remote.getUserProfile(uid) ?: return@forEach

                val username = (data["username"] as? String).orEmpty()
                val displayName = (data["displayName"] as? String).orEmpty()
                val email = data["email"] as? String
                val photoUrl = data["photoUrl"] as? String
                val defaultCurrency = (data["defaultCurrency"] as? String) ?: "USD"

                val prefs = (data["notificationPrefs"] as? Map<String, Boolean>) ?: emptyMap()
                val notifyPush = prefs["pushEnabled"] ?: true
                val notifyEmail = prefs["emailEnabled"] ?: true
                val notifyInvitePush = prefs["invitePush"] ?: true
                val notifyInviteEmail = prefs["inviteEmail"] ?: true
                val notifySettlementPush = prefs["settlementPush"] ?: true
                val notifySettlementEmail = prefs["settlementEmail"] ?: true

                val createdAt = (data["createdAt"] as? Number)?.toLong() ?: 0L
                val updatedAt = (data["updatedAt"] as? Number)?.toLong() ?: createdAt
                val deleted = data["deleted"] as? Boolean ?: false

                if (username.isBlank() && displayName.isBlank()) return@forEach

                userProfileDao.upsert(
                    UserProfileEntity(
                        uid = uid,
                        username = username,
                        displayName = displayName,
                        email = email,
                        photoUrl = photoUrl,
                        defaultCurrency = defaultCurrency,
                        notifyPush = notifyPush,
                        notifyEmail = notifyEmail,
                        notifyInvitePush = notifyInvitePush,
                        notifyInviteEmail = notifyInviteEmail,
                        notifySettlementPush = notifySettlementPush,
                        notifySettlementEmail = notifySettlementEmail,
                        createdAt = createdAt,
                        updatedAt = updatedAt,
                        deleted = deleted,
                        syncState = SyncState.SYNCED
                    )
                )
            }
        }
    }

    fun refreshUids(uids: Collection<String>) {
        val unique = uids.filter { it.isNotBlank() }.distinct()
        if (unique.isEmpty()) return

        scope.launch {
            unique.forEach { uid ->
                val data = remote.getUserProfile(uid) ?: return@forEach

                val username = (data["username"] as? String).orEmpty()
                val displayName = (data["displayName"] as? String).orEmpty()
                val email = data["email"] as? String
                val createdAt = (data["createdAt"] as? Number)?.toLong() ?: 0L
                val updatedAt = (data["updatedAt"] as? Number)?.toLong() ?: createdAt
                val deleted = data["deleted"] as? Boolean ?: false

                if (username.isBlank() && displayName.isBlank()) return@forEach

                userProfileDao.upsert(
                    UserProfileEntity(
                        uid = uid,
                        username = username,
                        displayName = displayName,
                        email = email,
                        createdAt = createdAt,
                        updatedAt = updatedAt,
                        deleted = deleted,
                        syncState = SyncState.SYNCED
                    )
                )
            }
        }
    }
}
