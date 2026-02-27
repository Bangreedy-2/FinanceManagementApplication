package com.bangreedy.splitsync.presentation.groupdetails

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bangreedy.splitsync.core.money.formatMinor
import com.bangreedy.splitsync.domain.model.DebtBucket
import com.bangreedy.splitsync.presentation.common.CurrencyPickerField
import com.bangreedy.splitsync.presentation.common.isValidCurrency
import kotlin.math.abs

/**
 * Settlement dialog for group: select which currency debts to settle with one person.
 */
@Composable
fun GroupSettleUpDialog(
    targetName: String,
    debtBuckets: List<DebtBucket>,
    defaultCurrency: String,
    isExecuting: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onConfirm: (selectedBuckets: List<DebtBucket>, payCurrency: String) -> Unit
) {
    val selectedState = remember(debtBuckets) {
        mutableStateMapOf<String, Boolean>().apply {
            debtBuckets.forEach { put(bucketKey(it), true) }
        }
    }
    var payCurrency by remember { mutableStateOf(defaultCurrency) }
    val payCurrencyValid = isValidCurrency(payCurrency)

    val selectedBuckets = remember(debtBuckets, selectedState.toMap()) {
        debtBuckets.filter { selectedState[bucketKey(it)] == true }
    }

    val allSelected = selectedBuckets.size == debtBuckets.size
    val noneSelected = selectedBuckets.isEmpty()

    AlertDialog(
        onDismissRequest = { if (!isExecuting) onDismiss() },
        title = { Text("Settle with $targetName") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (debtBuckets.isEmpty()) {
                    Text("No debts to settle", style = MaterialTheme.typography.bodyMedium)
                } else {
                    // Select all
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = allSelected,
                            onCheckedChange = { checked ->
                                debtBuckets.forEach { selectedState[bucketKey(it)] = checked }
                            }
                        )
                        Text("Select all", style = MaterialTheme.typography.bodyMedium)
                    }

                    HorizontalDivider()

                    // Each debt bucket
                    debtBuckets.forEach { bucket ->
                        val key = bucketKey(bucket)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Checkbox(
                                checked = selectedState[key] == true,
                                onCheckedChange = { selectedState[key] = it }
                            )
                            val absAmount = abs(bucket.netMinor)
                            val dirText = if (bucket.netMinor < 0)
                                "You owe ${formatMinor(absAmount, bucket.currency)}"
                            else
                                "Owes you ${formatMinor(absAmount, bucket.currency)}"
                            Text(
                                dirText,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (bucket.netMinor < 0)
                                    MaterialTheme.colorScheme.error
                                else
                                    MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    HorizontalDivider()

                    // Pay currency
                    Text("Pay in:", style = MaterialTheme.typography.labelMedium)
                    CurrencyPickerField(
                        value = payCurrency,
                        onCurrencyChanged = { payCurrency = it }
                    )

                    // Preview
                    if (selectedBuckets.isNotEmpty()) {
                        Spacer(Modifier.height(4.dp))
                        Text("This will settle:", style = MaterialTheme.typography.labelMedium)
                        selectedBuckets.forEach { b ->
                            Text(
                                "  ${formatMinor(abs(b.netMinor), b.currency)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        val needsFx = selectedBuckets.any {
                            !it.currency.equals(payCurrency, ignoreCase = true)
                        }
                        if (needsFx) {
                            Text(
                                "FX rates will be locked at current values",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    error?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(selectedBuckets, payCurrency) },
                enabled = !noneSelected && payCurrencyValid && !isExecuting && debtBuckets.isNotEmpty()
            ) {
                if (isExecuting) {
                    CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                }
                Text(if (allSelected) "Settle All" else "Settle Selected")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isExecuting) { Text("Cancel") }
        }
    )
}

private fun bucketKey(b: DebtBucket): String =
    "${b.contextType}|${b.contextId}|${b.currency}"

