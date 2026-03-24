// Infers a sensible default unit for an ingredient name using overrides and regex precedence.
package com.example.pantrychef.core

object UnitInference {

    private val overrides = mapOf(
        "flour" to "g",
        "mehl" to "g",
        "potatoes" to "kg",
        "kartoffeln" to "kg"
    )

    private val reL = Regex("""\b(milk|water|juice|broth|stock|wine|beer)\b""")
    private val reKG = Regex("""\b(potato(es)?|onion(s)?|carrot(s)?|cabbage(s)?|watermelon(s)?|pumpkin(s)?|beet(root)?(s)?|turnip(s)?|leek(s)?)\b""")
    private val reG = Regex(
        """\b(
            flour(s)?|mehl|sugar(s)?|rice|pasta|lentil(s)?|bean(s)?|oat(s)?|quinoa|couscous|cereal(s)?|
            chocolate|cocoa|cornstarch|yeast|baking(\s+)?powder|baking(\s+)?soda|
            cheese|butter|beef|pork|chicken|ham|sausage(s)?|salami|bacon|
            salt|pepper|cinnamon|paprika|oregano|basil|cumin|curry|turmeric|thyme|rosemary|
            garlic(\s+)?powder|onion(\s+)?powder|chili|chilli
        )\b""".trimIndent(), RegexOption.IGNORE_CASE
    )
    private val reML = Regex("""\b(oil|olive(\s+)?oil|vinegar|sauce|ketchup|mayonnaise|mustard|soy(\s+)?sauce|dressing|syrup)\b""", RegexOption.IGNORE_CASE)
    private val rePCS = Regex("""\b(egg(s)?|apple(s)?|banana(s)?|tomato(es)?|garlic|lemon(s)?|lime(s)?|pepper(s)?|avocado(s)?|bread|bun(s)?|roll(s)?)\b""", RegexOption.IGNORE_CASE)

    fun inferUnit(name: String): String {
        val n = name.trim().lowercase()
        if (n.isEmpty()) return "pcs"

        overrides.entries.firstOrNull { n.contains(it.key) }?.let { return it.value }

        if (reL.containsMatchIn(n)) return "l"
        if (reKG.containsMatchIn(n)) return "kg"
        if (reG.containsMatchIn(n)) return "g"
        if (reML.containsMatchIn(n)) return "ml"
        if (rePCS.containsMatchIn(n)) return "pcs"

        return "pcs"
    }
}
