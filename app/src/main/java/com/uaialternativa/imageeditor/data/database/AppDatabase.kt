package com.uaialternativa.imageeditor.data.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import android.content.Context

/**
 * Room database for the Image Editor application
 */
@Database(
    entities = [SavedImageEntity::class],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    
    abstract fun savedImageDao(): SavedImageDao
    
    companion object {
        const val DATABASE_NAME = "image_editor_database"
        
        /**
         * Migration from version 1 to 2 (example for future use)
         * This would be used when we need to update the database schema
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Example migration - add new column
                // database.execSQL("ALTER TABLE saved_images ADD COLUMN new_column TEXT")
            }
        }
        
        /**
         * Creates the Room database instance with proper configuration
         */
        fun create(context: Context): AppDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                DATABASE_NAME
            )
            .addMigrations(
                // Add migrations here as needed
                // MIGRATION_1_2
            )
            .fallbackToDestructiveMigration() // For development - remove in production
            .build()
        }
        
        /**
         * Creates an in-memory database for testing
         */
        fun createInMemory(context: Context): AppDatabase {
            return Room.inMemoryDatabaseBuilder(
                context.applicationContext,
                AppDatabase::class.java
            )
            .allowMainThreadQueries() // Only for testing
            .build()
        }
    }
}