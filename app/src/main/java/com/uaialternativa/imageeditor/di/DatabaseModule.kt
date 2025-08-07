package com.uaialternativa.imageeditor.di

import android.content.Context
import androidx.room.Room
import com.uaialternativa.imageeditor.data.database.AppDatabase
import com.uaialternativa.imageeditor.data.database.SavedImageDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        ).build()
    }
    
    @Provides
    fun provideSavedImageDao(database: AppDatabase): SavedImageDao {
        return database.savedImageDao()
    }
}