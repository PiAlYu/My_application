package com.example.storechecklist.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        ChecklistEntity::class,
        ChecklistItemEntity::class,
        ItemProgressEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun checklistDao(): ChecklistDao
}

