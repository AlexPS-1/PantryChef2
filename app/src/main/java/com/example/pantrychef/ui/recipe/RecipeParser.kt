// Utilities to parse AI-generated recipe text into title, one-liner, ingredients, steps, and pantry partitioning.
package com.example.pantrychef.ui.recipe

import com.example.pantrychef.data.local.PantryItem

object RecipeParser {
    fun extractTitle(text: String): String {
        val first = text.lines().firstOrNull()?.trim().orEmpty()
        return first
    }

    fun extractOneLiner(text: String): String {
        val lines = text.lines()
        for (i in 1 until lines.size) {
            val t = lines[i].trim()
            if (t.startsWith("Ingredients", ignoreCase = true)) break
            if (t.isNotBlank()) return t
        }
        return ""
    }

    fun extractIngredients(text: String): List<String> {
        val lines = text.lines()
        val stepsIdx = lines.indexOfFirst { it.trim().startsWith("Steps", ignoreCase = true) }
            .let { if (it == -1) lines.size else it }

        val headerIdx = lines.indexOfFirst { ln ->
            val t = ln.trim()
            t.equals("Ingredients", ignoreCase = true) ||
                    t.equals("Ingredients:", ignoreCase = true) ||
                    t.startsWith("Ingredients", ignoreCase = true) && t.removePrefix("Ingredients").trim().let { it.isEmpty() || it == ":" || it == "-" }
        }

        if (headerIdx != -1) {
            val (primary, fallback) = readBlock(lines, from = headerIdx + 1, to = stepsIdx)
            return if (primary.isNotEmpty()) primary else fallback
        }

        val (primaryFallback, looseFallback) = detectListishBlock(lines, to = stepsIdx)
        return if (primaryFallback.isNotEmpty()) primaryFallback else looseFallback
    }

    fun extractSteps(text: String): List<String> {
        val lines = text.lines()
        val start = lines.indexOfFirst { it.trim().startsWith("Steps", ignoreCase = true) }
        if (start == -1) return emptyList()
        val out = ArrayList<String>()
        for (i in (start + 1) until lines.size) {
            val raw = lines[i].trim()
            if (raw.firstOrNull()?.isDigit() == true && raw.drop(1).trimStart().startsWith(".")) {
                out.add(raw.substringAfter(".").trim())
            } else if (raw.isBlank()) {
                continue
            } else {
                if (out.isNotEmpty()) out[out.lastIndex] = out.last() + " " + raw
            }
        }
        return out
    }

    fun partitionByPantry(
        ingredients: List<String>,
        pantry: List<PantryItem>
    ): Pair<List<String>, List<String>> {
        if (ingredients.isEmpty()) return Pair(emptyList(), emptyList())
        val names = pantry.map { it.name.lowercase() }.sortedByDescending { it.length }
        val pantryHits = mutableListOf<String>()
        val additions = mutableListOf<String>()
        for (line in ingredients) {
            val lower = line.lowercase()
            val match = names.firstOrNull { n -> n.isNotBlank() && lower.contains(n) }
            if (match != null) pantryHits += line else additions += line
        }
        return Pair(pantryHits, additions)
    }

    private fun readBlock(lines: List<String>, from: Int, to: Int): Pair<List<String>, List<String>> {
        val primary = ArrayList<String>()
        val fallback = ArrayList<String>()
        for (i in from until to) {
            val raw = lines[i].trim()
            if (raw.startsWith("-")) {
                primary.add(raw.removePrefix("-").trim())
            } else if (raw.isNotBlank()) {
                fallback.add(raw)
            }
        }
        return primary to fallback
    }

    private fun detectListishBlock(lines: List<String>, to: Int): Pair<List<String>, List<String>> {
        val bullets = ArrayList<String>()
        var inBullets = false
        for (i in 0 until to) {
            val raw = lines[i].trim()
            if (raw.startsWith("-")) {
                inBullets = true
                bullets.add(raw.removePrefix("-").trim())
            } else if (inBullets) {
                break
            }
        }
        if (bullets.isNotEmpty()) return bullets to emptyList()

        val loose = ArrayList<String>()
        var started = false
        for (i in 0 until to) {
            val raw = lines[i].trim()
            if (raw.isNotBlank() && raw.length <= 80 && !raw.startsWith("Title", true) && !raw.startsWith("Fun one-liner", true) && !raw.startsWith("Steps", true)) {
                loose.add(raw)
                started = true
            } else if (started) {
                break
            }
        }
        return emptyList<String>() to loose
    }
}
