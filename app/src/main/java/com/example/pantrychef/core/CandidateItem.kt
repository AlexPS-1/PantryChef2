// Defines a generic pantry item candidate detected or suggested by various sources.
package com.example.pantrychef.core

data class CandidateItem(
    val name: String,
    val count: Int,
    val unit: String,
    val confidence: Float,
    val source: Source
) {
    enum class Source { ON_DEVICE, OCR, BARCODE, GEMINI }
}
