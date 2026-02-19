package com.bangreedy.splitsync.presentation.settleup

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.bangreedy.splitsync.core.money.formatMinor
import com.bangreedy.splitsync.domain.model.Member
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun SettleUpScreen(
    groupId: String,
    initialFromId: String,
    initialToId: String,
    initialAmountMinor: Long,
    onBack: () -> Unit
) {
    val vm: SettleUpViewModel = koinViewModel(
        parameters = { parametersOf(groupId, initialFromId, initialToId, initialAmountMinor) }
    )
    val state by vm.state.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Settle Up", style = MaterialTheme.typography.headlineSmall)

        PersonPicker(
            label = "From",
            members = state.members,
            selectedId = state.fromId,
            onPick = vm::onFromSelected
        )
        PersonPicker(
            label = "To",
            members = state.members,
            selectedId = state.toId,
            onPick = vm::onToSelected
        )

        Text(
            text = "Suggested: ${formatMinor(state.suggestedAmountMinor, state.currency)}",
            style = MaterialTheme.typography.bodyMedium
        )

        OutlinedTextField(
            value = state.amountText,
            onValueChange = vm::onAmountChange,
            label = { Text("Amount") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f)) { Text("Cancel") }
            Button(
                onClick = { vm.save(onBack) },
                enabled = !state.isSaving,
                modifier = Modifier.weight(1f)
            ) { Text(if (state.isSaving) "Saving..." else "Confirm") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PersonPicker(
    label: String,
    members: List<Member>,
    selectedId: String,
    onPick: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedName = members.firstOrNull { it.uid == selectedId }?.displayName ?: "Select"

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selectedName,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            members.forEach { m ->
                DropdownMenuItem(
                    text = { Text(m.displayName) },
                    onClick = {
                        onPick(m.uid)
                        expanded = false
                    }
                )
            }
        }
    }
}
