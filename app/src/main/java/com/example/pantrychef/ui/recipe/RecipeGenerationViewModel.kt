package com.example.pantrychef.ui.recipe

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pantrychef.core.GeminiHttp
import com.example.pantrychef.core.RecipePreferences
import com.example.pantrychef.core.RecipesRepository
import com.example.pantrychef.data.local.PantryDao
import com.example.pantrychef.data.local.PantryItem
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

data class RecipeUiState(
    val isGenerating: Boolean = false,
    val recipe: StructuredRecipe? = null,
    val rawText: String = "",
    val errorMessage: String? = null,
    val currentSavedId: String? = null
)

@HiltViewModel
class RecipeGenerationViewModel @Inject constructor(
    private val pantryDao: PantryDao,
    private val geminiHttp: GeminiHttp,
    private val recipesRepository: RecipesRepository
) : ViewModel() {

    val pantryItems: StateFlow<List<PantryItem>> = pantryDao.observeAll()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _uiState = MutableStateFlow(RecipeUiState())
    val uiState: StateFlow<RecipeUiState> = _uiState.asStateFlow()

    private val _savedRecipes = MutableStateFlow<List<RecipesRepository.SavedRecipe>>(emptyList())
    val savedRecipes: StateFlow<List<RecipesRepository.SavedRecipe>> = _savedRecipes.asStateFlow()

    val pantryIngredients: StateFlow<List<StructuredIngredient>> = combine(
        uiState,
        pantryItems
    ) { state, pantry ->
        val recipe = state.recipe ?: return@combine emptyList()
        RecipeParser.partitionIngredients(recipe.ingredients, pantry).pantry
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val extraIngredients: StateFlow<List<StructuredIngredient>> = combine(
        uiState,
        pantryItems
    ) { state, pantry ->
        val recipe = state.recipe ?: return@combine emptyList()
        RecipeParser.partitionIngredients(recipe.ingredients, pantry).additions
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val isCurrentRecipeSaved: StateFlow<Boolean> = uiState
        .combine(savedRecipes) { state, saved ->
            val savedId = state.currentSavedId
            savedId != null && saved.any { it.id == savedId }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    init {
        refreshSavedRecipes()
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun clearCurrentRecipe() {
        _uiState.value = RecipeUiState()
    }

    fun generateRecipe(preferences: RecipePreferences) {
        val pantrySnapshot = pantryItems.value
        if (pantrySnapshot.isEmpty()) {
            _uiState.value = RecipeUiState(
                errorMessage = "Add at least one pantry item before generating a recipe."
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = RecipeUiState(isGenerating = true)

            val prompt = buildRecipePrompt(
                pantryItems = pantrySnapshot,
                preferences = preferences
            )

            val response = withTimeoutOrNull(35_000) {
                geminiHttp.generateRecipe(prompt)
            }

            if (response == null) {
                _uiState.value = RecipeUiState(
                    errorMessage = "PantryChef timed out. Try again."
                )
                return@launch
            }

            val parsed = RecipeParser.fromModelText(response)
            if (parsed == null) {
                _uiState.value = RecipeUiState(
                    errorMessage = "PantryChef returned a format I couldn't read. Try again."
                )
                return@launch
            }

            _uiState.value = RecipeUiState(
                isGenerating = false,
                recipe = parsed,
                rawText = RecipeParser.toDisplayText(parsed),
                errorMessage = null,
                currentSavedId = null
            )
        }
    }

    fun saveCurrentRecipe() {
        val state = _uiState.value
        val recipe = state.recipe ?: return
        if (state.currentSavedId != null) return

        viewModelScope.launch {
            val rawText = state.rawText.ifBlank { RecipeParser.toDisplayText(recipe) }
            val id = recipesRepository.saveRecipe(
                title = recipe.title,
                text = rawText
            )

            _uiState.value = _uiState.value.copy(currentSavedId = id)
            refreshSavedRecipes()
        }
    }

    fun loadSavedRecipe(id: String) {
        viewModelScope.launch {
            val payload = recipesRepository.loadRecipe(id)
            val recipe = payload?.let { RecipeParser.fromSavedText(it.text) }

            if (payload == null || recipe == null) {
                _uiState.value = RecipeUiState(
                    errorMessage = "That saved recipe could not be loaded."
                )
                return@launch
            }

            _uiState.value = RecipeUiState(
                isGenerating = false,
                recipe = recipe,
                rawText = payload.text,
                errorMessage = null,
                currentSavedId = payload.id
            )
        }
    }

    fun deleteSavedRecipe(id: String) {
        viewModelScope.launch {
            recipesRepository.deleteRecipe(id)
            if (_uiState.value.currentSavedId == id) {
                _uiState.value = RecipeUiState()
            }
            refreshSavedRecipes()
        }
    }

    private fun refreshSavedRecipes() {
        viewModelScope.launch {
            _savedRecipes.value = recipesRepository.listRecipes()
        }
    }

    private fun buildRecipePrompt(
        pantryItems: List<PantryItem>,
        preferences: RecipePreferences
    ): String {
        val pantryLines = pantryItems.joinToString(separator = "\n") { item ->
            "- ${item.name}: ${item.quantity} ${item.unit}"
        }

        val guidance = buildList {
            if (preferences.mealTime.isNotEmpty()) {
                add("Meal time: ${preferences.mealTime.joinToString()}")
            }
            if (preferences.styleSpeed.isNotEmpty()) {
                add("Style and speed: ${preferences.styleSpeed.joinToString()}")
            }
            if (preferences.diet.isNotEmpty()) {
                add("Diet: ${preferences.diet.joinToString()}")
            }
            if (preferences.cuisine.isNotEmpty()) {
                add("Cuisine: ${preferences.cuisine.joinToString()}")
            }
            if (preferences.mood.isNotEmpty()) {
                add("Mood: ${preferences.mood.joinToString()}")
            }
            if (preferences.mustInclude.isNotEmpty()) {
                add("Must include all of: ${preferences.mustInclude.joinToString()}")
            }

            add(
                if (preferences.maxNewAdditions <= 0) {
                    "Do not add any non-pantry ingredients other than salt, pepper, oil, and water."
                } else {
                    "You may add up to ${preferences.maxNewAdditions} non-pantry ingredients, excluding salt, pepper, oil, and water."
                }
            )

            add("Voice: ${preferences.voice ?: "Playful Classic"}")
            add("Child-friendly: ${preferences.childFriendly}")
            add("Spicy language: ${preferences.spicyLanguage}")
        }.joinToString(separator = "\n")

        return """
            You are PantryChef, an Android cooking assistant.

            Create exactly one recipe from the pantry items below.
            Be practical, appealing, and realistic for a home cook.

            Pantry items:
            $pantryLines

            Preferences:
            $guidance

            Return JSON only.
            Do not use markdown fences.
            Do not include commentary before or after the JSON.

            JSON schema:
            {
              "title": "string",
              "oneLiner": "string",
              "servings": 2,
              "ingredients": [
                {
                  "name": "string",
                  "amount": 1.0,
                  "unit": "pcs",
                  "note": "optional short note"
                }
              ],
              "steps": [
                "string",
                "string"
              ]
            }

            Rules:
            - Use metric-friendly everyday units such as g, kg, ml, l, tbsp, tsp, cup, can, jar, pcs.
            - Every ingredient must include a positive numeric amount and a unit.
            - Keep steps concise and actionable.
            - Prefer pantry items whenever possible.
            - Make the title specific and non-generic.
        """.trimIndent()
    }
}