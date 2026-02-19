package com.bangreedy.splitsync.presentation.groupdetails

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bangreedy.splitsync.presentation.invites.InviteUserDialog
import com.google.firebase.auth.FirebaseAuth
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun GroupDetailsScreen(
    groupId: String,
    onAddExpense: () -> Unit,
    onOpenSettleUp: () -> Unit,
    onSettleSuggestion: (fromId: String, toId: String, amountMinor: Long) -> Unit
) {
    val vm: GroupDetailsViewModel = koinViewModel(parameters = { parametersOf(groupId) })
    val state by vm.state.collectAsState()

    val groupName = state.group?.name ?: "Group"
    val inviterUid = FirebaseAuth.getInstance().currentUser?.uid

    // Optional: later we’ll pass inviter displayName from your profile (users/{uid})
    val inviterDisplayName: String? = null

    var showInvite by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = groupName,
            style = MaterialTheme.typography.headlineSmall
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onAddExpense) { Text("Add Expense") }
            OutlinedButton(onClick = onOpenSettleUp) { Text("Settle Up") }

            OutlinedButton(
                enabled = inviterUid != null,
                onClick = { showInvite = true }
            ) { Text("Invite") }
        }

        if (showInvite && inviterUid != null) {
            InviteUserDialog(
                groupId = groupId,
                groupName = groupName,
                inviterUid = inviterUid,
                inviterDisplayName = inviterDisplayName,
                onDismiss = { showInvite = false }
            )
        }

        Text("Members", style = MaterialTheme.typography.titleMedium)

        // ✅ Members list stays (now should represent accepted users)
        LazyColumn(modifier = Modifier.heightIn(max = 250.dp)) {
            items(state.members, key = { it.uid }) { m ->
                Text(m.displayName, style = MaterialTheme.typography.bodyLarge)
                m.email?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                Spacer(Modifier.height(10.dp))
            }
        }

        LedgerSection(
            groupId = groupId,
            onSettleSuggestion = onSettleSuggestion
        )
    }
}
