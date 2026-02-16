package com.bangreedy.splitsync.presentation.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import org.koin.androidx.compose.koinViewModel

@Composable
fun AuthScreen(vm: AuthViewModel = koinViewModel()) {
    val state by vm.state.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("SplitSync", style = MaterialTheme.typography.headlineLarge)
        Text("Sign in or create an account", style = MaterialTheme.typography.bodyMedium)

        OutlinedTextField(
            value = state.email,
            onValueChange = vm::onEmailChange,
            label = { Text("Email") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = state.password,
            onValueChange = vm::onPasswordChange,
            label = { Text("Password") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = vm::signIn,
                enabled = !state.isLoading,
                modifier = Modifier.weight(1f)
            ) { Text(if (state.isLoading) "..." else "Sign In") }

            Button(
                onClick = vm::signUp,
                enabled = !state.isLoading,
                modifier = Modifier.weight(1f)
            ) { Text(if (state.isLoading) "..." else "Sign Up") }
        }

        Text("Password must be at least 6 characters.", style = MaterialTheme.typography.bodySmall)
    }
}
