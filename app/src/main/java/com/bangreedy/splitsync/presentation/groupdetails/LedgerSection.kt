package com.bangreedy.splitsync.presentation.groupdetails

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bangreedy.splitsync.core.money.formatMinor
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun LedgerSection(groupId: String,
                  onSettleSuggestion: (fromId: String, toId: String, amountMinor: Long) -> Unit) {
    val vm: LedgerViewModel = koinViewModel(parameters = { parametersOf(groupId) })
    val state by vm.state.collectAsState()
    val currency = state.expenses.firstOrNull()?.currency ?: "EUR"
    val userDefaultCurrency = state.userDefaultCurrency

    fun nameOf(memberId: String): String =
        state.members.firstOrNull { it.uid == memberId }?.displayName ?: "Unknown"

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Balances", style = MaterialTheme.typography.titleMedium)
        state.members.forEach { m ->
            val bal = state.balances[m.uid] ?: 0L
            Text("${m.displayName}: ${formatMinor(bal, currency)}")
        }
        Spacer(Modifier.height(8.dp))
        Text("Suggested settlements", style = MaterialTheme.typography.titleMedium)

        if (state.suggestions.isEmpty()) {
            Text("Everyone is settled up 🎉", style = MaterialTheme.typography.bodyMedium)
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                state.suggestions.forEach { s ->
                    Text(
                        text = "${nameOf(s.fromMemberId)} pays ${nameOf(s.toMemberId)} ${formatMinor(s.amountMinor, currency)}",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSettleSuggestion(s.fromMemberId, s.toMemberId, s.amountMinor) }
                            .padding(vertical = 6.dp)
                    )
                }

            }
        }

        Spacer(Modifier.height(8.dp))
        Text("Expenses", style = MaterialTheme.typography.titleMedium)

        LazyColumn(modifier = Modifier.heightIn(max = 320.dp)) {
            items(state.expenses, key = { it.id }) { e ->
                val payerName = state.members.firstOrNull { it.uid == e.payerMemberId }?.displayName ?: "Unknown"
                Column {
                    Text("$payerName paid ${formatMinor(e.amountMinor, e.currency)}")
                    e.note?.let { Text(it, style = MaterialTheme.typography.bodySmall) }

                    // Show converted amount if different currency
                    if (!e.currency.equals(userDefaultCurrency, ignoreCase = true)) {
                        val conversion = state.conversions[e.id]
                        if (conversion != null) {
                            val sourceLabel = when (conversion.source) {
                                com.bangreedy.splitsync.domain.model.FxSource.Remote -> "(live rate)"
                                com.bangreedy.splitsync.domain.model.FxSource.Cache -> "(cached)"
                            }
                            Text(
                                "≈ ${formatMinor(conversion.convertedMinor, userDefaultCurrency)} $sourceLabel",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
            }
        }
    }
}
