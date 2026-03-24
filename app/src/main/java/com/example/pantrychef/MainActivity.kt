package com.example.pantrychef

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.pantrychef.ui.add.AddItemSheet
import com.example.pantrychef.ui.pantry.PantryScreen
import com.example.pantrychef.ui.pantry.PantryViewModel
import com.example.pantrychef.ui.recipe.RecipeGenerationViewModel
import com.example.pantrychef.ui.recipe.RecipeResultScreen
import com.example.pantrychef.ui.recipe.RecipeScreen
import com.example.pantrychef.ui.recipe.SavedRecipesScreen
import com.example.pantrychef.ui.scan.ScanPantryScreen
import com.example.pantrychef.ui.theme.PantryChefTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            PantryChefTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    PantryChefRoot()
                }
            }
        }
    }
}

private object Destinations {
    const val Pantry = "pantry"
    const val RecipePreferences = "recipe_preferences"
    const val RecipeResult = "recipe_result"
    const val SavedRecipes = "saved_recipes"
    const val Scan = "scan"
}

@Composable
private fun PantryChefRoot(
    pantryViewModel: PantryViewModel = hiltViewModel(),
    recipeViewModel: RecipeGenerationViewModel = hiltViewModel()
) {
    val navController = rememberNavController()
    var showAddSheet by remember { mutableStateOf(false) }

    NavHost(
        navController = navController,
        startDestination = Destinations.Pantry
    ) {
        composable(Destinations.Pantry) {
            PantryScreen(
                viewModel = pantryViewModel,
                onOpenAddItem = { showAddSheet = true },
                onOpenRecipeCustomization = { navController.navigate(Destinations.RecipePreferences) },
                onOpenSaved = { navController.navigate(Destinations.SavedRecipes) },
                onOpenScanPantry = { navController.navigate(Destinations.Scan) }
            )
        }

        composable(Destinations.RecipePreferences) {
            RecipeScreen(
                viewModel = recipeViewModel,
                onBack = { navController.popBackStack() },
                onOpenResult = {
                    navController.navigateSingleTopTo(Destinations.RecipeResult)
                }
            )
        }

        composable(Destinations.RecipeResult) {
            RecipeResultScreen(
                viewModel = recipeViewModel,
                onBack = { navController.popBackStack() },
                onOpenPreferences = {
                    navController.navigateSingleTopTo(Destinations.RecipePreferences)
                }
            )
        }

        composable(Destinations.SavedRecipes) {
            SavedRecipesScreen(
                viewModel = recipeViewModel,
                onBack = { navController.popBackStack() },
                onOpenRecipe = {
                    navController.navigateSingleTopTo(Destinations.RecipeResult)
                }
            )
        }

        composable(Destinations.Scan) {
            ScanPantryScreen(
                onBack = { navController.popBackStack() },
                onAddItems = { items ->
                    items.forEach { detected ->
                        pantryViewModel.insertItem(
                            name = detected.name,
                            quantity = detected.count.toDouble(),
                            unit = detected.unit
                        )
                    }
                    navController.popBackStack(Destinations.Pantry, inclusive = false)
                }
            )
        }
    }

    if (showAddSheet) {
        AddItemSheet(
            onDismiss = { showAddSheet = false },
            onAdd = { name, quantity, unit ->
                pantryViewModel.insertItem(
                    name = name,
                    quantity = quantity,
                    unit = unit
                )
                showAddSheet = false
            },
            onOpenScan = {
                showAddSheet = false
                navController.navigateSingleTopTo(Destinations.Scan)
            }
        )
    }
}

private fun NavHostController.navigateSingleTopTo(route: String) {
    navigate(route) {
        launchSingleTop = true
    }
}