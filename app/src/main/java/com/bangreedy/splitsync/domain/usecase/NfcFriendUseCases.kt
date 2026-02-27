package com.bangreedy.splitsync.domain.usecase

import com.bangreedy.splitsync.domain.model.NfcInvitePayload
import com.bangreedy.splitsync.domain.repository.NfcFriendRepository
import com.bangreedy.splitsync.domain.repository.NfcRedeemResult

class CreateNfcFriendTokenUseCase(
    private val repo: NfcFriendRepository
) {
    suspend operator fun invoke(uid: String): NfcInvitePayload =
        repo.createToken(uid)
}

class AcceptNfcFriendInviteUseCase(
    private val repo: NfcFriendRepository
) {
    suspend operator fun invoke(myUid: String, payload: NfcInvitePayload): NfcRedeemResult =
        repo.redeemToken(myUid, payload)
}

