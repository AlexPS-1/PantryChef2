// Provides keyword-to-generic pantry item mappings for OCR token resolution.
package com.example.pantrychef.core

object ClassLexicon {

    data class Entry(val name: String, val unit: String)

    private val map: Map<String, Entry> = mapOf(
        "pasta" to Entry("pasta", "box"),
        "spaghetti" to Entry("pasta", "box"),
        "penne" to Entry("pasta", "box"),
        "noodles" to Entry("noodles", "pack"),
        "ramen" to Entry("noodles", "pack"),
        "udon" to Entry("noodles", "pack"),
        "rice" to Entry("rice", "bag"),
        "beans" to Entry("beans", "can"),
        "tomato" to Entry("tomato", "pcs"),
        "tomatoes" to Entry("tomato", "pcs"),
        "tuna" to Entry("tuna", "can"),
        "flour" to Entry("flour", "bag"),
        "sugar" to Entry("sugar", "bag"),
        "salt" to Entry("salt", "pack"),
        "pepper" to Entry("pepper", "pack"),
        "coffee" to Entry("coffee", "bag"),
        "tea" to Entry("tea", "box"),
        "milk" to Entry("milk", "carton"),
        "yogurt" to Entry("yogurt", "cup"),
        "cheese" to Entry("cheese", "pack"),
        "butter" to Entry("butter", "pack"),
        "bread" to Entry("bread", "loaf"),
        "eggs" to Entry("eggs", "pcs"),
        "cucumber" to Entry("cucumber", "pcs"),
        "onion" to Entry("onion", "pcs"),
        "garlic" to Entry("garlic", "pcs"),
        "apple" to Entry("apple", "pcs"),
        "banana" to Entry("banana", "pcs"),
        "oil" to Entry("oil", "bottle"),
        "olive" to Entry("oil", "bottle")
    )

    fun resolve(token: String): Entry? = map[token.lowercase()]
}
