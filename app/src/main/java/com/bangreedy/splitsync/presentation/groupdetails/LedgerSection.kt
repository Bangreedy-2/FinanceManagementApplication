package com.bangreedy.splitsync.presentation.groupdetails

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bangreedy.splitsync.core.money.formatMinor
import com.bangreedy.splitsync.domain.model.TotalInDefault
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Balances section showing multi-currency per-member balances.
 * Does NOT own a ViewModel — receives state from parent.
 */
@Composable
fun BalancesSection(
    state: LedgerUiState,
    myUid: String
) {
    if (state.members.isEmpty()) {
        Text("No members yet", style = MaterialTheme.typography.bodyMedium)
        return
    }

    val hasAnyBalance = state.balancesByCurrency.values.any { currMap ->
        currMap.values.any { it != 0L }
    }

    if (!hasAnyBalance && state.expenses.isEmpty()) {
        Text("No expenses yet", style = MaterialTheme.typography.bodyMedium)
        return
    }

    state.members.forEach { m ->
        val currMap = state.balancesByCurrency[m.uid]
        val total = state.memberTotals[m.uid]
        val hasBalance = currMap?.values?.any { it != 0L } == true

        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(m.displayName, style = MaterialTheme.typography.bodyLarge)

                if (total != null && hasBalance) {
                    TotalBadge(total)
                } else {
                    Text(
                        "settled",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Per-currency lines
            if (currMap != null && hasBalance) {
                currMap.filter { it.value != 0L }.forEach { (currency, bal) ->
                    val sign = if (bal > 0) "+" else ""
                    Text(
                        "  $sign${formatMinor(bal, currency)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (bal > 0) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.error
                    )
                }
                // Show "last updated" if approx
                total?.lastUpdatedAtMillis?.let { millis ->
                    val sdf = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
                    Text(
                        "  rates: ${sdf.format(Date(millis))}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * Settle-up section: one button per other member who has a non-zero pairwise debt.
 */
@Composable
fun SettleUpSection(
    state: LedgerUiState,
    myUid: String,
    onSettleWith: (targetUid: String) -> Unit
) {
    val otherMembers = state.members.filter { it.uid != myUid }

    if (otherMembers.isEmpty()) {
        Text("No other members", style = MaterialTheme.typography.bodyMedium)
        return
    }

    otherMembers.forEach { m ->
        val key = if (myUid < m.uid) Pair(myUid, m.uid) else Pair(m.uid, myUid)
        val pairCurrMap = state.pairwise[key]
        val hasDebt = pairCurrMap?.values?.any { it != 0L } == true

        OutlinedButton(
            onClick = { onSettleWith(m.uid) },
            enabled = hasDebt,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                if (hasDebt) "Settle with ${m.displayName}"
                else "Settled with ${m.displayName} ✓"
            )
        }
    }
}

@Composable
private fun TotalBadge(total: TotalInDefault) {
    val approxPrefix = if (total.isApprox) "≈ " else ""
    val label = when {
        total.amountMinor > 0 -> "+$approxPrefix${formatMinor(total.amountMinor, total.currency)}"
        total.amountMinor < 0 -> "$approxPrefix${formatMinor(total.amountMinor, total.currency)}"
        else -> "settled"
    }
    val color = when {
        total.amountMinor > 0 -> MaterialTheme.colorScheme.primary
        total.amountMinor < 0 -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurface
    }
    Text(label, style = MaterialTheme.typography.bodyMedium, color = color)
}
