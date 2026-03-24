package com.example.pantrychef.ui.pantry

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pantrychef.core.GeminiHttp
import com.example.pantrychef.core.RecipePreferences
import com.example.pantrychef.core.RecipesRepository
import com.example.pantrychef.data.local.PantryDao
import com.example.pantrychef.data.local.PantryItem
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

@HiltViewModel
class PantryViewModel @Inject constructor(
    private val pantryDao: PantryDao,
    private val geminiHttp: GeminiHttp,
    private val recipesRepository: RecipesRepository
) : ViewModel() {

    val items: StateFlow<List<PantryItem>> = pantryDao.observeAll()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _recipeText = MutableStateFlow("")
    val recipeText: StateFlow<String> = _recipeText

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating

    private val _savedRecipes = MutableStateFlow<List<RecipesRepository.SavedRecipe>>(emptyList())
    val savedRecipes: StateFlow<List<RecipesRepository.SavedRecipe>> = _savedRecipes

    private var prefs: RecipePreferences = RecipePreferences()

    init {
        refreshSavedRecipes()
    }

    data class Parsed(
        val title: String = "",
        val oneLiner: String = "",
        val ingredients: List<String> = emptyList(),
        val steps: String = ""
    )

    private fun parseRecipe(text: String): Parsed {
        if (text.isBlank()) return Parsed()

        val lines = text.lines()
        var i = 0

        var title = ""
        while (i < lines.size && title.isBlank()) {
            val t = lines[i].trim()
            if (t.isNotBlank() && !t.equals("title", ignoreCase = true)) {
                title = t
            }
            i++
        }

        var one = ""
        while (i < lines.size && one.isBlank()) {
            val t = lines[i].trim()
            if (
                t.isNotBlank() &&
                !t.startsWith("ingredients", ignoreCase = true) &&
                !t.startsWith("steps", ignoreCase = true)
            ) {
                one = t
                break
            }
            i++
        }

        fun idxOfHeader(name: String): Int {
            return lines.indexOfFirst { it.trim().startsWith(name, ignoreCase = true) }
        }

        val idxIng = idxOfHeader("ingredients")
        val idxSteps = idxOfHeader("steps")

        val ingredients = if (idxIng != -1) {
            val end = if (idxSteps != -1 && idxSteps > idxIng) idxSteps else lines.size
            lines.subList(idxIng + 1, end)
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .map {
                    if (it.startsWith("- ")) {
                        it.substring(2).trim()
                    } else {
                        it.trimStart('-', '•', ' ').trim()
                    }
                }
        } else {
            emptyList()
        }

        val stepsText = if (idxSteps != -1) {
            lines.drop(idxSteps + 1).joinToString("\n").trim()
        } else {
            val start = if (idxIng != -1) idxIng + 1 + ingredients.size else i
            lines.drop(start).joinToString("\n").trim()
        }

        return Parsed(
            title = title,
            oneLiner = one,
            ingredients = ingredients,
            steps = stepsText
        )
    }

    private fun splitPantryVsAdditions(
        ingredients: List<String>,
        pantry: List<PantryItem>
    ): Pair<List<String>, List<String>> {
        if (ingredients.isEmpty()) return emptyList<String>() to emptyList()

        val pantryNames = pantry.map { it.name.lowercase() }
        val pantrySet = pantryNames.toSet()

        fun matchesPantry(line: String): Boolean {
            val lowered = line.lowercase()
            return pantrySet.any { token ->
                token.isNotBlank() && lowered.contains(token)
            }
        }

        val inPantry = mutableListOf<String>()
        val additions = mutableListOf<String>()

        for (ingredient in ingredients) {
            if (matchesPantry(ingredient)) {
                inPantry += ingredient
            } else {
                additions += ingredient
            }
        }

        return inPantry to additions
    }

    val recipeTitle: StateFlow<String> = recipeText
        .map { parseRecipe(it).title }
        .stateIn(viewModelScope, SharingStarted.Eagerly, "")

    val oneLiner: StateFlow<String> = recipeText
        .map { parseRecipe(it).oneLiner }
        .stateIn(viewModelScope, SharingStarted.Eagerly, "")

    val pantryIngredients: StateFlow<List<String>> = combine(recipeText, items) { text, pantryItems ->
        val parsed = parseRecipe(text)
        splitPantryVsAdditions(parsed.ingredients, pantryItems).first
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val extraIngredients: StateFlow<List<String>> = combine(recipeText, items) { text, pantryItems ->
        val parsed = parseRecipe(text)
        splitPantryVsAdditions(parsed.ingredients, pantryItems).second
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val recipeSteps: StateFlow<String> = recipeText
        .map { parseRecipe(it).steps }
        .stateIn(viewModelScope, SharingStarted.Eagerly, "")

    val isCurrentRecipeSaved: StateFlow<Boolean> = combine(recipeText, savedRecipes) { text, saved ->
        val currentTitle = parseRecipe(text).title
        currentTitle.isNotBlank() && saved.any { it.title == currentTitle }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    fun insertItem(name: String, quantity: Double, unit: String) {
        viewModelScope.launch {
            pantryDao.insert(
                PantryItem(
                    id = 0L,
                    name = name.trim(),
                    quantity = quantity,
                    unit = unit.trim()
                )
            )
        }
    }

    fun deleteItem(id: Long) {
        viewModelScope.launch {
            pantryDao.deleteById(id)
        }
    }

    fun updateItem(id: Long, name: String, quantity: Double, unit: String) {
        viewModelScope.launch {
            pantryDao.updateById(
                id = id,
                name = name.trim(),
                quantity = quantity,
                unit = unit.trim()
            )
        }
    }

    fun generateRecipes() {
        viewModelScope.launch {
            _isGenerating.value = true
            _recipeText.value = ""

            val pantryLines = items.value.joinToString("\n") {
                "- ${it.name} ${it.quantity} ${it.unit}"
            }.ifBlank {
                "- (empty pantry)"
            }

            fun joinOrEmpty(label: String, xs: List<String>): String {
                return if (xs.isEmpty()) "" else "$label: ${xs.joinToString(", ")}\n"
            }

            val softGuidance = buildString {
                append(joinOrEmpty("Meal time", prefs.mealTime))
                append(joinOrEmpty("Style & speed", prefs.styleSpeed))
                append(joinOrEmpty("Diet", prefs.diet))
                append(joinOrEmpty("Cuisine", prefs.cuisine))
                append(joinOrEmpty("Mood", prefs.mood))
            }.trimEnd()

            val mustsLine = if (prefs.mustInclude.isNotEmpty()) {
                "MUST include ALL of these: ${prefs.mustInclude.joinToString(", ")}.\n"
            } else {
                ""
            }

            val additionsLine = when {
                prefs.maxNewAdditions <= 0 ->
                    "Do not add any new non-pantry ingredients.\n"

                else ->
                    "You may add up to ${prefs.maxNewAdditions} small non-pantry ingredients if needed (salt, pepper, oil, water are free).\n"
            }

            val personaBlock = buildPersonaBlock(
                voice = prefs.voice ?: "Playful Classic",
                spicy = prefs.spicyLanguage,
                childFriendly = prefs.childFriendly
            )

            val kidBlock = if (prefs.childFriendly) childFriendlyBlock() else ""

            val prompt = buildString {
                appendLine("You are PantryChef — a witty, slightly mischievous kitchen companion.")
                appendLine(personaBlock)

                if (kidBlock.isNotBlank()) {
                    appendLine()
                    appendLine(kidBlock)
                }

                appendLine("Create ONE creative recipe using the pantry items below.")
                appendLine("Tone: lively and charming, but keep directions practical.")
                appendLine()

                if (softGuidance.isNotBlank()) {
                    appendLine("Cooking vibe (soft guidance):")
                    appendLine(softGuidance)
                    appendLine()
                }

                append(mustsLine)
                append(additionsLine)

                appendLine("Pantry:")
                appendLine(pantryLines)
                appendLine()

                appendLine("FORMAT (you MUST follow this exactly; be human and fun):")
                appendLine("Title")
                appendLine("A playful one-liner about the dish (one sentence).")
                appendLine("Ingredients (for ~2 servings, metric units)")
                appendLine("- Each line starts with '- ' and includes a realistic quantity + unit (e.g. - 200 g spaghetti, - 1 tbsp olive oil).")
                appendLine("- Use sensible cooking quantities and everyday units (g, ml, tbsp, tsp, pcs).")
                appendLine("- Always provide a number and unit for EVERY ingredient.")
                appendLine("Steps")
                appendLine("1. ... encouraging, conversational, concise.")
                appendLine("2. ...")
                appendLine()
                appendLine("STYLE CHECKLIST (you MUST satisfy all):")
                appendLine("- Match the selected Voice’s vibe and include at least TWO short stylistic phrases from the persona’s examples.")
                appendLine("- Keep sentences punchy (max ~2 per step).")
                appendLine("- No markdown, no emojis, no innuendo, no profanity. Keep it PG and clever.")
                appendLine("- Never include hate speech or slurs.")
            }

            try {
                val ai = withTimeoutOrNull(30_000) {
                    geminiHttp.generateRecipe(prompt)
                }

                val text = when {
                    ai == null -> buildGenerationErrorRecipe(
                        "PantryChef timed out while talking to Gemini. Try again in a moment."
                    )

                    ai.isBlank() -> buildGenerationErrorRecipe(
                        "Gemini returned an empty response. Try again."
                    )

                    else -> enforceNonEmptyTitle(ai, items.value, prefs)
                }

                _recipeText.value = text
            } catch (t: Throwable) {
                _recipeText.value = buildGenerationErrorRecipe(
                    message = t.message ?: "Unknown Gemini error."
                )
            } finally {
                _isGenerating.value = false
            }
        }
    }

    fun applyAiAnswer(updatedRecipeText: String) {
        val cleaned = updatedRecipeText.trim()
        if (cleaned.isBlank()) return
        _recipeText.value = enforceNonEmptyTitle(cleaned, items.value, prefs)
    }

    private fun enforceNonEmptyTitle(
        raw: String,
        pantry: List<PantryItem>,
        prefs: RecipePreferences
    ): String {
        val lines = raw.lines().toMutableList()
        val firstIdx = lines.indexOfFirst { it.isNotBlank() }

        if (firstIdx == -1) {
            return buildFallbackTitle(pantry, prefs)
        }

        val first = lines[firstIdx].trim()
        val looksLikeEmptyTitle = first.equals("title", ignoreCase = true) || first.isBlank()

        if (!looksLikeEmptyTitle) return raw

        val fallback = buildFallbackTitle(pantry, prefs)
        lines[firstIdx] = fallback
        return lines.joinToString("\n")
    }

    private fun buildFallbackTitle(
        pantry: List<PantryItem>,
        prefs: RecipePreferences
    ): String {
        val main = pantry.maxByOrNull { it.quantity }
            ?.name
            ?.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }

        val cuisine = prefs.cuisine.firstOrNull()
        val meal = prefs.mealTime.firstOrNull()

        return when {
            main != null && cuisine != null -> "$cuisine $main, Pantry-Style"
            main != null && meal != null -> "$meal $main, Quick & Easy"
            main != null -> "Quick $main Surprise"
            cuisine != null -> "$cuisine Pantry Special"
            else -> "PantryChef Special"
        }
    }

    private fun buildGenerationErrorRecipe(message: String): String {
        return """
            PantryChef Hit a Snag
            We couldn't generate a recipe this time, but your pantry items are still safe and ready.

            Ingredients
            - 1 pcs patience
            - 1 pcs retry button
            - 1 splash internet check

            Steps
            1. Read this note: $message
            2. Check that GEMINI_API_KEY exists in local.properties and that the device has internet access.
            3. Go back and try Generate Recipe again.
        """.trimIndent()
    }

    private fun buildPersonaBlock(
        voice: String,
        spicy: Boolean,
        childFriendly: Boolean
    ): String {
        val base = when (voice) {
            "Plain (Minimal)" -> """
                VOICE: Plain Minimal
                - Straightforward, instructional, minimal adjectives.
                - No humor, no puns, no metaphors. Just precise cooking directions.
                - Keep sentences short. Avoid flourish and storytelling.
            """.trimIndent()

            "British Pub Cook" -> """
                VOICE: British Pub Cook
                - Hearty, cozy, cheeky. No faff, big comfort. Pub slang welcome.
                - Sample phrases (use at least two): "Give it a proper stir", "Right then", "That’ll do nicely", "On the hob", "Tuck in".
            """.trimIndent()

            "California Wellness" -> """
                VOICE: California Wellness
                - Breezy, wholesome, farmers-market fresh. Uplifting and balanced.
                - Sample phrases (use at least two): "Lift with lemon", "Fresh and vibrant", "Gentle heat", "Clean finish", "Feel-good energy".
            """.trimIndent()

            "Italian Nonna" -> """
                VOICE: Italian Nonna
                - Warm, loving, a tiny bit bossy — simple moves done perfectly; family-table vibes.
                - Sample phrases (use at least two): "Ascolta", "Basta", "Capito?", "Save the pasta water", "Piano, piano".
            """.trimIndent()

            "Science-y Food Nerd" -> """
                VOICE: Science-y Food Nerd
                - Curious lab-coat energy with charm; quick why-it-works notes.
                - Sample phrases (use at least two): "Cue Maillard magic", "Starch emulsifies", "Thermal carryover", "Umami boost", "Shear-thinning sauce".
            """.trimIndent()

            "Street-food Storyteller" -> """
                VOICE: Street-food Storyteller
                - Neon nights and sizzling carts: bold, messy, joyful, punchy rhythms.
                - Sample phrases (use at least two): "Sizzle till the edges char", "Hit it with tang", "Crunch meets heat", "Saucy drip", "Market-stall swagger".
            """.trimIndent()

            "Nordic Minimalist" -> """
                VOICE: Nordic Minimalist
                - Calm, pared-back, seasonal; few ingredients, clean technique, quiet confidence.
                - Sample phrases (use at least two): "Let it rest", "Cool clarity", "Subtle sweetness", "Clean lines", "Gentle warmth".
            """.trimIndent()

            "BBQ Pitmaster" -> """
                VOICE: BBQ Pitmaster
                - Backyard swagger: smoke, patience, bark talk; big-flavor bravado.
                - Sample phrases (use at least two): "Low and slow", "Let the smoke work", "Build that bark", "Sticky glaze", "Rest the meat".
            """.trimIndent()

            "Kitchen Nightmare (Brutally Honest)" -> """
                VOICE: Kitchen Nightmare (Brutally Honest)
                - Blunt, over-the-top critique with rapid-fire commands. High heat, zero nonsense. Sarcastic jabs OK, never hateful.
                - Sample phrases (use at least two): "Listen up", "Start again, properly", "Stop overcooking it", "Taste as you go", "It’s raw? Fix it".
            """.trimIndent()

            "Pirate" -> """
                VOICE: Pirate
                - Seafaring swagger; rollicking commands; nautical metaphors.
                - Sample phrases (use at least two): "Avast", "Hoist the heat", "Galley’s honor", "Trim the sails", "A splash o’ the briny".
            """.trimIndent()

            "Cute for Kids" -> """
                VOICE: Cute for Kids
                - Gentle, friendly, encouraging; simple words; tiny jokes about kitchen “magic”.
                - Sample phrases (use at least two): "Sprinkle a little magic", "Gentle bubbles", "Taste buddy check", "Happy stir", "Little chef high-five".
            """.trimIndent()

            else -> """
                VOICE: Playful Classic
                - Upbeat, witty, slightly mischievous; friendly kitchen confidence.
                - Sample phrases (use at least two): "Victory lap of olive oil", "Saucy little number", "Golden and gorgeous", "Big flavor, low drama".
            """.trimIndent()
        }

        val spice = if (spicy) wittySpiceLayer() else "- Keep it clean and friendly."
        val kidHint = if (childFriendly) {
            "- Keep word choices simple and friendly; avoid sarcasm and sharp burns."
        } else {
            ""
        }

        return listOf(base, spice, kidHint)
            .filter { it.isNotBlank() }
            .joinToString("\n")
    }

    private fun wittySpiceLayer(): String = """
        SPICE LAYER (PG, clever puns only):
        - Add playful heat/chaos lines (use at least one): "Bring the heat without burning the plot", "Stir up a little trouble — in the pan", "Too hot to handle? Good — that’s flavor talking", "Drama level: simmering", "Keep it saucy, keep it classy".
        - Riff with clean metaphors (sizzle, steam, chaos, rescue, plot twist, victory). No innuendo, no profanity, no body refs.
    """.trimIndent()

    private fun childFriendlyBlock(): String = """
        CHILD-FRIENDLY MODE:
        - Mild flavors and low heat; avoid spicy heat unless clearly optional.
        - Prefer familiar ingredients; suggest friendly swaps (e.g., carrot instead of chili).
        - Clear, short steps; note safe helper tasks for kids (wash, mix, garnish).
        - Texture tips: keep veggies tender-crisp, avoid mush unless intended.
        - If adding greens/veg, suggest optional tiny pieces or grated versions.
    """.trimIndent()

    fun clearRecipe() {
        _recipeText.value = ""
        _isGenerating.value = false
    }

    fun setRecipePreferences(p: RecipePreferences) {
        prefs = p
    }

    fun saveCurrentRecipe(title: String, text: String) {
        viewModelScope.launch(Dispatchers.IO) {
            recipesRepository.saveRecipe(title, text)
            _savedRecipes.value = recipesRepository.listRecipes()
        }
    }

    fun toggleSaveCurrentRecipe() {
        val currentText = recipeText.value
        if (currentText.isBlank()) return

        viewModelScope.launch(Dispatchers.IO) {
            val currentTitle = parseRecipe(currentText).title.ifBlank { "PantryChef Recipe" }
            val existing = recipesRepository.listRecipes().firstOrNull { it.title == currentTitle }

            if (existing == null) {
                recipesRepository.saveRecipe(currentTitle, currentText)
            } else {
                recipesRepository.deleteRecipe(existing.id)
            }

            _savedRecipes.value = recipesRepository.listRecipes()
        }
    }

    fun refreshSavedRecipes() {
        viewModelScope.launch(Dispatchers.IO) {
            _savedRecipes.value = recipesRepository.listRecipes()
        }
    }

    fun deleteSavedRecipe(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            recipesRepository.deleteRecipe(id)
            _savedRecipes.value = recipesRepository.listRecipes()
        }
    }

    fun openSavedRecipe(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val payload = recipesRepository.loadRecipe(id)
            if (payload != null) {
                _recipeText.value = payload.text
            }
        }
    }
}