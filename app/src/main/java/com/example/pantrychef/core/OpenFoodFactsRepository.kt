// Repository for querying OpenFoodFacts and mapping results to domain suggestions with simple in-memory caching.
package com.example.pantrychef.core

import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import com.example.pantrychef.di.NetworkModule.OffClient

@Singleton
class OpenFoodFactsRepository @Inject constructor(
    @OffClient
    private val baseClient: OkHttpClient,
    private val json: Json,
    @com.example.pantrychef.di.IoDispatcher private val io: CoroutineDispatcher
) {
    private val userAgent =
        "PantryChef/1.0 (Android; com.example.pantrychef) contact:dev@pantrychef.invalid"

    private data class CacheEntry(val timestamp: Long, val data: List<OffSuggestion>)
    private val cache = mutableMapOf<String, CacheEntry>()
    private val cacheTtlMs = TimeUnit.MINUTES.toMillis(10)

    suspend fun searchFoods(query: String): List<OffSuggestion> = searchInternal(query)

    private suspend fun searchInternal(query: String): List<OffSuggestion> {
        val q = query.trim().lowercase()
        if (q.length < 2) return emptyList()

        cache[q]?.let {
            if (System.currentTimeMillis() - it.timestamp < cacheTtlMs) {
                Log.d("OFF", "Cache hit for '$q' (${it.data.size} results)")
                return it.data
            } else cache.remove(q)
        }

        val url = HttpUrl.Builder()
            .scheme("https")
            .host("world.openfoodfacts.net")
            .addPathSegments("cgi/search.pl")
            .addQueryParameter("search_terms", q)
            .addQueryParameter("search_simple", "1")
            .addQueryParameter("action", "process")
            .addQueryParameter("json", "1")
            .addQueryParameter("page_size", "15")
            .addQueryParameter(
                "fields",
                "code,product_name,brands,languages_tags,categories_tags,quantity"
            )
            .build()

        val req = Request.Builder()
            .url(url)
            .get()
            .header("User-Agent", userAgent)
            .header("Accept", "application/json")
            .header("Accept-Language", "en,de;q=0.9")
            .build()

        return withContext(io) {
            runCatching {
                val http = baseClient.newBuilder()
                    .connectTimeout(5, TimeUnit.SECONDS)
                    .readTimeout(6, TimeUnit.SECONDS)
                    .callTimeout(6, TimeUnit.SECONDS)
                    .build()

                http.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) {
                        Log.w("OFF", "HTTP ${resp.code} for $url")
                        return@use emptyList()
                    }

                    val raw = resp.body?.string().orEmpty()
                    if (raw.isBlank()) return@use emptyList()

                    val dto = json.decodeFromString(OffSearchResponse.serializer(), raw)
                    val products = dto.products

                    val englishish = products.filter { p ->
                        val langs = p.languagesTags?.any { it.startsWith("en", ignoreCase = true) } == true
                        val cats = p.categoriesTags?.any { it.startsWith("en:", ignoreCase = true) } == true
                        langs || cats
                    }

                    val chosen = if (englishish.isNotEmpty()) englishish else products
                    val result = chosen.mapNotNull { it.toDomain() }
                        .distinctBy { it.name.lowercase() }
                        .take(15)

                    cache[q] = CacheEntry(System.currentTimeMillis(), result)
                    result
                }
            }.getOrElse { e ->
                Log.e("OFF", "OFF search failed for '$q': ${e.message}", e)
                emptyList()
            }
        }
    }
}
