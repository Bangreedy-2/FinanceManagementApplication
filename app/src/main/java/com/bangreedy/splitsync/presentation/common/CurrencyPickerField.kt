package com.bangreedy.splitsync.presentation.common

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import com.bangreedy.splitsync.core.currency.CurrencyMeta

/**
 * A text field with a filtered dropdown of currencies from [CurrencyMeta.supportedCurrencies].
 *
 * - User can type freely; dropdown shows matching currencies when ≥ 1 character is entered.
 * - Matching is by code prefix OR name contains (case-insensitive).
 * - Selecting from the dropdown fills the field with that code.
 * - [onCurrencyChanged] is called on every text change with the uppercased input.
 * - [isValid] can be checked externally via [CurrencyMeta.supportedCurrencies.containsKey].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrencyPickerField(
    value: String,
    onCurrencyChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Currency"
) {
    val allCurrencies = CurrencyMeta.supportedCurrencies
    var expanded by remember { mutableStateOf(false) }

    val filtered = remember(value) {
        if (value.isBlank()) emptyList()
        else allCurrencies.entries.filter { (code, name) ->
            code.startsWith(value, ignoreCase = true) ||
                    name.contains(value, ignoreCase = true)
        }
    }

    val isValid = allCurrencies.containsKey(value.uppercase())

    // Show dropdown when there's input text and matching results
    val showDropdown = value.isNotBlank() && filtered.isNotEmpty() && !isValid

    Box(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = { newVal ->
                onCurrencyChanged(newVal.uppercase())
                expanded = true
            },
            label = { Text(label) },
            singleLine = true,
            isError = value.isNotBlank() && !isValid,
            supportingText = if (value.isNotBlank() && !isValid) {
                { Text("Select a valid currency") }
            } else if (isValid) {
                { Text(CurrencyMeta.nameOf(value)) }
            } else null,
            modifier = Modifier.fillMaxWidth()
        )

        DropdownMenu(
            expanded = showDropdown && expanded,
            onDismissRequest = { expanded = false },
            properties = PopupProperties(focusable = false),
            modifier = Modifier
                .heightIn(max = 200.dp)
                .fillMaxWidth(0.9f)
        ) {
            filtered.take(8).forEach { (code, name) ->
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(code, style = MaterialTheme.typography.bodyLarge)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                name,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    onClick = {
                        onCurrencyChanged(code)
                        expanded = false
                    }
                )
            }
        }
    }
}

/** Check if a currency code is in the supported registry. */
fun isValidCurrency(code: String): Boolean =
    CurrencyMeta.supportedCurrencies.containsKey(code.uppercase())


