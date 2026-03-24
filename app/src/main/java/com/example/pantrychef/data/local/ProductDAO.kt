// Data access object for managing products in the local Room database.
package com.example.pantrychef.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ProductDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(products: List<Product>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(product: Product): Long

    @Query("SELECT * FROM products WHERE barcode = :barcode LIMIT 1")
    suspend fun getByBarcode(barcode: String): Product?

    @Query("SELECT * FROM products WHERE name LIKE :like ORDER BY name LIMIT :limit")
    suspend fun searchByNameLike(like: String, limit: Int = 8): List<Product>

    @Query("DELETE FROM products WHERE lastFetched < :olderThanMillis")
    suspend fun purgeOld(olderThanMillis: Long)
}
