package com.bangreedy.splitsync.domain.repository

import com.bangreedy.splitsync.domain.model.NfcInvitePayload

sealed class NfcRedeemResult {
    data class FriendRequestSent(val friendDisplayName: String) : NfcRedeemResult()
    data class AlreadyFriends(val friendDisplayName: String) : NfcRedeemResult()
    data class Error(val message: String) : NfcRedeemResult()
}

interface NfcFriendRepository {
    suspend fun createToken(uid: String): NfcInvitePayload
    suspend fun redeemToken(myUid: String, payload: NfcInvitePayload): NfcRedeemResult
}

