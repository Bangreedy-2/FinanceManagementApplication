package com.bangreedy.splitsync.domain.model

data class Settlement(
    val fromMemberId: String,
    val toMemberId: String,
    val amountMinor: Long
)
