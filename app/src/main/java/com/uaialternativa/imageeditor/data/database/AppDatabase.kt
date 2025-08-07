package com.uaialternativa.imageeditor.data.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context

@Database(
    entities = [SavedImageEntity::class],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun savedImageDao(): SavedImageDao
    
    companion object {
        const val DATABASE_NAME = "image_editor_database"
    }
}