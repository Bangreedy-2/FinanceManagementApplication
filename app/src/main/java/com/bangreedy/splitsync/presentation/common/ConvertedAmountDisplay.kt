package com.bangreedy.splitsync.presentation.common

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import com.bangreedy.splitsync.core.money.formatInstantAsDate
import com.bangreedy.splitsync.core.money.formatMinor
import com.bangreedy.splitsync.domain.model.ConversionResult
import com.bangreedy.splitsync.domain.model.FxSource

/**
 * Displays a single converted amount.
 * If conversion is stale (cached), shows "last updated" timestamp.
 */
@Composable
fun ConvertedAmountDisplay(
    convertedMinor: Long,
    targetCurrency: String,
    conversionResult: ConversionResult?,
    modifier: Modifier = Modifier,
    style: TextStyle = TextStyle(fontSize = 12.sp),
) {
    val formatted = formatMinor(convertedMinor, targetCurrency)
    val isCached = conversionResult?.source == FxSource.Cache
    val lastUpdated = if (isCached && conversionResult != null) {
        " (last updated ${formatInstantAsDate(conversionResult.fetchedAt)})"
    } else {
        ""
    }

    Text(
        text = formatted + lastUpdated,
        style = style,
        modifier = modifier
    )
}

/**
 * Displays original amount on top, converted amount below (for list items).
 */
@Composable
fun OriginalAndConvertedAmount(
    originalMinor: Long,
    originalCurrency: String,
    convertedMinor: Long,
    targetCurrency: String,
    conversionResult: ConversionResult?,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = formatMinor(originalMinor, originalCurrency),
            style = TextStyle(fontSize = 14.sp)
        )
        if (conversionResult != null) {
            val converted = formatMinor(convertedMinor, targetCurrency)
            val isCached = conversionResult.source == FxSource.Cache
            val lastUpdated = if (isCached) "\n(last updated ${formatInstantAsDate(conversionResult.fetchedAt)})" else ""

            Text(
                text = "≈ $converted$lastUpdated",
                style = TextStyle(fontSize = 12.sp)
            )
        }
    }
}


