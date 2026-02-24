package com.bangreedy.splitsync.data.remote.dto

data class FirestoreExpenseSplit(
    val expenseId: String = "",
    val memberId: String = "",
    val owedMinor: Long = 0L
)
