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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun GroupDetailsScreen(groupId: String, onAddExpense: () -> Unit)
 {
    val vm: GroupDetailsViewModel = koinViewModel(parameters = { parametersOf(groupId) })
    val state by vm.state.collectAsState()

    var memberName by remember { mutableStateOf("") }
    var memberEmail by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = state.group?.name ?: "Group",
            style = MaterialTheme.typography.headlineSmall
        )
        Button(onClick = onAddExpense) { Text("Add Expense") }


        Text("Members", style = MaterialTheme.typography.titleMedium)

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                modifier = Modifier.weight(1f),
                value = memberName,
                onValueChange = { memberName = it },
                label = { Text("Name") },
                singleLine = true
            )
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                modifier = Modifier.weight(1f),
                value = memberEmail,
                onValueChange = { memberEmail = it },
                label = { Text("Email (optional)") },
                singleLine = true
            )
            Button(onClick = {
                vm.onAddMember(memberName, memberEmail.takeIf { it.isNotBlank() })
                memberName = ""
                memberEmail = ""
            }) { Text("Add") }
        }

        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        Spacer(Modifier.height(8.dp))

        LazyColumn(modifier = Modifier.heightIn(max = 250.dp)) {
            items(state.members, key = { it.id }) { m ->
                Text(m.displayName, style = MaterialTheme.typography.bodyLarge)
                m.email?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                Spacer(Modifier.height(10.dp))
            }
        }
        LedgerSection(groupId = groupId)
    }
}
