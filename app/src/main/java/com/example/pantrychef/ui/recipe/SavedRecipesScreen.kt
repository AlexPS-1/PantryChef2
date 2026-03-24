package com.example.pantrychef.ui.recipe

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.pantrychef.ui.pantry.PantryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedRecipesScreen(
    viewModel: PantryViewModel = hiltViewModel(),
    onOpenRecipe: (String) -> Unit = {},
    onBack: () -> Unit = {}
) {
    val recipes by viewModel.savedRecipes.collectAsState()
    LaunchedEffect(Unit) { viewModel.refreshSavedRecipes() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Saved recipes") },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("Back") }
                }
            )
        }
    ) { inner ->
        if (recipes.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(inner),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                Text("No saved recipes yet.")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(inner)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(recipes, key = { it.id }) { r ->
                    ElevatedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenRecipe(r.id) }
                    ) {
                        Column(
                            Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(r.title, style = MaterialTheme.typography.titleMedium)
                            Text(r.dateString, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextButton(onClick = { onOpenRecipe(r.id) }) { Text("Open") }
                                TextButton(onClick = { viewModel.deleteSavedRecipe(r.id) }) { Text("Delete") }
                            }
                        }
                    }
                }
            }
        }
    }
}
