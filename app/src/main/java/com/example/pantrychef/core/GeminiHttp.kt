package com.example.pantrychef.core

import android.graphics.Bitmap
import com.example.pantrychef.BuildConfig
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeminiHttp @Inject constructor() {

    private val model by lazy {
        GenerativeModel(
            modelName = GEMINI_MODEL_NAME,
            apiKey = BuildConfig.GEMINI_API_KEY
        )
    }

    suspend fun generateRecipe(prompt: String): String {
        val resp = model.generateContent(prompt)
        return resp.text.orEmpty()
    }

    suspend fun identifyItemCounts(images: List<Bitmap>): List<CandidateItem> {
        if (images.isEmpty()) return emptyList()

        val visionContent = content {
            images.forEach { bmp -> image(bmp) }
            text(VISION_PROMPT)
        }

        val text = runCatching { model.generateContent(visionContent).text.orEmpty() }
            .getOrDefault("")
            .trim()

        if (text.isBlank()) return emptyList()

        return parsePlainList(text)
    }

    private fun parsePlainList(text: String): List<CandidateItem> {
        val out = mutableListOf<CandidateItem>()

        text.lines()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .forEach { line ->
                val m = ITEM_LINE.find(line)
                if (m != null) {
                    val rawName = m.groupValues[1].trim()
                    val count = m.groupValues[2].toIntOrNull() ?: 1
                    val canonical = rawName.lowercase()
                        .removePrefix("-")
                        .removePrefix("•")
                        .trim()

                    val unit = defaultUnitFor(canonical)

                    out += CandidateItem(
                        name = canonical,
                        count = count.coerceAtLeast(1),
                        unit = unit,
                        confidence = 0.75f,
                        source = CandidateItem.Source.GEMINI
                    )
                }
            }

        return out.groupBy { it.name }.map { (_, items) ->
            val maxConf = items.maxBy { it.confidence }
            maxConf.copy(count = items.sumOf { it.count })
        }
    }

    private fun defaultUnitFor(name: String): String = when (name.lowercase()) {
        "pasta" -> "box"
        "noodles" -> "pack"
        "rice" -> "bag"
        "beans", "tuna" -> "can"
        "milk" -> "carton"
        "yogurt" -> "cup"
        "bread" -> "loaf"
        "eggs" -> "pcs"
        "oil" -> "bottle"
        else -> "pcs"
    }

    private companion object {
        const val GEMINI_MODEL_NAME = "gemini-3.1-flash-lite-preview"

        val ITEM_LINE = Regex(
            pattern = """^\s*[-•]?\s*([A-Za-z][A-Za-z0-9 \-_/&]*)[^0-9]*[x×]\s*([0-9]{1,3})\s*$"""
        )

        const val VISION_PROMPT =
            "From these photos, list generic pantry items with integer counts. " +
                    "Rules: no brands, no bullets unless a simple dash, no extra commentary, " +
                    "plain text only, one item per line, EXACT format: name x count. " +
                    "Examples: cucumber x 2, pasta x 1, noodles x 3."
    }
}