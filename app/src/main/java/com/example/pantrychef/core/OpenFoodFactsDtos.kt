// Defines data classes for OpenFoodFacts API responses and their domain mapping.
package com.example.pantrychef.core

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OffSearchResponse(
    @SerialName("products") val products: List<OffProductDto> = emptyList()
)

@Serializable
data class OffProductDto(
    @SerialName("code") val code: String? = null,
    @SerialName("product_name") val productName: String? = null,
    @SerialName("brands") val brands: String? = null,
    @SerialName("languages_tags") val languagesTags: List<String>? = null,
    @SerialName("categories_tags") val categoriesTags: List<String>? = null
) {
    fun toDomain(): OffSuggestion? {
        val name = productName?.trim().orEmpty()
        if (name.isEmpty()) return null
        val brand = brands?.split(",")?.firstOrNull()?.trim().orEmpty()
        val display = if (brand.isNotEmpty()) "$name · $brand" else name
        return OffSuggestion(
            code = code.orEmpty(),
            label = display,
            name = name,
            brand = brand,
            quantity = 1.0,
            unit = "pcs"
        )
    }
}

data class OffSuggestion(
    val code: String,
    val label: String,
    val name: String,
    val brand: String,
    val quantity: Double,
    val unit: String
)
