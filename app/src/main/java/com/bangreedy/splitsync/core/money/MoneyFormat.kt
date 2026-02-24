package com.bangreedy.splitsync.core.money

import kotlin.math.abs

fun formatMinor(amountMinor: Long, currency: String): String {
    val sign = if (amountMinor < 0) "-" else ""
    val absVal = abs(amountMinor)
    val major = absVal / 100
    val minor = absVal % 100
    return "$sign$currency $major.${minor.toString().padStart(2, '0')}"
}
