// Manages saving, loading, listing, and deleting locally stored recipe files in JSON format.
package com.example.pantrychef.core

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

class RecipesRepository(
    private val appContext: Context,
    private val json: Json
) {
    private val dir: File by lazy {
        File(appContext.filesDir, "recipes").also { if (!it.exists()) it.mkdirs() }
    }

    @Serializable
    data class RecipePayload(
        val id: String,
        val title: String,
        val createdAt: Long,
        val text: String
    )

    data class SavedRecipe(
        val id: String,
        val title: String,
        val createdAt: Long
    ) {
        val dateString: String get() {
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            return sdf.format(createdAt)
        }
    }

    suspend fun saveRecipe(title: String, text: String): String {
        val safeTitle = title.ifBlank { "Untitled" }
            .take(60)
            .replace(Regex("""[^\w\s-]"""), "")
            .replace(" ", "_")
        val id = "${System.currentTimeMillis()}_$safeTitle"
        val payload = RecipePayload(
            id = id,
            title = title.ifBlank { "Untitled" },
            createdAt = System.currentTimeMillis(),
            text = text
        )
        val f = File(dir, "$id.json")
        f.writeText(json.encodeToString(RecipePayload.serializer(), payload))
        return id
    }

    suspend fun listRecipes(): List<SavedRecipe> {
        return dir.listFiles { file -> file.extension == "json" }
            ?.mapNotNull { f ->
                runCatching {
                    val payload = json.decodeFromString(RecipePayload.serializer(), f.readText())
                    SavedRecipe(payload.id, payload.title, payload.createdAt)
                }.getOrNull()
            }
            ?.sortedByDescending { it.createdAt }
            ?: emptyList()
    }

    suspend fun loadRecipe(id: String): RecipePayload? {
        val f = File(dir, "$id.json")
        if (!f.exists()) return null
        return runCatching {
            json.decodeFromString(RecipePayload.serializer(), f.readText())
        }.getOrNull()
    }

    suspend fun deleteRecipe(id: String) {
        val f = File(dir, "$id.json")
        if (f.exists()) f.delete()
    }
}
