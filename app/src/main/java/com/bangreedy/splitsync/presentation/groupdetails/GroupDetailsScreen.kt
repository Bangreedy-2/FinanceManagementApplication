package com.bangreedy.splitsync.presentation.groupdetails

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bangreedy.splitsync.core.money.formatMinor
import com.bangreedy.splitsync.domain.model.FxSource
import com.bangreedy.splitsync.presentation.invites.InviteUserDialog
import com.bangreedy.splitsync.presentation.ui.components.ProfileImage
import com.google.firebase.auth.FirebaseAuth
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupDetailsScreen(
    groupId: String,
    onAddExpense: () -> Unit
) {
    val vm: GroupDetailsViewModel = koinViewModel(parameters = { parametersOf(groupId) })
    val state by vm.state.collectAsState()

    val ledgerVm: LedgerViewModel = koinViewModel(parameters = { parametersOf(groupId) })
    val ledgerState by ledgerVm.state.collectAsState()

    val groupName = state.group?.name ?: "Group"
    val myUid = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    var showInvite by remember { mutableStateOf(false) }

    // Settle dialog
    if (ledgerState.showSettleDialog) {
        val targetName = ledgerState.members
            .firstOrNull { it.uid == ledgerState.settleTargetUid }
            ?.displayName ?: "Member"
        GroupSettleUpDialog(
            targetName = targetName,
            debtBuckets = ledgerState.settleTargetBuckets,
            defaultCurrency = ledgerState.userDefaultCurrency,
            isExecuting = ledgerState.isExecutingPlan,
            error = ledgerState.settleError,
            onDismiss = { ledgerVm.dismissSettleDialog() },
            onConfirm = { selected, payCurrency ->
                ledgerVm.executeSettlePlan(selected, payCurrency)
            }
        )
    }

    // Invite dialog
    if (showInvite && myUid.isNotBlank()) {
        InviteUserDialog(
            groupId = groupId,
            groupName = groupName,
            inviterUid = myUid,
            inviterDisplayName = null,
            onDismiss = { showInvite = false }
        )
    }

    Scaffold(
        topBar = {
            androidx.compose.material3.TopAppBar(
                title = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        state.group?.let { group ->
                            ProfileImage(
                                photoUrl = group.photoUrl,
                                displayName = group.name,
                                size = 40.dp,
                                isGroup = true,
                                onPhotoSelected = { uri -> vm.updateGroupPhoto(uri) }
                            )
                        }
                        Text(state.group?.name ?: "Group Details")
                    }
                },
                actions = {
                    IconButton(onClick = { showInvite = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Invite Member")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // ── Action buttons ──
            item {
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onAddExpense) { Text("Add Expense") }
                    OutlinedButton(
                        enabled = myUid.isNotBlank(),
                        onClick = { showInvite = true }
                    ) { Text("Invite") }
                }
            }

            // ── Members ──
            item {
                Spacer(Modifier.height(4.dp))
                Text("Members", style = MaterialTheme.typography.titleMedium)
            }
            items(state.members, key = { it.uid }) { m ->
                Column {
                    Text(m.displayName, style = MaterialTheme.typography.bodyLarge)
                    m.email?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // ── Balances ──
            item {
                Spacer(Modifier.height(4.dp))
                HorizontalDivider()
                Spacer(Modifier.height(4.dp))
                Text("Balances", style = MaterialTheme.typography.titleMedium)
            }
            item {
                BalancesSection(
                    state = ledgerState,
                    myUid = myUid
                )
            }

            // ── Settle Up ──
            item {
                Spacer(Modifier.height(4.dp))
                HorizontalDivider()
                Spacer(Modifier.height(4.dp))
                Text("Settle Up", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                SettleUpSection(
                    state = ledgerState,
                    myUid = myUid,
                    onSettleWith = { targetUid -> ledgerVm.openSettleFor(targetUid) }
                )
            }

            // ── Expenses ──
            item {
                Spacer(Modifier.height(4.dp))
                HorizontalDivider()
                Spacer(Modifier.height(4.dp))
                Text("Expenses", style = MaterialTheme.typography.titleMedium)
            }

            if (ledgerState.expenses.isEmpty()) {
                item {
                    Text("No expenses yet", style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                items(ledgerState.expenses, key = { it.id }) { e ->
                    val payerName = ledgerState.members
                        .firstOrNull { it.uid == e.payerMemberId }?.displayName ?: "Unknown"
                    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Text(
                            "$payerName paid ${formatMinor(e.amountMinor, e.currency)}",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        e.note?.let {
                            Text(it, style = MaterialTheme.typography.bodySmall)
                        }

                        // Converted amount
                        if (!e.currency.equals(ledgerState.userDefaultCurrency, ignoreCase = true)) {
                            val conversion = ledgerState.conversions[e.id]
                            if (conversion != null) {
                                val sourceLabel = when (conversion.source) {
                                    FxSource.Remote -> "(live rate)"
                                    FxSource.Cache -> "(cached)"
                                }
                                Text(
                                    "≈ ${formatMinor(conversion.convertedMinor, ledgerState.userDefaultCurrency)} $sourceLabel",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                    }
                }
            }

            // ── Payments ──
            if (ledgerState.payments.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(4.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(4.dp))
                    Text("Payments", style = MaterialTheme.typography.titleMedium)
                }
                items(ledgerState.payments, key = { it.id }) { p ->
                    val fromName = ledgerState.members
                        .firstOrNull { it.uid == p.fromMemberId }?.displayName ?: p.fromMemberId.take(8)
                    val toName = ledgerState.members
                        .firstOrNull { it.uid == p.toMemberId }?.displayName ?: p.toMemberId.take(8)
                    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Text(
                            "$fromName paid $toName ${formatMinor(p.amountMinor, p.currency)}",
                            style = MaterialTheme.typography.bodyLarge
                        )

                        // Converted amount
                        if (!p.currency.equals(ledgerState.userDefaultCurrency, ignoreCase = true)) {
                            val conversion = ledgerState.conversions[p.id]
                            if (conversion != null) {
                                val sourceLabel = when (conversion.source) {
                                    FxSource.Remote -> "(live rate)"
                                    FxSource.Cache -> "(cached)"
                                }
                                Text(
                                    "≈ ${formatMinor(conversion.convertedMinor, ledgerState.userDefaultCurrency)} $sourceLabel",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                    }
                }
            }

            // Bottom padding
            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}
