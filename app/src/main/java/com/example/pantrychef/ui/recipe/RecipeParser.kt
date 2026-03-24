package com.example.pantrychef.ui.recipe

import com.example.pantrychef.data.local.PantryItem
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class StructuredRecipe(
    val title: String,
    val oneLiner: String,
    val ingredients: List<StructuredIngredient>,
    val steps: List<String>,
    val servings: Int = 2
)

@Serializable
data class StructuredIngredient(
    val name: String,
    val amount: Double,
    val unit: String,
    val note: String = ""
)

data class IngredientBuckets(
    val pantry: List<StructuredIngredient>,
    val additions: List<StructuredIngredient>
)

object RecipeParser {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
    }

    private val fencedJsonRegex = Regex(
        pattern = "```(?:json)?\\s*(\\{[\\s\\S]*?\\})\\s*```",
        options = setOf(RegexOption.IGNORE_CASE)
    )

    private val pantryAliases: Map<String, Set<String>> = mapOf(
        "bell pepper" to setOf("capsicum", "pepper"),
        "scallion" to setOf("spring onion", "green onion"),
        "chickpeas" to setOf("garbanzo beans", "garbanzo"),
        "cilantro" to setOf("coriander"),
        "zucchini" to setOf("courgette"),
        "eggplant" to setOf("aubergine"),
        "cornstarch" to setOf("corn flour"),
        "ground beef" to setOf("minced beef", "mince"),
        "pasta" to setOf("spaghetti", "penne", "fusilli", "linguine", "noodles"),
        "rice" to setOf("basmati", "jasmine rice"),
        "cheese" to setOf("cheddar", "mozzarella", "parmesan"),
        "tomato" to setOf("tomatoes", "cherry tomatoes"),
        "onion" to setOf("red onion", "yellow onion", "white onion"),
        "oil" to setOf("olive oil", "vegetable oil", "sunflower oil")
    )

    fun fromModelText(text: String): StructuredRecipe? {
        val jsonPayload = extractJsonPayload(text) ?: return null
        return runCatching {
            json.decodeFromString(StructuredRecipe.serializer(), jsonPayload)
        }.getOrNull()?.normalized()
    }

    fun fromSavedText(text: String): StructuredRecipe? {
        return fromModelText(text) ?: parseLegacyDisplayText(text)
    }

    fun toDisplayText(recipe: StructuredRecipe): String {
        return buildString {
            appendLine(recipe.title)
            appendLine(recipe.oneLiner)
            appendLine()
            appendLine("Ingredients")
            recipe.ingredients.forEach { ingredient ->
                val amountText = if (ingredient.amount % 1.0 == 0.0) {
                    ingredient.amount.toInt().toString()
                } else {
                    ingredient.amount.toString()
                }

                val noteSuffix = ingredient.note
                    .trim()
                    .takeIf { it.isNotBlank() }
                    ?.let { " ($it)" }
                    .orEmpty()

                appendLine("- $amountText ${ingredient.unit} ${ingredient.name}$noteSuffix")
            }
            appendLine()
            appendLine("Steps")
            recipe.steps.forEachIndexed { index, step ->
                appendLine("${index + 1}. $step")
            }
        }.trim()
    }

    fun partitionIngredients(
        ingredients: List<StructuredIngredient>,
        pantryItems: List<PantryItem>
    ): IngredientBuckets {
        val pantryIndex = pantryItems
            .map { normalizeIngredientName(it.name) }
            .filter { it.isNotBlank() }

        val pantryMatches = mutableListOf<StructuredIngredient>()
        val additions = mutableListOf<StructuredIngredient>()

        ingredients.forEach { ingredient ->
            val normalizedIngredient = normalizeIngredientName(ingredient.name)
            val ingredientTokens = normalizedIngredient.split(" ")
                .filter { it.isNotBlank() }
                .toSet()

            val matches = pantryIndex.any { pantryName ->
                val pantryTokens = pantryName.split(" ")
                    .filter { it.isNotBlank() }
                    .toSet()

                normalizedIngredient == pantryName ||
                        normalizedIngredient.contains(pantryName) ||
                        pantryName.contains(normalizedIngredient) ||
                        tokenOverlap(ingredientTokens, pantryTokens) >= 0.6f ||
                        aliasesMatch(normalizedIngredient, pantryName)
            }

            if (matches) {
                pantryMatches += ingredient
            } else {
                additions += ingredient
            }
        }

        return IngredientBuckets(
            pantry = pantryMatches,
            additions = additions
        )
    }

    private fun extractJsonPayload(text: String): String? {
        val fenced = fencedJsonRegex
            .find(text)
            ?.groupValues
            ?.getOrNull(1)
            ?.trim()

        if (!fenced.isNullOrBlank()) return fenced

        val start = text.indexOf('{')
        val end = text.lastIndexOf('}')
        if (start == -1 || end == -1 || end <= start) return null

        return text.substring(start, end + 1).trim()
    }

    private fun parseLegacyDisplayText(text: String): StructuredRecipe? {
        if (text.isBlank()) return null

        val lines = text.lines()
        var index = 0

        var title = ""
        while (index < lines.size && title.isBlank()) {
            val line = lines[index].trim()
            if (line.isNotBlank() && !line.equals("title", ignoreCase = true)) {
                title = line
            }
            index++
        }

        var oneLiner = ""
        while (index < lines.size && oneLiner.isBlank()) {
            val line = lines[index].trim()
            if (
                line.isNotBlank() &&
                !line.startsWith("ingredients", ignoreCase = true) &&
                !line.startsWith("steps", ignoreCase = true)
            ) {
                oneLiner = line
            }
            index++
        }

        val ingredientHeader = lines.indexOfFirst {
            it.trim().startsWith("ingredients", ignoreCase = true)
        }
        val stepsHeader = lines.indexOfFirst {
            it.trim().startsWith("steps", ignoreCase = true)
        }

        val ingredientLines = if (ingredientHeader != -1) {
            val end = if (stepsHeader > ingredientHeader) stepsHeader else lines.size
            lines.subList(ingredientHeader + 1, end)
                .map { it.trim().trimStart('-', '•').trim() }
                .filter { it.isNotBlank() }
        } else {
            emptyList()
        }

        val ingredients = ingredientLines.map { line ->
            parseLegacyIngredient(line)
        }

        val steps = if (stepsHeader != -1) {
            lines.drop(stepsHeader + 1)
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .map { it.removePrefix("${it.takeWhile(Char::isDigit)}.").trim() }
        } else {
            emptyList()
        }

        if (title.isBlank() || ingredients.isEmpty() || steps.isEmpty()) return null

        return StructuredRecipe(
            title = title,
            oneLiner = oneLiner,
            ingredients = ingredients,
            steps = steps,
            servings = 2
        ).normalized()
    }

    private fun parseLegacyIngredient(line: String): StructuredIngredient {
        val tokens = line.split(" ").filter { it.isNotBlank() }
        val amount = tokens.firstOrNull()?.replace(",", ".")?.toDoubleOrNull() ?: 1.0
        val unit = tokens.getOrNull(1) ?: "pcs"
        val name = tokens.drop(2).joinToString(" ").ifBlank { line }

        return StructuredIngredient(
            name = name.trim(),
            amount = amount,
            unit = unit.trim(),
            note = ""
        )
    }

    private fun StructuredRecipe.normalized(): StructuredRecipe {
        return copy(
            title = title.trim().ifBlank { "PantryChef Special" },
            oneLiner = oneLiner.trim(),
            ingredients = ingredients
                .map { ingredient ->
                    ingredient.copy(
                        name = ingredient.name.trim(),
                        amount = ingredient.amount.takeIf { it > 0 } ?: 1.0,
                        unit = ingredient.unit.trim().ifBlank { "pcs" },
                        note = ingredient.note.trim()
                    )
                }
                .filter { it.name.isNotBlank() },
            steps = steps
                .map { it.trim() }
                .filter { it.isNotBlank() }
        )
    }

    private fun normalizeIngredientName(raw: String): String {
        val lowered = raw.lowercase()
            .replace(Regex("[^a-z0-9 ]"), " ")
            .replace(Regex("\\b(chopped|diced|minced|fresh|large|small|medium|extra|virgin)\\b"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()

        return singularize(lowered)
    }

    private fun singularize(value: String): String {
        return value.split(" ")
            .map { token ->
                when {
                    token.endsWith("ies") && token.length > 3 -> token.dropLast(3) + "y"
                    token.endsWith("oes") && token.length > 3 -> token.dropLast(2)
                    token.endsWith("s") && !token.endsWith("ss") && token.length > 3 -> token.dropLast(1)
                    else -> token
                }
            }
            .joinToString(" ")
    }

    private fun tokenOverlap(a: Set<String>, b: Set<String>): Float {
        if (a.isEmpty() || b.isEmpty()) return 0f
        val overlap = a.intersect(b).size.toFloat()
        return overlap / minOf(a.size, b.size).toFloat()
    }

    private fun aliasesMatch(ingredient: String, pantry: String): Boolean {
        val ingredientSet = pantryAliases[ingredient].orEmpty() + ingredient
        val pantrySet = pantryAliases[pantry].orEmpty() + pantry
        return ingredientSet.any { it == pantry } ||
                pantrySet.any { it == ingredient } ||
                ingredientSet.intersect(pantrySet).isNotEmpty()
    }
}