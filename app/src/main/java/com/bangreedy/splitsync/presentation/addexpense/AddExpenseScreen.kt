package com.bangreedy.splitsync.presentation.addexpense

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.bangreedy.splitsync.domain.model.Member
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun AddExpenseScreen(
    groupId: String,
    onBack: () -> Unit
) {
    val vm: AddExpenseViewModel = koinViewModel(parameters = { parametersOf(groupId) })
    val state by vm.state.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Add Expense", style = MaterialTheme.typography.headlineSmall)

        OutlinedTextField(
            value = state.amountText,
            onValueChange = vm::onAmountChange,
            label = { Text("Amount") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = state.note,
            onValueChange = vm::onNoteChange,
            label = { Text("Note (optional)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        PayerPicker(
            members = state.members,
            payerId = state.payerMemberId,
            onPick = vm::onPayerSelected
        )

        Text("Participants", style = MaterialTheme.typography.titleMedium)

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(state.members, key = { it.id }) { m ->
                ParticipantRow(
                    member = m,
                    checked = m.id in state.participantIds,
                    onToggle = { vm.onToggleParticipant(m.id) }
                )
            }
        }

        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f)) { Text("Cancel") }
            Button(
                onClick = { vm.save(onDone = onBack) },
                enabled = !state.isSaving,
                modifier = Modifier.weight(1f)
            ) { Text(if (state.isSaving) "Saving..." else "Save") }
        }
    }
}

@Composable
private fun ParticipantRow(member: Member, checked: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(member.displayName, style = MaterialTheme.typography.bodyLarge)
            member.email?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
        }
        Checkbox(checked = checked, onCheckedChange = { onToggle() })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PayerPicker(
    members: List<Member>,
    payerId: String?,
    onPick: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val payerName = members.firstOrNull { it.id == payerId }?.displayName ?: "Select payer"

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = payerName,
            onValueChange = {},
            readOnly = true,
            label = { Text("Paid by") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            members.forEach { m ->
                DropdownMenuItem(
                    text = { Text(m.displayName) },
                    onClick = {
                        onPick(m.id)
                        expanded = false
                    }
                )
            }
        }
    }
}
