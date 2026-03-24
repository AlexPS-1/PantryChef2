// Represents a product entity in the local Room database, mapped from OpenFoodFacts data.
package com.example.pantrychef.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "products",
    indices = [Index(value = ["barcode"], unique = true)]
)
data class Product(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val barcode: String?,
    val name: String,
    val brand: String?,
    val quantitySize: String?,
    val imageUrl: String?,
    val lastFetched: Long = System.currentTimeMillis()
)
