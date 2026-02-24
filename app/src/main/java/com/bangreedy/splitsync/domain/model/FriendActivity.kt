package com.bangreedy.splitsync.domain.model

sealed class FriendActivityItem {
    abstract val id: String
    abstract val createdAt: Long
    abstract val currency: String
    abstract val amountMinor: Long
    abstract val pairwiseDeltaMinor: Long  // positive = friend owes me, negative = I owe friend
    abstract val source: ActivitySource

    data class ExpenseItem(
        override val id: String,
        override val createdAt: Long,
        override val currency: String,
        override val amountMinor: Long,
        override val pairwiseDeltaMinor: Long,
        override val source: ActivitySource,
        val description: String?,
        val payerUid: String,
        val payerName: String
    ) : FriendActivityItem()

    data class PaymentItem(
        override val id: String,
        override val createdAt: Long,
        override val currency: String,
        override val amountMinor: Long,
        override val pairwiseDeltaMinor: Long,
        override val source: ActivitySource,
        val fromUid: String,
        val toUid: String,
        val fromName: String,
        val toName: String
    ) : FriendActivityItem()
}

sealed class ActivitySource {
    data class Group(val groupId: String, val groupName: String) : ActivitySource()
    data object Direct : ActivitySource()
}

data class FriendActivity(
    val items: List<FriendActivityItem>,
    val netByCurrency: Map<String, Long>  // positive = friend owes me
)

