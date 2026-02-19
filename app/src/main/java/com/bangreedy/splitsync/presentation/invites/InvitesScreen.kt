package com.bangreedy.splitsync.presentation.invites

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun InvitesScreen(
    myUid: String
) {
    val vm: InvitesViewModel = koinViewModel(parameters = { parametersOf(myUid) })
    val state by vm.state.collectAsState()

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Invites", style = MaterialTheme.typography.headlineSmall)
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        LazyColumn {
            items(state.invites, key = { it.inviteId }) { inv ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(inv.groupName.ifBlank { "Group" }, style = MaterialTheme.typography.titleMedium)
                        val inviterName = state.inviterNames[inv.inviterUid]
                        Text("From: ${inviterName ?: "Unknown user"}")
                        if (inviterName == null) {
                            Text(inv.inviterUid, style = MaterialTheme.typography.bodySmall)
                        }

                        Text("Status: ${inv.status}")

                        if (inv.status == "pending") {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = { vm.accept(inv.inviteId, inv.groupId) }) { Text("Accept") }
                                OutlinedButton(onClick = { vm.decline(inv.inviteId) }) { Text("Decline") }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
            }
        }
    }
}
