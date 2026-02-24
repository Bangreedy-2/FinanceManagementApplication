package com.bangreedy.splitsync.domain.model

@JvmInline
value class MoneyMinor(val value: Long) {
    init { require(value >= 0) { "MoneyMinor must be >= 0" } }
}
