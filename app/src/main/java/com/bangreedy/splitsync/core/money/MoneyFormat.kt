package com.bangreedy.splitsync.core.money

import com.bangreedy.splitsync.core.currency.CurrencyMeta
import com.bangreedy.splitsync.domain.model.ConversionResult
import com.bangreedy.splitsync.domain.model.FxSource
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.abs

/**
 * Format a centified amount for display.
 *
 * IMPORTANT: every [amountMinor] in the app is stored as "value × 100"
 * regardless of currency (parseToMinorUnits always multiplies by 100).
 *
 * We therefore always divide by 100 first to get the major amount, then
 * decide how many decimal places to show based on the currency:
 *   • 0-decimal currencies (JPY, KRW …) → show integer, no decimals
 *   • 2-decimal currencies (USD, EUR …) → show 2 decimals (the cents we stored)
 *   • 3-decimal currencies (BHD, KWD …) → show 2 decimals (we only have cents precision)
 */
fun formatMinor(amountMinor: Long, currency: String): String {
    val sign = if (amountMinor < 0) "-" else ""
    val absVal = abs(amountMinor)
    val displayDecimals = CurrencyMeta.getMinorUnits(currency)

    // Storage is always centified (÷100)
    val major = absVal / 100
    val cents = absVal % 100          // 0‑99

    return when {
        displayDecimals == 0 -> {
            // For zero-decimal currencies round to nearest whole unit
            val rounded = if (cents >= 50) major + 1 else major
            "$sign${currency.uppercase()} $rounded"
        }
        else -> {
            // Show exactly 2 decimal places (our storage precision)
            val centsStr = cents.toString().padStart(2, '0')
            "$sign${currency.uppercase()} $major.$centsStr"
        }
    }
}

/**
 * Format an amount with conversion details.
 * Shows original amount and converted amount with metadata.
 * Note: You need to pass the target currency code separately.
 */
fun formatWithConversion(
    originalMinor: Long,
    originalCurrency: String,
    conversionResult: ConversionResult?,
    targetCurrency: String? = null
): String {
    val original = formatMinor(originalMinor, originalCurrency)

    if (conversionResult == null) {
        return original
    }

    val target = targetCurrency ?: conversionResult.convertedMinor.toString()
    val converted = formatMinor(conversionResult.convertedMinor, target)

    return if (conversionResult.source == FxSource.Cache) {
        // Show "last updated" for stale cached rates
        val lastUpdated = formatInstantAsDate(conversionResult.fetchedAt)
        "$original ≈ $converted (last updated $lastUpdated)"
    } else {
        // Remote/fresh rate
        "$original ≈ $converted"
    }
}

/**
 * Format instant as "Feb 22, 2026" or "Feb 22 18:45" depending on detail level.
 */
fun formatInstantAsDate(instant: Instant, detailed: Boolean = false): String {
    val dateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault())
    return if (detailed) {
        DateTimeFormatter.ofPattern("MMM dd HH:mm").format(dateTime)
    } else {
        DateTimeFormatter.ofPattern("MMM dd, yyyy").format(dateTime)
    }
}
