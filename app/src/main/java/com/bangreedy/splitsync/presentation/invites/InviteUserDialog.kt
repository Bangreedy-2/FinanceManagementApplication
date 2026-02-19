package com.bangreedy.splitsync.presentation.invites

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import org.koin.androidx.compose.koinViewModel

@Composable
fun InviteUserDialog(
    groupId: String,
    groupName: String,
    inviterUid: String,
    inviterDisplayName: String?,
    onDismiss: () -> Unit,
    vm: SendInviteViewModel = koinViewModel()
) {
    val state by vm.state.collectAsState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Invite user") },
        text = {
            Column {
                OutlinedTextField(
                    value = state.input,
                    onValueChange = vm::onInput,
                    label = { Text("Username or email") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                )
                state.error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            Button(
                enabled = !state.isSending && state.input.trim().isNotBlank(),
                onClick = {
                    vm.send(
                        groupId = groupId,
                        groupName = groupName,
                        inviterUid = inviterUid,
                        inviterName = inviterDisplayName,
                        onDone = onDismiss
                    )
                }
            ) {
                Text(if (state.isSending) "Sending..." else "Send")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
