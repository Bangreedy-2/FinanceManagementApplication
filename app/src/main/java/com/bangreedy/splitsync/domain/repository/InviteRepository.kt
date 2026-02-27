package com.bangreedy.splitsync.domain.repository

import com.bangreedy.splitsync.domain.model.Invite
import kotlinx.coroutines.flow.Flow

interface InviteRepository {
    fun observeMyInvites(myUid: String): Flow<List<Invite>>

    suspend fun sendInviteByUsernameOrEmail(
        groupId: String,
        groupName: String,
        inviterUid: String,
        inviterDisplayName: String?,
        input: String
    )

    suspend fun acceptInvite(
        myUid: String,
        inviteId: String,
        groupId: String,
        inviterUid: String,
        groupName: String
    )

    suspend fun declineInvite(myUid: String, inviteId: String)
}
