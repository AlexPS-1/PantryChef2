package com.example.pantrychef.ui.recipe

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.pantrychef.core.RecipePreferences

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RecipeScreen(
    viewModel: RecipeGenerationViewModel,
    onBack: () -> Unit,
    onOpenResult: () -> Unit
) {
    val pantryItems by viewModel.pantryItems.collectAsState()
    val scrollState = rememberScrollState()

    val mealTime = remember { mutableStateListOf<String>() }
    val styleSpeed = remember { mutableStateListOf<String>() }
    val diet = remember { mutableStateListOf<String>() }
    val cuisine = remember { mutableStateListOf<String>() }
    val mood = remember { mutableStateListOf<String>() }

    val voices = listOf(
        "Playful Classic",
        "Plain (Minimal)",
        "British Pub Cook",
        "California Wellness",
        "Italian Nonna",
        "Science-y Food Nerd",
        "Street-food Storyteller",
        "Nordic Minimalist",
        "BBQ Pitmaster",
        "Kitchen Nightmare (Brutally Honest)",
        "Pirate",
        "Cute for Kids"
    )

    var selectedVoice by remember { mutableStateOf("Playful Classic") }
    var spicyLanguage by remember { mutableStateOf(false) }
    var childFriendly by remember { mutableStateOf(false) }
    var mustIncludeText by remember { mutableStateOf("") }
    var allowAdditions by remember { mutableStateOf(true) }
    var additionsCount by remember { mutableFloatStateOf(2f) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Recipe preferences") },
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
            ) {
                Button(
                    onClick = {
                        val preferences = RecipePreferences(
                            mealTime = mealTime.toList(),
                            styleSpeed = styleSpeed.toList(),
                            diet = diet.toList(),
                            cuisine = cuisine.toList(),
                            mood = mood.toList(),
                            mustInclude = mustIncludeText
                                .split(",")
                                .map { it.trim() }
                                .filter { it.isNotBlank() },
                            maxNewAdditions = if (allowAdditions) additionsCount.toInt() else 0,
                            voice = selectedVoice,
                            spicyLanguage = spicyLanguage,
                            childFriendly = childFriendly
                        )
                        viewModel.generateRecipe(preferences)
                        onOpenResult()
                    },
                    enabled = pantryItems.isNotEmpty(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .height(52.dp)
                ) {
                    Text("Generate recipe")
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ElevatedCard(shape = MaterialTheme.shapes.extraLarge) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Pantry snapshot",
                        style = MaterialTheme.typography.titleMedium
                    )
                    if (pantryItems.isEmpty()) {
                        Text("No pantry items yet.")
                    } else {
                        Text(
                            text = pantryItems.joinToString { "${it.name} (${it.quantity} ${it.unit})" },
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            PreferenceGroup(
                title = "Meal time",
                options = listOf(
                    "Breakfast",
                    "Brunch",
                    "Lunch",
                    "Dinner",
                    "Snack",
                    "Dessert",
                    "Late-night"
                ),
                selected = mealTime
            )

            PreferenceGroup(
                title = "Style & speed",
                options = listOf(
                    "Quick",
                    "One-pan",
                    "Comforting",
                    "Fresh",
                    "Meal prep",
                    "Cozy",
                    "Budget",
                    "High-protein",
                    "Low-effort",
                    "Fancy-ish",
                    "Crispy",
                    "Saucy"
                ),
                selected = styleSpeed
            )

            PreferenceGroup(
                title = "Diet",
                options = listOf(
                    "Vegetarian",
                    "Vegan",
                    "High-protein",
                    "Low-carb",
                    "Dairy-free",
                    "Gluten-free",
                    "Halal-style",
                    "Kid-friendly"
                ),
                selected = diet
            )

            PreferenceGroup(
                title = "Cuisine",
                options = listOf(
                    "Italian",
                    "Mediterranean",
                    "Mexican",
                    "Asian-inspired",
                    "British",
                    "American BBQ",
                    "Nordic",
                    "Middle Eastern",
                    "Indian-inspired",
                    "French bistro"
                ),
                selected = cuisine
            )

            PreferenceGroup(
                title = "Mood",
                options = listOf(
                    "Cozy",
                    "Light",
                    "Bold",
                    "Nostalgic",
                    "Fancy-ish",
                    "Chaotic good",
                    "Wholesome",
                    "Cheeky",
                    "Comfort food",
                    "Fresh and bright"
                ),
                selected = mood
            )

            ElevatedCard(shape = MaterialTheme.shapes.extraLarge) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Voice",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Choose the personality PantryChef uses when writing your recipe.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        voices.forEach { voice ->
                            FilterChip(
                                selected = selectedVoice == voice,
                                onClick = { selectedVoice = voice },
                                label = { Text(voice) }
                            )
                        }
                    }
                }
            }

            ElevatedCard(shape = MaterialTheme.shapes.extraLarge) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Constraints",
                        style = MaterialTheme.typography.titleMedium
                    )

                    OutlinedTextField(
                        value = mustIncludeText,
                        onValueChange = { mustIncludeText = it },
                        label = { Text("Must include ingredients (comma-separated)") },
                        placeholder = { Text("e.g. eggs, spinach, cheddar") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    HorizontalDivider()

                    ToggleRow(
                        label = "Allow a few extra ingredients",
                        checked = allowAdditions,
                        onCheckedChange = { allowAdditions = it }
                    )

                    if (allowAdditions) {
                        Text("Max new additions: ${additionsCount.toInt()}")
                        Slider(
                            value = additionsCount,
                            onValueChange = { additionsCount = it },
                            valueRange = 1f..6f,
                            steps = 4
                        )
                    }

                    HorizontalDivider()

                    ToggleRow(
                        label = "Keep language playful",
                        checked = spicyLanguage,
                        onCheckedChange = { spicyLanguage = it }
                    )

                    ToggleRow(
                        label = "Child-friendly mode",
                        checked = childFriendly,
                        onCheckedChange = { childFriendly = it }
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PreferenceGroup(
    title: String,
    options: List<String>,
    selected: MutableList<String>
) {
    ElevatedCard(shape = MaterialTheme.shapes.extraLarge) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                options.forEach { option ->
                    val isSelected = option in selected
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            if (isSelected) {
                                selected.remove(option)
                            } else {
                                selected.add(option)
                            }
                        },
                        label = { Text(option) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}