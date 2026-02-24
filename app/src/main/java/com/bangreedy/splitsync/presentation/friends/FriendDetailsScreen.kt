package com.bangreedy.splitsync.presentation.friends

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.bangreedy.splitsync.core.money.formatMinor
import com.bangreedy.splitsync.domain.model.ActivitySource
import com.bangreedy.splitsync.domain.model.FriendActivityItem
import com.bangreedy.splitsync.domain.model.TotalInDefault
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendDetailsScreen(
    friendUid: String,
    onBack: () -> Unit,
    onNavigateToGroup: (String) -> Unit,
    vm: FriendDetailsViewModel = koinViewModel(parameters = { parametersOf(friendUid) })
) {
    val state by vm.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHostState.showSnackbar(it)
            vm.clearError()
        }
    }

    if (state.showAddExpense) {
        AddDirectExpenseDialog(
            onDismiss = { vm.toggleAddExpense() },
            onConfirm = { amount, currency, note, iPayForFriend ->
                vm.addDirectExpense(amount, currency, note, iPayForFriend)
            }
        )
    }

    if (state.showSettleUp) {
        DirectSettleUpDialog(
            onDismiss = { vm.toggleSettleUp() },
            onConfirm = { amount, currency, iPayFriend ->
                vm.settleUp(amount, currency, iPayFriend)
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(state.friendDisplayName.ifBlank { "Friend Details" })
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // Friend profile header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Avatar
                if (state.friendPhotoUrl != null) {
                    AsyncImage(
                        model = state.friendPhotoUrl,
                        contentDescription = "Profile photo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                    )
                } else {
                    Surface(
                        modifier = Modifier.size(64.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                state.friendDisplayName.take(1).uppercase().ifEmpty { "?" },
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                Spacer(Modifier.width(16.dp))

                Column {
                    Text(
                        state.friendDisplayName.ifBlank { "Loading…" },
                        style = MaterialTheme.typography.titleLarge
                    )
                    if (state.friendUsername.isNotBlank()) {
                        Text(
                            "@${state.friendUsername}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    state.friendEmail?.let { email ->
                        Text(
                            email,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(Modifier.height(12.dp))

            // Totals section
            val netByCurrency = state.activity.netByCurrency
            if (netByCurrency.isNotEmpty() || state.totalInDefault != null) {
                Text("Balance", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))

                // Total in default currency (headline)
                state.totalInDefault?.let { total ->
                    TotalInDefaultLine(total)
                    Spacer(Modifier.height(6.dp))
                }

                // Per-currency breakdown
                netByCurrency.forEach { (currency, net) ->
                    val label = when {
                        net > 0 -> "Owes you ${formatMinor(net, currency)}"
                        net < 0 -> "You owe ${formatMinor(-net, currency)}"
                        else -> "Settled up in $currency"
                    }
                    Text(
                        label,
                        style = MaterialTheme.typography.bodyMedium,
                        color = when {
                            net > 0 -> MaterialTheme.colorScheme.primary
                            net < 0 -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.onSurface
                        }
                    )
                }
                Spacer(Modifier.height(12.dp))
            } else {
                Text("No transactions yet", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(12.dp))
            }

            // Action buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(onClick = { vm.toggleAddExpense() }, modifier = Modifier.weight(1f)) {
                    Text("Add Expense")
                }
                OutlinedButton(onClick = { vm.toggleSettleUp() }, modifier = Modifier.weight(1f)) {
                    Text("Settle Up")
                }
            }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(Modifier.height(8.dp))

            Text("Activity", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))

            // Timeline
            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items(state.activity.items, key = { it.id }) { item ->
                    ActivityItemRow(
                        item = item,
                        onClick = {
                            when (val src = item.source) {
                                is ActivitySource.Group -> onNavigateToGroup(src.groupId)
                                is ActivitySource.Direct -> { /* already here */ }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ActivityItemRow(item: FriendActivityItem, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            when (item) {
                is FriendActivityItem.ExpenseItem -> {
                    Text(
                        "${item.payerName} paid ${formatMinor(item.amountMinor, item.currency)}",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    item.description?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall)
                    }
                }
                is FriendActivityItem.PaymentItem -> {
                    Text(
                        "${item.fromName} paid ${item.toName} ${formatMinor(item.amountMinor, item.currency)}",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Delta
                val deltaText = when {
                    item.pairwiseDeltaMinor > 0 -> "Owes you ${formatMinor(item.pairwiseDeltaMinor, item.currency)}"
                    item.pairwiseDeltaMinor < 0 -> "You owe ${formatMinor(-item.pairwiseDeltaMinor, item.currency)}"
                    else -> ""
                }
                Text(
                    deltaText,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (item.pairwiseDeltaMinor > 0) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.error
                )

                // Source label
                val sourceLabel = when (val src = item.source) {
                    is ActivitySource.Group -> "Group: ${src.groupName}"
                    is ActivitySource.Direct -> "Direct"
                }
                Text(
                    sourceLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun AddDirectExpenseDialog(
    onDismiss: () -> Unit,
    onConfirm: (amountMinor: Long, currency: String, note: String?, iPayForFriend: Boolean) -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    var currency by remember { mutableStateOf("USD") }
    var note by remember { mutableStateOf("") }
    var iPaid by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Expense") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Amount") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = currency,
                    onValueChange = { currency = it.uppercase() },
                    label = { Text("Currency") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note (optional)") },
                    singleLine = true
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Who paid?", modifier = Modifier.weight(1f))
                    FilterChip(
                        selected = iPaid,
                        onClick = { iPaid = true },
                        label = { Text("I paid") }
                    )
                    Spacer(Modifier.width(8.dp))
                    FilterChip(
                        selected = !iPaid,
                        onClick = { iPaid = false },
                        label = { Text("They paid") }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val minor = parseAmountToMinor(amountText)
                    if (minor != null && minor > 0) {
                        onConfirm(minor, currency, note.ifBlank { null }, iPaid)
                    }
                },
                enabled = amountText.isNotBlank()
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun DirectSettleUpDialog(
    onDismiss: () -> Unit,
    onConfirm: (amountMinor: Long, currency: String, iPayFriend: Boolean) -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    var currency by remember { mutableStateOf("USD") }
    var iPay by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Settle Up") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Amount") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = currency,
                    onValueChange = { currency = it.uppercase() },
                    label = { Text("Currency") },
                    singleLine = true
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Direction:", modifier = Modifier.weight(1f))
                    FilterChip(
                        selected = iPay,
                        onClick = { iPay = true },
                        label = { Text("I pay them") }
                    )
                    Spacer(Modifier.width(8.dp))
                    FilterChip(
                        selected = !iPay,
                        onClick = { iPay = false },
                        label = { Text("They pay me") }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val minor = parseAmountToMinor(amountText)
                    if (minor != null && minor > 0) {
                        onConfirm(minor, currency, iPay)
                    }
                },
                enabled = amountText.isNotBlank()
            ) { Text("Settle") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

/** Parse "12.50" → 1250L (centified) */
private fun parseAmountToMinor(text: String): Long? {
    val t = text.trim().replace(',', '.')
    if (t.isBlank()) return null
    val parts = t.split('.')
    return try {
        when (parts.size) {
            1 -> parts[0].toLong() * 100
            2 -> {
                val major = parts[0].toLong()
                val minorStr = parts[1].padEnd(2, '0').take(2)
                major * 100 + minorStr.toLong()
            }
            else -> null
        }
    } catch (_: Throwable) { null }
}

@Composable
private fun TotalInDefaultLine(total: TotalInDefault) {
    val amount = total.amountMinor
    val approxPrefix = if (total.isApprox) "≈ " else ""

    val (label, color) = when {
        amount > 0 -> "Owes you $approxPrefix${formatMinor(amount, total.currency)}" to MaterialTheme.colorScheme.primary
        amount < 0 -> "You owe $approxPrefix${formatMinor(-amount, total.currency)}" to MaterialTheme.colorScheme.error
        else -> "Settled up ${approxPrefix}in ${total.currency}" to MaterialTheme.colorScheme.onSurface
    }

    Text(
        text = "Total: $label",
        style = MaterialTheme.typography.titleSmall,
        color = color
    )

    // "Last updated" line if cached rates were used
    total.lastUpdatedAtMillis?.let { millis ->
        val sdf = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
        val formatted = sdf.format(Date(millis))
        Text(
            text = "Rates last updated $formatted",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    // Warning if some currencies couldn't be converted
    if (total.missingCurrencies.isNotEmpty()) {
        Text(
            text = "Some currencies couldn't be converted: ${total.missingCurrencies.joinToString(", ")}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.error
        )
    }
}







