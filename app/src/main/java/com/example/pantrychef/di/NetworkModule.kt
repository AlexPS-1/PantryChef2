package com.example.pantrychef.di

import android.content.Context
import com.example.pantrychef.BuildConfig
import com.google.ai.client.generativeai.GenerativeModel
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.Cache
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Qualifier
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Qualifier
    @Retention(AnnotationRetention.BINARY)
    annotation class OffClient

    private class OffHeadersInterceptor : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val ua = "PantryChef/1.0 (Android; app)"
            val req = chain.request()
                .newBuilder()
                .header("User-Agent", ua)
                .header("Accept", "application/json")
                .build()
            return chain.proceed(req)
        }
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        @ApplicationContext context: Context
    ): OkHttpClient {
        val cacheDir = File(context.cacheDir, "http_cache")
        val cache = Cache(cacheDir, 5L * 1024 * 1024)

        return OkHttpClient.Builder()
            .cache(cache)
            .addInterceptor(OffHeadersInterceptor())
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(8, TimeUnit.SECONDS)
            .writeTimeout(8, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    @OffClient
    fun provideOffOkHttpClient(
        @ApplicationContext context: Context
    ): OkHttpClient {
        val cacheDir = File(context.cacheDir, "http_cache_off")
        val cache = Cache(cacheDir, 3L * 1024 * 1024)

        return OkHttpClient.Builder()
            .cache(cache)
            .protocols(listOf(Protocol.HTTP_1_1))
            .retryOnConnectionFailure(true)
            .addInterceptor(OffHeadersInterceptor())
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .callTimeout(22, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideKotlinxJson(): Json =
        Json {
            ignoreUnknownKeys = true
            isLenient = true
            explicitNulls = false
        }

    @Provides
    @Singleton
    fun provideGenerativeModel(): GenerativeModel =
        GenerativeModel(
            modelName = GEMINI_MODEL_NAME,
            apiKey = BuildConfig.GEMINI_API_KEY
        )

    private const val GEMINI_MODEL_NAME = "gemini-3.1-flash-lite-preview"
}