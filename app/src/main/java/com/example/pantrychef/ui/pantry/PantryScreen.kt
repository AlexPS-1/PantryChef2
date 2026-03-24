// Pantry screen showing items, with edit and delete actions and recipe generation entry.
package com.example.pantrychef.ui.pantry

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.pantrychef.data.local.PantryItem
import com.example.pantrychef.ui.add.EditItemSheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantryScreen(
    viewModel: PantryViewModel = hiltViewModel(),
    onOpenAddItem: () -> Unit,
    onOpenRecipeCustomization: () -> Unit,
    onOpenSaved: () -> Unit,
    onOpenScanPantry: () -> Unit = {}
) {
    val pantryItems by viewModel.items.collectAsState()
    val canGenerate = pantryItems.isNotEmpty()

    var editing by remember { mutableStateOf<PantryItem?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("PantryChef") },
                actions = {
                    TextButton(onClick = onOpenSaved) { Text("Saved") }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onOpenAddItem) { Text("+") }
        },
        floatingActionButtonPosition = FabPosition.End,
        bottomBar = {
            BottomAppBar {
                Spacer(Modifier.weight(1f))
                Button(
                    onClick = onOpenRecipeCustomization,
                    enabled = canGenerate,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .height(48.dp)
                ) { Text("Generate Recipe") }
            }
        }
    ) { innerPadding ->
        PantryContent(
            items = pantryItems,
            onDelete = { id -> viewModel.deleteItem(id) },
            onEdit = { item -> editing = item },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        )
    }

    val toEdit = editing
    if (toEdit != null) {
        EditItemSheet(
            title = "Edit Pantry Item",
            initialName = toEdit.name,
            initialQuantity = toEdit.quantity,
            initialUnit = toEdit.unit,
            onDismiss = { editing = null },
            onSave = { newName, newQty, newUnit ->
                viewModel.updateItem(
                    id = toEdit.id,
                    name = newName,
                    quantity = newQty,
                    unit = newUnit
                )
                editing = null
            }
        )
    }
}

@Composable
private fun PantryContent(
    items: List<PantryItem>,
    onDelete: (Long) -> Unit,
    onEdit: (PantryItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val bottomBarHeight = 80.dp

    if (items.isEmpty()) {
        Box(modifier, contentAlignment = Alignment.Center) {
            Text(
                text = "Your pantry is feeling lonely.\nTap + to add your first item!",
                style = MaterialTheme.typography.bodyLarge
            )
        }
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp, end = 16.dp, top = 12.dp, bottom = 12.dp + bottomBarHeight
        ),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(items, key = { it.id }) { item ->
            PantryRow(item = item, onDelete = onDelete, onEdit = onEdit)
        }
    }
}

@Composable
private fun PantryRow(
    item: PantryItem,
    onDelete: (Long) -> Unit,
    onEdit: (PantryItem) -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val qty = if (item.quantity % 1.0 == 0.0) item.quantity.toInt().toString() else item.quantity.toString()
                Text(
                    text = "$qty ${item.unit}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { onEdit(item) }) { Text("Edit") }
                FilledTonalButton(onClick = { onDelete(item.id) }) { Text("Delete") }
            }
        }
    }
}
