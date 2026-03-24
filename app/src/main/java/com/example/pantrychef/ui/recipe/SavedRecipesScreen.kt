package com.example.pantrychef.ui.recipe

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.pantrychef.core.RecipesRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedRecipesScreen(
    viewModel: RecipeGenerationViewModel,
    onBack: () -> Unit,
    onOpenRecipe: () -> Unit
) {
    val savedRecipes by viewModel.savedRecipes.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Saved recipes") },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text("Back")
                    }
                }
            )
        },
        bottomBar = {
            BottomAppBar(
                modifier = Modifier.navigationBarsPadding()
            ) {}
        }
    ) { innerPadding ->
        if (savedRecipes.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "No saved recipes yet.",
                    style = MaterialTheme.typography.headlineSmall
                )
                Text(
                    text = "Generate a recipe and tap Save to keep it here.",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(
                    items = savedRecipes,
                    key = { savedRecipe -> savedRecipe.id }
                ) { savedRecipe ->
                    SavedRecipeRow(
                        recipe = savedRecipe,
                        onOpen = {
                            viewModel.loadSavedRecipe(savedRecipe.id)
                            onOpenRecipe()
                        },
                        onDelete = {
                            viewModel.deleteSavedRecipe(savedRecipe.id)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SavedRecipeRow(
    recipe: RecipesRepository.SavedRecipe,
    onOpen: () -> Unit,
    onDelete: () -> Unit
) {
    ElevatedCard(shape = MaterialTheme.shapes.extraLarge) {
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ListItem(
                headlineContent = {
                    Text(recipe.title)
                },
                supportingContent = {
                    Text("Saved ${recipe.dateString}")
                }
            )

            androidx.compose.foundation.layout.Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FilledTonalButton(
                    onClick = onOpen,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Open")
                }

                TextButton(
                    onClick = onDelete,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Delete")
                }
            }
        }
    }
}