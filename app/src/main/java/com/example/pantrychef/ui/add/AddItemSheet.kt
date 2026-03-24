// Bottom-sheet UI to add pantry items with OFF suggestions, unit inference, and optional scan entry.
package com.example.pantrychef.ui.add

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.pantrychef.core.OffSuggestion
import com.example.pantrychef.core.UnitInference

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddItemSheet(
    onDismiss: () -> Unit,
    onAdd: (name: String, quantity: Double, unit: String) -> Unit,
    vm: AddItemViewModel = hiltViewModel(),
    onOpenScan: () -> Unit = {}
) {
    var name by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("1") }

    val unitOptions = listOf("pcs", "g", "kg", "ml", "l", "tbsp", "tsp", "cup", "can", "jar")
    var unit by remember { mutableStateOf(unitOptions.first()) }

    val suggestions by vm.suggestions.collectAsState()
    val loading by vm.loading.collectAsState()
    val query by vm.query.collectAsState()

    var showSuggestions by remember { mutableStateOf(true) }

    LaunchedEffect(name, showSuggestions) {
        if (showSuggestions) vm.setQuery(name) else vm.clearSuggestions()
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    LaunchedEffect(Unit) { sheetState.expand() }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        contentWindowInsets = { WindowInsets.ime }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Add Pantry Item",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "Close")
                }
            }

            OutlinedButton(
                onClick = {
                    onDismiss()
                    onOpenScan()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(imageVector = Icons.Filled.CameraAlt, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Scan pantry")
            }

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name (type to get suggestions)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AssistChip(
                    onClick = {
                        showSuggestions = !showSuggestions
                        if (!showSuggestions) vm.clearSuggestions() else vm.setQuery(name)
                    },
                    label = { Text(if (showSuggestions) "Hide suggestions" else "Show suggestions") }
                )
                if (loading && showSuggestions) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                }
            }

            if (showSuggestions && suggestions.isNotEmpty() && query.isNotBlank()) {
                SuggestionsList(
                    suggestions = suggestions,
                    onPick = { s ->
                        name = s.name
                        val hinted = extractUnitToken(s.label)
                        val inferred = hinted ?: UnitInference.inferUnit(s.name)
                        unit = inferred
                        quantity = s.quantity.toString()
                        showSuggestions = false
                        vm.clearSuggestions()
                    }
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = quantity,
                    onValueChange = {
                        quantity = it.filter { ch -> ch.isDigit() || ch == '.' || ch == ',' }
                    },
                    label = { Text("Quantity") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next
                    ),
                    modifier = Modifier.weight(1f)
                )

                var unitExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = unitExpanded,
                    onExpandedChange = { unitExpanded = it },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = unit,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Unit") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = unitExpanded)
                        },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    DropdownMenu(
                        expanded = unitExpanded,
                        onDismissRequest = { unitExpanded = false }
                    ) {
                        unitOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    unit = option
                                    unitExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) { Text("Cancel") }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = {
                        val qty = quantity.replace(',', '.').toDoubleOrNull() ?: 1.0
                        onAdd(name.ifBlank { "Unnamed" }, qty, unit.ifBlank { "pcs" })
                        name = ""
                        quantity = "1"
                        unit = unitOptions.first()
                        showSuggestions = true
                        vm.clearSuggestions()
                        vm.setQuery("")
                    },
                    enabled = name.isNotBlank()
                ) { Text("Add") }
            }

            Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.ime))
            Spacer(Modifier.height(8.dp))
        }
    }
}

private fun extractUnitToken(label: String): String? {
    val match = Regex("""\b(kg|g|ml|l)\b""", RegexOption.IGNORE_CASE).find(label)
    return match?.groupValues?.get(1)?.lowercase()
}

@Composable
private fun SuggestionsList(
    suggestions: List<com.example.pantrychef.core.OffSuggestion>,
    onPick: (com.example.pantrychef.core.OffSuggestion) -> Unit
) {
    Surface(
        tonalElevation = 1.dp,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth()
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 220.dp),
            contentPadding = PaddingValues(vertical = 4.dp)
        ) {
            items(suggestions) { s ->
                SuggestionRow(s, onPick)
            }
        }
    }
}

@Composable
private fun SuggestionRow(
    s: com.example.pantrychef.core.OffSuggestion,
    onPick: (com.example.pantrychef.core.OffSuggestion) -> Unit
) {
    ListItem(
        headlineContent = { Text(s.label) },
        supportingContent = {
            val infoParts = buildList {
                add("${s.quantity} ${s.unit}")
                if (s.code.isNotBlank()) add(s.code)
            }
            if (infoParts.isNotEmpty()) Text(infoParts.joinToString(" • "))
        },
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onPick(s) }
            .padding(horizontal = 4.dp)
            .heightIn(min = 56.dp),
        trailingContent = null
    )
    HorizontalDivider()
}
