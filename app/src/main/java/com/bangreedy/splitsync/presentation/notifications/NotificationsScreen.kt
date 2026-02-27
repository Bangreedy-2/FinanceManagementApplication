package com.bangreedy.splitsync.presentation.notifications

import androidx.compose.foundation.clickable
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
fun NotificationsScreen(uid: String) {
    val vm: NotificationsViewModel = koinViewModel(parameters = { parametersOf(uid) })
    val state by vm.state.collectAsState()

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Notifications", style = MaterialTheme.typography.headlineSmall)
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        LazyColumn {
            items(state.items, key = { it.id }) { n ->
                Card(
                    Modifier
                        .fillMaxWidth()
                        .clickable { vm.onOpen(n.id) }
                ) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(n.title, style = MaterialTheme.typography.titleMedium)
                        if (n.body.isNotBlank()) Text(n.body)
                        if (!n.read) Text("Unread", color = MaterialTheme.colorScheme.primary)
                    }
                }
                Spacer(Modifier.height(10.dp))
            }
        }
    }
}