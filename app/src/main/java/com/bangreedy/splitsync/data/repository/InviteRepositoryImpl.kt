package com.bangreedy.splitsync.data.repository

import com.bangreedy.splitsync.data.remote.firestore.FirestoreInviteDataSource
import com.bangreedy.splitsync.data.remote.firestore.FirestoreUserLookupDataSource
import com.bangreedy.splitsync.domain.model.AppNotification
import com.bangreedy.splitsync.domain.model.Invite
import com.bangreedy.splitsync.domain.repository.InviteRepository
import com.bangreedy.splitsync.domain.repository.NotificationRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.util.UUID

class InviteRepositoryImpl(
    private val lookupDs: FirestoreUserLookupDataSource,
    private val inviteDs: FirestoreInviteDataSource,
    private val notificationRepo: NotificationRepository
) : InviteRepository {

    override fun observeMyInvites(myUid: String): Flow<List<Invite>> = callbackFlow {
        val reg = inviteDs.listenInvites(
            myUid = myUid,
            onChange = { docs ->
                val invites = docs.mapNotNull { d ->
                    val inviteId = d.getString("inviteId") ?: d.id
                    val groupId = d.getString("groupId") ?: return@mapNotNull null
                    val groupName = d.getString("groupName") ?: ""
                    val inviterUid = d.getString("inviterUid") ?: ""
                    val inviterName = d.getString("inviterDisplayName")
                    val status = d.getString("status") ?: "pending"
                    val createdAt = d.getLong("createdAt") ?: 0L

                    Invite(inviteId, groupId, groupName, inviterUid, inviterName, status, createdAt)
                }
                trySend(invites).isSuccess
            },
            onError = { e -> close(e) }
        )
        awaitClose { reg.remove() }
    }

    override suspend fun sendInviteByUsernameOrEmail(
        groupId: String,
        groupName: String,
        inviterUid: String,
        inviterDisplayName: String?,
        input: String
    ) {
        val trimmed = input.trim()
        require(trimmed.isNotBlank()) { "Enter a username or email" }

        val targetUid =
            if (trimmed.contains("@")) lookupDs.findUidByEmail(trimmed)
            else lookupDs.findUidByUsername(trimmed)

        require(targetUid != null) { "User not found" }
        require(targetUid != inviterUid) { "You can’t invite yourself" }

        inviteDs.sendInvite(
            targetUid = targetUid,
            groupId = groupId,
            groupName = groupName,
            inviterUid = inviterUid,
            inviterDisplayName = inviterDisplayName
        )
    }

    override suspend fun acceptInvite(
        myUid: String,
        inviteId: String,
        groupId: String,
        inviterUid: String,
        groupName: String
    ) {
        inviteDs.acceptInvite(myUid, inviteId, groupId)

        // Notify inviter that the invite was accepted
        if (inviterUid.isNotBlank() && inviterUid != myUid) {
            val now = System.currentTimeMillis()
            val n = AppNotification(
                id = UUID.randomUUID().toString(),
                type = "invite_accepted",
                title = "Invite accepted",
                body = "Someone accepted your invite to ${groupName.ifBlank { "a group" }}",
                groupId = groupId,
                actorUid = myUid,
                createdAt = now,
                read = false
            )
            notificationRepo.createNotification(ownerUid = inviterUid, notification = n)
        }
    }

    override suspend fun declineInvite(myUid: String, inviteId: String) {
        inviteDs.declineInvite(myUid, inviteId)
    }
}
