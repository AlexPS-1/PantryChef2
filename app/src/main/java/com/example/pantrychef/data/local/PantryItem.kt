// Represents a pantry item entity stored in the local Room database.
package com.example.pantrychef.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class PantryItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val quantity: Double,
    val unit: String,
    val expiresOn: String? = null
)
