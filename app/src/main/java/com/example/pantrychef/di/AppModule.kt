// Provides application-level dependencies for PantryChef, including database, network, and AI services.
package com.example.pantrychef.di

import android.content.Context
import androidx.room.Room
import com.example.pantrychef.core.GeminiHttp
import com.example.pantrychef.core.OpenFoodFactsRepository
import com.example.pantrychef.core.RecipesRepository
import com.example.pantrychef.data.local.AppDb
import com.example.pantrychef.data.local.PantryDao
import com.example.pantrychef.ui.scan.OnDeviceDetector
import com.example.pantrychef.ui.scan.MlKitDetector
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideGemini(): GeminiHttp = GeminiHttp()

    @Provides
    @Singleton
    fun provideOFF(
        http: OkHttpClient,
        json: Json,
        @IoDispatcher io: CoroutineDispatcher
    ): OpenFoodFactsRepository = OpenFoodFactsRepository(http, json, io)

    @Provides
    @Singleton
    fun provideRecipesRepository(
        @ApplicationContext ctx: Context,
        json: Json
    ): RecipesRepository = RecipesRepository(ctx, json)

    @Provides
    @Singleton
    fun provideAppDb(@ApplicationContext ctx: Context): AppDb =
        Room.databaseBuilder(ctx, AppDb::class.java, "pantrychef.db").build()

    @Provides
    fun providePantryDao(db: AppDb): PantryDao = db.pantryDao()

    @Provides
    @IoDispatcher
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

    @Provides
    @Singleton
    fun provideOnDeviceDetector(@ApplicationContext ctx: Context): OnDeviceDetector = MlKitDetector(ctx)
}
