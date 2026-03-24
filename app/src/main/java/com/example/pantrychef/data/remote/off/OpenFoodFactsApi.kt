// Provides network access to the OpenFoodFacts API for product search and barcode lookups.
package com.example.pantrychef.data.remote.off

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class OpenFoodFactsApi(
    private val client: OkHttpClient,
    private val json: Json
) {
    private val base = "https://world.openfoodfacts.org"

    suspend fun search(query: String, pageSize: Int = 8): SearchResponse {
        val q = URLEncoder.encode(query, StandardCharsets.UTF_8.toString())
        val url = "$base/cgi/search.pl" +
                "?search_simple=1&action=process&json=1" +
                "&fields=code,product_name,brands,quantity,image_small_url" +
                "&page_size=$pageSize&search_terms=$q"

        val req = Request.Builder().url(url).get().build()
        client.newCall(req).execute().use { resp ->
            val body = resp.body?.string().orEmpty()
            return json.decodeFromString(SearchResponse.serializer(), body)
        }
    }

    suspend fun getProduct(barcode: String): ProductResponse {
        val url = "$base/api/v0/product/$barcode.json"
        val req = Request.Builder().url(url).get().build()
        client.newCall(req).execute().use { resp ->
            val body = resp.body?.string().orEmpty()
            return json.decodeFromString(ProductResponse.serializer(), body)
        }
    }
}

@Serializable
data class SearchResponse(
    @SerialName("products") val products: List<OffProduct> = emptyList()
)

@Serializable
data class ProductResponse(
    @SerialName("status") val status: Int = 0,
    @SerialName("product") val product: OffProduct? = null
)

@Serializable
data class OffProduct(
    @SerialName("code") val code: String? = null,
    @SerialName("product_name") val productName: String? = null,
    @SerialName("brands") val brands: String? = null,
    @SerialName("quantity") val quantity: String? = null,
    @SerialName("image_small_url") val imageSmallUrl: String? = null
)
