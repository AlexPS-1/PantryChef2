// Data access object for managing pantry items in the local Room database.
package com.example.pantrychef.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PantryDao {

    @Query("SELECT * FROM pantryitem ORDER BY name COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<PantryItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: PantryItem): Long

    @Query("DELETE FROM pantryitem WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE pantryitem SET name = :name, quantity = :quantity, unit = :unit WHERE id = :id")
    suspend fun updateById(id: Long, name: String, quantity: Double, unit: String): Int
}
