package com.example.pantrychef.ui.pantry

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
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
                    IconButton(onClick = onOpenScanPantry) {
                        Icon(
                            imageVector = Icons.Filled.CameraAlt,
                            contentDescription = "Scan pantry"
                        )
                    }
                    TextButton(onClick = onOpenSaved) {
                        Text("Saved")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onOpenAddItem) {
                Text("+")
            }
        },
        floatingActionButtonPosition = FabPosition.End,
        bottomBar = {
            BottomAppBar(
                modifier = Modifier.navigationBarsPadding()
            ) {
                Button(
                    onClick = onOpenRecipeCustomization,
                    enabled = canGenerate,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .height(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.RestaurantMenu,
                        contentDescription = null
                    )
                    Spacer(Modifier.size(8.dp))
                    Text("Generate Recipe")
                }
            }
        }
    ) { innerPadding ->
        PantryContent(
            items = pantryItems,
            onDelete = viewModel::deleteItem,
            onEdit = { editing = it },
            onOpenAddItem = onOpenAddItem,
            onOpenScanPantry = onOpenScanPantry,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        )
    }

    val itemToEdit = editing
    if (itemToEdit != null) {
        EditItemSheet(
            title = "Edit Pantry Item",
            initialName = itemToEdit.name,
            initialQuantity = itemToEdit.quantity,
            initialUnit = itemToEdit.unit,
            onDismiss = { editing = null },
            onSave = { newName, newQuantity, newUnit ->
                viewModel.updateItem(
                    id = itemToEdit.id,
                    name = newName,
                    quantity = newQuantity,
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
    onOpenAddItem: () -> Unit,
    onOpenScanPantry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bottomBarHeight = 84.dp

    if (items.isEmpty()) {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            ElevatedCard(
                shape = MaterialTheme.shapes.extraLarge,
                modifier = Modifier.padding(20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Your pantry is feeling lonely.",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = "Add a few ingredients or scan your shelves to get started.",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(onClick = onOpenScanPantry) {
                            Text("Scan pantry")
                        }
                        Button(onClick = onOpenAddItem) {
                            Text("Add item")
                        }
                    }
                }
            }
        }
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 12.dp,
            bottom = 12.dp + bottomBarHeight
        ),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(items, key = { it.id }) { item ->
            PantryRow(
                item = item,
                onDelete = onDelete,
                onEdit = onEdit
            )
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
                Text(
                    text = "${formatQuantity(item.quantity)} ${item.unit}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { onEdit(item) }) {
                    Text("Edit")
                }
                FilledTonalButton(onClick = { onDelete(item.id) }) {
                    Text("Delete")
                }
            }
        }
    }
}

private fun formatQuantity(quantity: Double): String {
    return if (quantity % 1.0 == 0.0) {
        quantity.toInt().toString()
    } else {
        quantity.toString()
    }
}