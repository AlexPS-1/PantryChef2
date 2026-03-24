// Defines the Room database for PantryChef, including pantry and product entities.
package com.example.pantrychef.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        PantryItem::class,
        Product::class
    ],
    version = 2,
    exportSchema = true
)
abstract class AppDb : RoomDatabase() {
    abstract fun pantryDao(): PantryDao
    abstract fun productDao(): ProductDao
}
