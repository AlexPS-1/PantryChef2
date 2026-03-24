package com.example.pantrychef.ui.recipe

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeResultScreen(
    viewModel: RecipeGenerationViewModel,
    onBack: () -> Unit,
    onOpenPreferences: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val pantryIngredients by viewModel.pantryIngredients.collectAsState()
    val extraIngredients by viewModel.extraIngredients.collectAsState()
    val isSaved by viewModel.isCurrentRecipeSaved.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Recipe result") },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text("Back")
                    }
                },
                actions = {
                    TextButton(onClick = onOpenPreferences) {
                        Text("Edit")
                    }
                }
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },
        bottomBar = {
            BottomAppBar(
                modifier = Modifier.navigationBarsPadding()
            ) {
                FilledTonalButton(
                    onClick = { viewModel.saveCurrentRecipe() },
                    enabled = uiState.recipe != null && !isSaved,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (isSaved) "Saved" else "Save")
                }

                Spacer(Modifier.padding(6.dp))

                Button(
                    onClick = onOpenPreferences,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Regenerate")
                }
            }
        }
    ) { innerPadding ->
        when {
            uiState.isGenerating -> {
                LoadingState(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                )
            }

            uiState.recipe == null -> {
                EmptyRecipeState(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                )
            }

            else -> {
                val recipe = uiState.recipe!!
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        ElevatedCard(shape = MaterialTheme.shapes.extraLarge) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = recipe.title,
                                    style = MaterialTheme.typography.headlineSmall
                                )
                                if (recipe.oneLiner.isNotBlank()) {
                                    Text(
                                        text = recipe.oneLiner,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = "Serves ${recipe.servings}",
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }
                    }

                    item {
                        IngredientSection(
                            title = "Already in your pantry",
                            ingredients = pantryIngredients,
                            emptyText = "No direct pantry matches were found."
                        )
                    }

                    item {
                        IngredientSection(
                            title = "You may need these extras",
                            ingredients = extraIngredients,
                            emptyText = "No extra ingredients needed."
                        )
                    }

                    item {
                        ElevatedCard(shape = MaterialTheme.shapes.extraLarge) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "Steps",
                                    style = MaterialTheme.typography.titleLarge
                                )

                                recipe.steps.forEachIndexed { index, step ->
                                    ElevatedCard {
                                        Column(
                                            modifier = Modifier.padding(12.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            AssistChip(
                                                onClick = {},
                                                label = { Text("Step ${index + 1}") }
                                            )
                                            Text(step)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadingState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator()
        Spacer(Modifier.padding(8.dp))
        Text(
            text = "PantryChef is building a recipe...",
            style = MaterialTheme.typography.titleMedium
        )
    }
}

@Composable
private fun EmptyRecipeState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "No recipe yet.",
            style = MaterialTheme.typography.headlineSmall
        )
        Text(
            text = "Pick your preferences and generate one from your pantry.",
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
private fun IngredientSection(
    title: String,
    ingredients: List<StructuredIngredient>,
    emptyText: String
) {
    ElevatedCard(shape = MaterialTheme.shapes.extraLarge) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge
            )

            if (ingredients.isEmpty()) {
                Text(emptyText)
            } else {
                ingredients.forEach { ingredient ->
                    Text(
                        text = "• ${formatIngredient(ingredient)}",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }
}

private fun formatIngredient(ingredient: StructuredIngredient): String {
    val amountText = if (ingredient.amount % 1.0 == 0.0) {
        ingredient.amount.toInt().toString()
    } else {
        ingredient.amount.toString()
    }

    val note = ingredient.note.takeIf { it.isNotBlank() }?.let { " ($it)" }.orEmpty()
    return "$amountText ${ingredient.unit} ${ingredient.name}$note"
}