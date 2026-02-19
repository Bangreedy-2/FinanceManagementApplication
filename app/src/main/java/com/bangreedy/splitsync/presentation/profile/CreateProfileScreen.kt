package com.bangreedy.splitsync.presentation.profile

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.koin.androidx.compose.koinViewModel

@Composable
fun CreateProfileScreen(
    uid: String,
    email: String?,
    onDone: () -> Unit,
    vm: CreateProfileViewModel = koinViewModel()
) {
    val state by vm.state.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Create your profile", style = MaterialTheme.typography.headlineSmall)

        OutlinedTextField(
            value = state.username,
            onValueChange = vm::onUsername,
            label = { Text("Username (unique)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        val statusText = when (state.usernameStatus) {
            UsernameStatus.Idle -> ""
            UsernameStatus.Invalid -> "Invalid (3–20 chars, letters/digits/_ only)"
            UsernameStatus.Checking -> "Checking..."
            UsernameStatus.Available -> "Available ✅"
            UsernameStatus.Taken -> "Taken ❌"
            UsernameStatus.Error -> "Could not check (try again)"
        }

        if (statusText.isNotBlank()) {
            Text(
                text = statusText,
                color = when (state.usernameStatus) {
                    UsernameStatus.Available -> MaterialTheme.colorScheme.primary
                    UsernameStatus.Taken, UsernameStatus.Invalid, UsernameStatus.Error -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }


        OutlinedTextField(
            value = state.displayName,
            onValueChange = vm::onDisplayName,
            label = { Text("Display name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )


        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        val canContinue =
            state.usernameStatus == UsernameStatus.Available &&
                    state.displayName.trim().isNotBlank() &&
                    !state.isSaving

        Button(
            onClick = { vm.save(uid, email, onDone) },
            enabled = canContinue,
            modifier = Modifier.fillMaxWidth()
        )
 {
            Text(if (state.isSaving) "Saving..." else "Continue")
        }

        Text("Username: 3–20 chars, letters/digits/_", style = MaterialTheme.typography.bodySmall)
    }
}
