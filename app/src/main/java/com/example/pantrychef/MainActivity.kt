// Main activity hosting PantryChef composables and simple in-app navigation.
package com.example.pantrychef

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.pantrychef.ui.add.AddItemSheet
import com.example.pantrychef.ui.pantry.PantryScreen
import com.example.pantrychef.ui.pantry.PantryViewModel
import com.example.pantrychef.ui.recipe.RecipeResultScreen
import com.example.pantrychef.ui.recipe.RecipeScreen
import com.example.pantrychef.ui.recipe.SavedRecipesScreen
import com.example.pantrychef.ui.scan.ScanPantryScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Surface(color = MaterialTheme.colorScheme.background) {
                PantryChefRoot()
            }
        }
    }
}

@Composable
private fun PantryChefRoot(
    pantryViewModel: PantryViewModel = hiltViewModel()
) {
    var route by remember { mutableStateOf("pantry") }
    var showAdd by remember { mutableStateOf(false) }

    when (route) {
        "pantry" -> {
            PantryScreen(
                viewModel = pantryViewModel,
                onOpenAddItem = { showAdd = true },
                onOpenRecipeCustomization = { route = "recipe_prefs" },
                onOpenSaved = { route = "saved" }
            )
        }
        "recipe_prefs" -> {
            RecipeScreen(
                viewModel = pantryViewModel,
                onBack = { route = "pantry" },
                onOpenResult = { route = "recipe_result" }
            )
        }
        "recipe_result" -> {
            RecipeResultScreen(
                viewModel = pantryViewModel,
                onBack = { route = "pantry" }
            )
        }
        "saved" -> {
            SavedRecipesScreen(
                viewModel = pantryViewModel,
                onBack = { route = "pantry" },
                onOpenRecipe = { route = "recipe_result" }
            )
        }
        "scan" -> {
            ScanPantryScreen(
                onBack = { route = "pantry" },
                onAddItems = { items ->
                    items.forEach { it ->
                        pantryViewModel.insertItem(
                            name = it.name,
                            quantity = it.count.toDouble(),
                            unit = it.unit
                        )
                    }
                    route = "pantry"
                }
            )
        }
    }

    if (showAdd) {
        AddItemSheet(
            onDismiss = { showAdd = false },
            onAdd = { name, qty, unit ->
                pantryViewModel.insertItem(name, qty, unit)
            },
            onOpenScan = {
                showAdd = false
                route = "scan"
            }
        )
    }
}
