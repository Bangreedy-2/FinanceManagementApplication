package com.bangreedy.splitsync.data.remote.dto

data class FirestoreExpense(
    val id: String = "",
    val groupId: String = "",
    val payerMemberId: String = "",
    val amountMinor: Long = 0L,
    val currency: String = "EUR",

    val note: String? = null,

    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val deleted: Boolean = false
)
