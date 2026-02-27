package com.bangreedy.splitsync.domain.model

import android.net.Uri

data class NfcInvitePayload(
    val uid: String,
    val tokenId: String,
    val version: Int = 1
) {
    fun toUriString(): String =
        "https://splitsync.app/friend?v=$version&uid=$uid&t=$tokenId"

    companion object {
        fun fromUri(uriString: String): NfcInvitePayload? {
            return try {
                val uri = Uri.parse(uriString)
                if (uri.host != "splitsync.app" || uri.path != "/friend") return null
                val uid = uri.getQueryParameter("uid") ?: return null
                val tokenId = uri.getQueryParameter("t") ?: return null
                val version = uri.getQueryParameter("v")?.toIntOrNull() ?: 1
                NfcInvitePayload(uid = uid, tokenId = tokenId, version = version)
            } catch (_: Exception) {
                null
            }
        }
    }
}

