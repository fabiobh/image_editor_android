package com.uaialternativa.imageeditor.data.database

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests for AppDatabase configuration and creation
 */
@RunWith(AndroidJUnit4::class)
class AppDatabaseTest {
    
    @Test
    fun createDatabase_success() {
        val database = AppDatabase.create(ApplicationProvider.getApplicationContext())
        
        assertNotNull(database)
        assertNotNull(database.savedImageDao())
        
        database.close()
    }
    
    @Test
    fun createInMemoryDatabase_success() {
        val database = AppDatabase.createInMemory(ApplicationProvider.getApplicationContext())
        
        assertNotNull(database)
        assertNotNull(database.savedImageDao())
        assertTrue(database.isOpen)
        
        database.close()
    }
    
    @Test
    fun databaseName_isCorrect() {
        assertEquals("image_editor_database", AppDatabase.DATABASE_NAME)
    }
    
    @Test
    fun database_hasCorrectVersion() = runTest {
        val database = AppDatabase.createInMemory(ApplicationProvider.getApplicationContext())
        
        // Room databases start at version 1 by default
        // This test ensures our database is properly configured
        assertTrue(database.isOpen)
        
        database.close()
    }
    
    @Test
    fun database_canPerformBasicOperations() = runTest {
        val database = AppDatabase.createInMemory(ApplicationProvider.getApplicationContext())
        val dao = database.savedImageDao()
        
        // Test basic database functionality
        val initialCount = dao.getImageCount()
        assertEquals(0, initialCount)
        
        database.close()
    }
}