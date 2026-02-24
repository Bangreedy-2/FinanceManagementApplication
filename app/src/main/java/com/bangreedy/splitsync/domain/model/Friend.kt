package com.bangreedy.splitsync.domain.model

data class Friend(
    val friendUid: String,
    val status: FriendStatus,
    val nickname: String? = null,
    val displayName: String = "",
    val username: String = "",
    val photoUrl: String? = null,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)

enum class FriendStatus {
    PENDING_OUTGOING,
    PENDING_INCOMING,
    ACCEPTED,
    BLOCKED;

    companion object {
        fun fromString(s: String): FriendStatus = when (s.lowercase()) {
            "pending_outgoing" -> PENDING_OUTGOING
            "pending_incoming" -> PENDING_INCOMING
            "accepted" -> ACCEPTED
            "blocked" -> BLOCKED
            else -> ACCEPTED
        }
    }

    fun toFirestoreString(): String = name.lowercase()
}

