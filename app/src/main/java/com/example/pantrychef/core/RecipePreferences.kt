// Defines user-selected recipe preferences that shape Gemini recipe generation.
package com.example.pantrychef.core

data class RecipePreferences(
    val mealTime: List<String> = emptyList(),
    val styleSpeed: List<String> = emptyList(),
    val diet: List<String> = emptyList(),
    val cuisine: List<String> = emptyList(),
    val mood: List<String> = emptyList(),
    val mustInclude: List<String> = emptyList(),
    val maxNewAdditions: Int = 2,
    val voice: String? = null,
    val spicyLanguage: Boolean = false,
    val childFriendly: Boolean = false
)
