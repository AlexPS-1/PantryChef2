// Full-screen recipe result UI: displays parsed recipe, scales servings, supports Ask AI (Q&A/Rewrite), copy/share, and saving.
package com.example.pantrychef.ui.recipe

import android.content.Intent
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.pantrychef.ui.pantry.PantryViewModel
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import com.example.pantrychef.core.GeminiHttp
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.foundation.shape.RoundedCornerShape

@EntryPoint
@InstallIn(SingletonComponent::class)
interface GeminiEntryPoint {
    fun gemini(): GeminiHttp
}

private enum class AskMode { QA, REWRITE }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeResultScreen(
    viewModel: PantryViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val title by viewModel.recipeTitle.collectAsState()
    val oneLiner by viewModel.oneLiner.collectAsState()
    val pantryIngredients by viewModel.pantryIngredients.collectAsState()
    val extraIngredients by viewModel.extraIngredients.collectAsState()
    val recipeText by viewModel.recipeText.collectAsState()
    val recipeSteps by viewModel.recipeSteps.collectAsState()
    val isSaved by viewModel.isCurrentRecipeSaved.collectAsState(initial = false)

    val hasContent = recipeText.isNotBlank()

    var askOpen by remember { mutableStateOf(false) }

    var servings by remember { mutableIntStateOf(2) }
    val scaleFactor = remember(servings) { (servings.coerceAtLeast(1) / 2.0) }

    val clipboard = LocalClipboardManager.current
    val ctx = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Recipe", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } },
                actions = {
                    if (hasContent) {
                        TextButton(onClick = { askOpen = true }) {
                            Icon(Icons.Filled.QuestionAnswer, contentDescription = "Ask AI")
                            Spacer(Modifier.width(6.dp))
                            Text("Ask")
                        }
                    }
                }
            )
        }
    ) { inner ->
        Column(
            modifier = Modifier
                .padding(inner)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (!hasContent) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 64.dp, bottom = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "cooking something up...",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                return@Column
            }

            Text(
                text = title.ifBlank { "PantryChef Recipe" },
                style = MaterialTheme.typography.headlineSmall
            )
            if (oneLiner.isNotBlank()) {
                Text(
                    text = oneLiner,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            ActionRow(
                isSaved = isSaved,
                onToggleSave = { viewModel.toggleSaveCurrentRecipe() },
                onCopy = {
                    clipboard.setText(
                        AnnotatedString(
                            buildShareText(
                                title, oneLiner, pantryIngredients, extraIngredients, recipeSteps
                            )
                        )
                    )
                },
                onShare = {
                    val share = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(
                            Intent.EXTRA_TEXT,
                            buildShareText(
                                title, oneLiner, pantryIngredients, extraIngredients, recipeSteps
                            )
                        )
                    }
                    ctx.startActivity(Intent.createChooser(share, "Share recipe via"))
                },
                onAsk = { askOpen = true }
            )

            Surface(
                tonalElevation = 2.dp,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Servings", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.weight(1f))
                    OutlinedButton(onClick = { servings = (servings - 1).coerceAtLeast(1) }) {
                        Text("-")
                    }
                    Text("$servings", style = MaterialTheme.typography.titleMedium)
                    OutlinedButton(onClick = { servings += 1 }) { Text("+") }
                }
            }

            if (pantryIngredients.isNotEmpty() || extraIngredients.isNotEmpty()) {
                Text("Ingredients", style = MaterialTheme.typography.titleLarge)
                if (pantryIngredients.isNotEmpty()) {
                    Text("From your pantry", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(6.dp))
                    pantryIngredients.forEach { line ->
                        Text("• ${scaleLine(line, scaleFactor)}")
                    }
                    HorizontalDivider(Modifier.padding(vertical = 10.dp))
                }
                if (extraIngredients.isNotEmpty()) {
                    Text("Additions", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(6.dp))
                    extraIngredients.forEach { line ->
                        Text("• ${scaleLine(line, scaleFactor)}")
                    }
                    HorizontalDivider(Modifier.padding(vertical = 10.dp))
                }
            }

            if (recipeSteps.isNotBlank()) {
                Text("Steps", style = MaterialTheme.typography.titleLarge)
                Text(recipeSteps, style = MaterialTheme.typography.bodyLarge)
            }

            Spacer(Modifier.height(12.dp))
            FilledTonalButton(
                onClick = { askOpen = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Icon(Icons.Filled.QuestionAnswer, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Ask AI about this recipe")
            }
            Spacer(Modifier.height(8.dp))
        }
    }

    if (askOpen) {
        AskAiDialog(
            baseRecipe = {
                buildShareText(title, oneLiner, pantryIngredients, extraIngredients, recipeSteps)
            },
            onApplyUpdatedRecipe = { updated ->
                viewModel.applyAiAnswer(updated)
            },
            onDismiss = { askOpen = false }
        )
    }
}

