package com.bangreedy.splitsync.presentation.groups

import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.koin.androidx.compose.koinViewModel
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox

@Composable
fun GroupsScreen(
    onGroupClick: (String) -> Unit,
    onInvitesClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    vm: GroupsViewModel = koinViewModel()
) {
    val state by vm.state.collectAsState()

    var name by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Groups", style = MaterialTheme.typography.headlineSmall)

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onInvitesClick) { Text("Invites") }

                OutlinedButton(onClick = onNotificationsClick) {
                    val unread = state.unreadNotifications
                    if (unread > 0) {
                        BadgedBox(
                            badge = { Badge { Text(unread.toString()) } }
                        ) {
                            Text("Notifications")
                        }
                    } else {
                        Text("Notifications")
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                modifier = Modifier.weight(1f),
                value = name,
                onValueChange = { name = it },
                label = { Text("New group name") },
                singleLine = true
            )
            Button(
                onClick = {
                    vm.onCreateGroup(name)
                    name = ""
                }
            ) {
                Text("Add")
            }
        }

        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(state.groups, key = { it.id }) { g ->
                Text(
                    text = g.name,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onGroupClick(g.id) }
                        .padding(vertical = 10.dp)
                )
                HorizontalDivider()
            }
        }
    }
}
