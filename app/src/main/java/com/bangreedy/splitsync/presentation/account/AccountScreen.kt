package com.bangreedy.splitsync.presentation.account

import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import org.koin.androidx.compose.koinViewModel

@Composable
fun AccountScreen(
    vm: AccountViewModel = koinViewModel()
) {
    val userProfile by vm.userProfile.collectAsState()
    val scrollState = rememberScrollState()

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            vm.updateProfilePhoto(uri)
        }
    }

    var showEditNameDialog by remember { mutableStateOf(false) }
    var showEditUsernameDialog by remember { mutableStateOf(false) }
    var showCurrencyDialog by remember { mutableStateOf(false) }

    if (showEditNameDialog) {
        val currentName = userProfile?.displayName.orEmpty()
        EditNameDialog(
            initialName = currentName,
            onDismiss = { showEditNameDialog = false },
            onConfirm = { newName ->
                vm.updateDisplayName(newName)
                showEditNameDialog = false
            }
        )
    }

    if (showEditUsernameDialog) {
        val currentUsername = userProfile?.username.orEmpty()
        EditUsernameDialog(
            initialUsername = currentUsername,
            onDismiss = { showEditUsernameDialog = false },
            onConfirm = { newUsername, onSuccess, onError ->
                vm.updateUsername(newUsername, onSuccess, onError)
            }
        )
    }

    if (showCurrencyDialog) {
        CurrencyDialog(
            onDismiss = { showCurrencyDialog = false },
            onSelect = { currency ->
                vm.updateDefaultCurrency(currency)
                showCurrencyDialog = false
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        userProfile?.let { profile ->
            // Profile Section
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .clickable {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (profile.photoUrl != null) {
                        AsyncImage(
                            model = profile.photoUrl,
                            contentDescription = "Profile Photo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Text(
                            text = profile.displayName.take(1).uppercase(),
                            style = MaterialTheme.typography.displayMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    // Overlay for edit indication
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.3f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Photo",
                            tint = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = profile.displayName,
                        style = MaterialTheme.typography.titleLarge
                    )
                    IconButton(onClick = { showEditNameDialog = true }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Name")
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "@${profile.username}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    IconButton(
                        onClick = { showEditUsernameDialog = true },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit Username",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (profile.email != null) {
                    Text(
                        text = profile.email,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider()

            // Settings Section
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                SettingItem(
                    title = "Default Currency",
                    subtitle = profile.defaultCurrency,
                    onClick = { showCurrencyDialog = true }
                )

                Text(
                    text = "Notifications",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(top = 8.dp)
                )

                // Master Toggles
                SwitchSetting(
                    title = "Push Notifications",
                    checked = profile.notificationPrefs.pushEnabled,
                    onCheckedChange = { checked ->
                        vm.updateNotificationPrefs(profile.notificationPrefs.copy(pushEnabled = checked))
                    }
                )
                SwitchSetting(
                    title = "Email Notifications",
                    checked = profile.notificationPrefs.emailEnabled,
                    onCheckedChange = { checked ->
                        vm.updateNotificationPrefs(profile.notificationPrefs.copy(emailEnabled = checked))
                    }
                )
            }

            HorizontalDivider()

            // Actions
            OutlinedButton(
                onClick = { vm.signOut() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Sign Out")
            }
        }
    }
}

@Composable
fun SettingItem(title: String, subtitle: String? = null, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun SwitchSetting(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = title, style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun EditNameDialog(
    initialName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Display Name") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Display Name") },
                singleLine = true
            )
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name) },
                enabled = name.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun EditUsernameDialog(
    initialUsername: String,
    onDismiss: () -> Unit,
    onConfirm: (String, () -> Unit, (String) -> Unit) -> Unit
) {
    var username by remember { mutableStateOf(initialUsername) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Change Username") },
        text = {
            Column {
                OutlinedTextField(
                    value = username,
                    onValueChange = {
                        username = it
                        error = null
                    },
                    label = { Text("Username") },
                    singleLine = true,
                    isError = error != null,
                    supportingText = {
                        if (error != null) Text(error!!)
                    }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    loading = true
                    onConfirm(username, {
                        loading = false
                        onDismiss()
                    }, { msg ->
                        loading = false
                        error = msg
                    })
                },
                enabled = username.isNotBlank() && !loading
            ) {
                if (loading) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Text("Save")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun CurrencyDialog(
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    var search by remember { mutableStateOf("") }
    val all = com.bangreedy.splitsync.core.currency.CurrencyMeta.supportedCurrencies
    val filtered = remember(search) {
        if (search.isBlank()) all.entries.toList()
        else all.entries.filter { (code, name) ->
            code.contains(search, ignoreCase = true) ||
                    name.contains(search, ignoreCase = true)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Currency") },
        text = {
            Column {
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    placeholder = { Text("Search…") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Column(
                    Modifier
                        .heightIn(max = 350.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    filtered.forEach { (code, name) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(code) }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(code, style = MaterialTheme.typography.bodyLarge)
                            Spacer(Modifier.width(12.dp))
                            Text(
                                name,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