@Composable
private fun ActionRow(
    isSaved: Boolean,
    onToggleSave: () -> Unit,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onAsk: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilledTonalButton(onClick = onToggleSave) {
            Icon(Icons.Filled.Star, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(if (isSaved) "Saved" else "Save")
        }
        OutlinedButton(onClick = onCopy) {
            Icon(Icons.Filled.ContentCopy, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Copy")
        }
        OutlinedButton(onClick = onShare) {
            Icon(Icons.Filled.Share, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Share")
        }
        Spacer(Modifier.weight(1f))
        OutlinedButton(onClick = onAsk) {
            Icon(Icons.Filled.QuestionAnswer, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Ask AI")
        }
    }
}

private fun buildShareText(
    title: String,
    oneLiner: String,
    pantry: List<String>,
    extras: List<String>,
    steps: String
): String {
    val sb = StringBuilder()
    if (title.isNotBlank()) sb.appendLine(title)
    if (oneLiner.isNotBlank()) sb.appendLine(oneLiner).appendLine()
    if (pantry.isNotEmpty() || extras.isNotEmpty()) {
        sb.appendLine("Ingredients")
        if (pantry.isNotEmpty()) {
            sb.appendLine("From your pantry")
            pantry.forEach { sb.appendLine("• $it") }
            sb.appendLine()
        }
        if (extras.isNotEmpty()) {
            sb.appendLine("Additions")
            extras.forEach { sb.appendLine("• $it") }
            sb.appendLine()
        }
    }
    if (steps.isNotBlank()) {
        sb.appendLine("Steps")
        sb.append(steps.trim())
    }
    return sb.toString().trim()
}

private fun scaleLine(line: String, factor: Double): String {
    val regex = Regex("""^\s*(\d+(?:[.,]\d+)?)(.*)$""")
    val m = regex.find(line) ?: return line
    val qtyStr = m.groupValues[1].replace(',', '.')
    val rest = m.groupValues[2]
    val qty = qtyStr.toDoubleOrNull() ?: return line
    val scaled = qty * factor
    val pretty = if (kotlin.math.abs(scaled - scaled.roundToInt()) < 0.01) {
        scaled.roundToInt().toString()
    } else {
        ((scaled * 10.0).roundToInt() / 10.0).toString()
    }
    return "$pretty$rest"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AskAiDialog(
    baseRecipe: () -> String,
    onApplyUpdatedRecipe: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val entryPoint = remember(context) {
        EntryPointAccessors.fromApplication(context.applicationContext, GeminiEntryPoint::class.java)
    }
    val gemini = remember { entryPoint.gemini() }

    var mode by remember { mutableStateOf(AskMode.QA) }
    var question by remember { mutableStateOf(TextFieldValue("")) }
    var answer by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current

    fun isValidRecipeText(text: String): Boolean {
        if (text.isBlank()) return false
        val hasIngredients = Regex("""(?im)^\s*ingredients\b.*$""").containsMatchIn(text)
        val hasSteps = Regex("""(?im)^\s*steps\b.*$""").containsMatchIn(text)
        return hasIngredients && hasSteps
    }
    val validAnswer by derivedStateOf { answer?.let { isValidRecipeText(it) } == true }

    val qaSuggestions = listOf(
        "How long to cook the pasta?",
        "How should I cut the onions?",
        "Can I make it gluten-free?",
        "Pan frying temperature?"
    )
    val rewriteSuggestions = listOf(
        "Make it vegetarian and remove bacon.",
        "Add 1 tsp chili flakes, reduce garlic.",
        "Scale to 4 servings and simplify step 3.",
        "Swap cream for coconut milk."
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            if (mode == AskMode.REWRITE && validAnswer) {
                FilledTonalButton(
                    onClick = {
                        val updated = answer!!.trim()
                        onApplyUpdatedRecipe(updated)
                        onDismiss()
                    }
                ) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Apply changes")
                }
            } else {
                TextButton(
                    onClick = {
                        if (question.text.isBlank()) return@TextButton
                        loading = true
                        answer = null
                        scope.launch {
                            val prompt = when (mode) {
                                AskMode.QA -> buildQAPrompt(baseRecipe(), question.text)
                                AskMode.REWRITE -> buildRewritePrompt(baseRecipe(), question.text)
                            }
                            val resp = runCatching { gemini.generateRecipe(prompt) }
                                .getOrElse { "Sorry—couldn’t get an answer right now." }
                            answer = resp
                            loading = false
                        }
                    },
                    enabled = !loading
                ) { Text(if (loading) "Asking…" else "Ask") }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !loading) { Text("Close") } },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(
                            Icons.Filled.Lightbulb,
                            contentDescription = null,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                    Column(Modifier.weight(1f)) {
                        Text(
                            if (mode == AskMode.QA) "Ask AI (Q&A)" else "Ask AI (Revise recipe)",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            if (mode == AskMode.QA)
                                "Get short, practical answers about timing, cuts, swaps."
                            else
                                "Describe your changes; AI returns a full revised recipe.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = mode == AskMode.QA,
                        onClick = { mode = AskMode.QA; answer = null },
                        label = { Text("Q&A") }
                    )
                    FilterChip(
                        selected = mode == AskMode.REWRITE,
                        onClick = { mode = AskMode.REWRITE; answer = null },
                        label = { Text("Revise recipe") }
                    )
                }

                OutlinedTextField(
                    value = question,
                    onValueChange = { question = it },
                    label = { Text(if (mode == AskMode.QA) "Ask about the recipe" else "Describe the changes you want") },
                    singleLine = false,
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        if (question.text.isNotBlank() && !loading) {
                            IconButton(onClick = { question = TextFieldValue("") }) {
                                Icon(Icons.Filled.Clear, contentDescription = "Clear")
                            }
                        }
                    }
                )

                val suggestions = if (mode == AskMode.QA) qaSuggestions else rewriteSuggestions
                Row(
                    modifier = Modifier
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    suggestions.forEach { s ->
                        AssistChip(
                            onClick = { question = TextFieldValue(s) },
                            label = { Text(s) }
                        )
                    }
                }

                if (loading) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                        CircularProgressIndicator()
                    }
                }

                if (!loading && !answer.isNullOrBlank()) {
                    if (mode == AskMode.REWRITE && validAnswer) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "Full recipe detected — you can Apply changes.",
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }

                    Surface(
                        tonalElevation = 2.dp,
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 0.dp, max = 320.dp)
                                .verticalScroll(rememberScrollState())
                                .padding(12.dp)
                        ) {
                            Text(
                                answer!!,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { clipboard.setText(AnnotatedString(answer!!)) }) {
                            Text("Copy")
                        }
                    }

                    if (mode == AskMode.REWRITE && !validAnswer) {
                        Text(
                            "Tip: ask for a revised recipe that includes Ingredients and Steps.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    )
}

private fun buildQAPrompt(baseRecipe: String, question: String): String = """
You are PantryChef, a cheerful, witty, slightly mischievous kitchen companion.

Answer the user's question about the EXISTING recipe below.
- Be brief, specific, and practical (1–3 short sentences).
- If timing/temps/cuts are asked, give concrete numbers or simple technique tips.
- Do NOT rewrite the whole recipe.
- Plain text only (no markdown, no emojis).

Recipe:
$baseRecipe

User question:
$question
""".trimIndent()

private fun buildRewritePrompt(baseRecipe: String, question: String): String = """
You are PantryChef — a witty, slightly mischievous kitchen companion.

REWRITE THE ENTIRE RECIPE to satisfy the user's request. Output must be plain text (no markdown, no emojis) in this exact order:

Title
A playful one-liner
Ingredients
- Bullet list, each line starts with "- " and includes a realistic quantity + metric unit (g, ml, tbsp, tsp, pcs).
Steps
1. Numbered, concise, practical steps.

Rules:
- Apply all requested changes. If asked to remove ingredients, remove them from BOTH ingredients and steps.
- Keep quantities consistent and realistic; adjust related steps accordingly.
- Keep the original voice/tone and ~2 servings baseline unless changed.

Current recipe:
$baseRecipe

Change request:
$question
""".trimIndent()
