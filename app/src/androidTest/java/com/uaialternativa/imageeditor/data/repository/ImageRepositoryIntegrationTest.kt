package com.uaialternativa.imageeditor.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.uaialternativa.imageeditor.data.database.AppDatabase
import com.uaialternativa.imageeditor.data.database.SavedImageDao
import com.uaialternativa.imageeditor.data.file.FileManager
import com.uaialternativa.imageeditor.domain.model.FilterType
import com.uaialternativa.imageeditor.domain.model.ImageOperation
import com.uaialternativa.imageeditor.domain.repository.ImageMetadata
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class ImageRepositoryIntegrationTest {

    private lateinit var database: AppDatabase
    private lateinit var dao: SavedImageDao
    private lateinit var fileManager: FileManager
    private lateinit var repository: ImageRepositoryImpl
    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        
        // Create in-memory database for testing
        database = Room.inMemoryDatabaseBuilder(
            context,
            AppDatabase::class.java
        ).build()
        
        dao = database.savedImageDao()
        fileManager = FileManager(context)
        repository = ImageRepositoryImpl(dao, fileManager)
    }

    @After
    fun tearDown() {
        database.close()
        
        // Clean up any test files
        runTest {
            fileManager.getAllEditedImageFiles().getOrNull()?.forEach { file ->
                file.delete()
            }
        }
    }

    @Test
    fun saveAndRetrieveImage_success() = runTest {
        // Given
        val testBitmap = createTestBitmap()
        val metadata = ImageMetadata(
            originalFileName = "test_original.jpg",
            width = 100,
            height = 100,
            appliedOperations = listOf(
                ImageOperation.Filter(FilterType.BRIGHTNESS, 0.5f),
                ImageOperation.Resize(100, 100)
            )
        )

        // When - Save image
        val saveResult = repository.saveImage(testBitmap, metadata)

        // Then - Verify save was successful
        assertTrue("Save should succeed", saveResult.isSuccess)
        val savedImage = saveResult.getOrThrow()
        
        assertNotNull("Image ID should not be null", savedImage.id)
        assertEquals("Original filename should match", "test_original.jpg", savedImage.originalFileName)
        assertEquals("Width should match", 100, savedImage.width)
        assertEquals("Height should match", 100, savedImage.height)
        assertEquals("Applied operations should match", metadata.appliedOperations, savedImage.appliedOperations)
        assertTrue("File size should be greater than 0", savedImage.fileSize > 0)
        
        // Verify file exists
        val file = File(savedImage.filePath)
        assertTrue("File should exist on disk", file.exists())
        assertTrue("File should have content", file.length() > 0)

        // When - Retrieve images
        val retrievedImages = repository.getSavedImages().first()

        // Then - Verify retrieval
        assertEquals("Should have 1 saved image", 1, retrievedImages.size)
        val retrievedImage = retrievedImages[0]
        assertEquals("Retrieved image should match saved image", savedImage.id, retrievedImage.id)
        assertEquals("Filename should match", savedImage.fileName, retrievedImage.fileName)
        assertEquals("File path should match", savedImage.filePath, retrievedImage.filePath)
    }

    @Test
    fun saveAndDeleteImage_success() = runTest {
        // Given
        val testBitmap = createTestBitmap()
        val metadata = ImageMetadata(
            originalFileName = "test_delete.jpg",
            width = 50,
            height = 50,
            appliedOperations = emptyList()
        )

        // When - Save image
        val saveResult = repository.saveImage(testBitmap, metadata)
        assertTrue("Save should succeed", saveResult.isSuccess)
        val savedImage = saveResult.getOrThrow()
        
        // Verify file exists before deletion
        val file = File(savedImage.filePath)
        assertTrue("File should exist before deletion", file.exists())

        // When - Delete image
        val deleteResult = repository.deleteImage(savedImage.id)

        // Then - Verify deletion
        assertTrue("Delete should succeed", deleteResult.isSuccess)
        
        // Verify file is deleted
        assertFalse("File should be deleted from disk", file.exists())
        
        // Verify database entry is removed
        val remainingImages = repository.getSavedImages().first()
        assertEquals("Should have no saved images after deletion", 0, remainingImages.size)
    }

    @Test
    fun getImageFile_success() = runTest {
        // Given
        val testBitmap = createTestBitmap()
        val metadata = ImageMetadata(
            originalFileName = "test_get_file.jpg",
            width = 75,
            height = 75,
            appliedOperations = listOf(ImageOperation.Filter(FilterType.GRAYSCALE, 1.0f))
        )

        // When - Save image
        val saveResult = repository.saveImage(testBitmap, metadata)
        assertTrue("Save should succeed", saveResult.isSuccess)
        val savedImage = saveResult.getOrThrow()

        // When - Get image file
        val fileResult = repository.getImageFile(savedImage.filePath)

        // Then - Verify file retrieval
        assertTrue("Get file should succeed", fileResult.isSuccess)
        val retrievedFile = fileResult.getOrThrow()
        
        assertEquals("File path should match", savedImage.filePath, retrievedFile.absolutePath)
        assertTrue("File should exist", retrievedFile.exists())
        assertTrue("File should be readable", retrievedFile.canRead())
        assertEquals("File size should match", savedImage.fileSize, retrievedFile.length())
    }

    @Test
    fun getImageById_success() = runTest {
        // Given
        val testBitmap = createTestBitmap()
        val metadata = ImageMetadata(
            originalFileName = "test_get_by_id.jpg",
            width = 120,
            height = 80,
            appliedOperations = listOf(
                ImageOperation.Resize(120, 80),
                ImageOperation.Filter(FilterType.SEPIA, 0.7f)
            )
        )

        // When - Save image
        val saveResult = repository.saveImage(testBitmap, metadata)
        assertTrue("Save should succeed", saveResult.isSuccess)
        val savedImage = saveResult.getOrThrow()

        // When - Get image by ID
        val getResult = repository.getImageById(savedImage.id)

        // Then - Verify retrieval
        assertTrue("Get by ID should succeed", getResult.isSuccess)
        val retrievedImage = getResult.getOrThrow()
        
        assertEquals("ID should match", savedImage.id, retrievedImage.id)
        assertEquals("Filename should match", savedImage.fileName, retrievedImage.fileName)
        assertEquals("File path should match", savedImage.filePath, retrievedImage.filePath)
        assertEquals("Original filename should match", savedImage.originalFileName, retrievedImage.originalFileName)
        assertEquals("Width should match", savedImage.width, retrievedImage.width)
        assertEquals("Height should match", savedImage.height, retrievedImage.height)
        assertEquals("Applied operations should match", savedImage.appliedOperations, retrievedImage.appliedOperations)
    }

    @Test
    fun getImageById_fileDeleted_removesFromDatabase() = runTest {
        // Given
        val testBitmap = createTestBitmap()
        val metadata = ImageMetadata(
            originalFileName = "test_orphaned.jpg",
            width = 60,
            height = 60,
            appliedOperations = emptyList()
        )

        // When - Save image
        val saveResult = repository.saveImage(testBitmap, metadata)
        assertTrue("Save should succeed", saveResult.isSuccess)
        val savedImage = saveResult.getOrThrow()

        // Manually delete the file to simulate orphaned database entry
        val file = File(savedImage.filePath)
        assertTrue("File should exist initially", file.exists())
        assertTrue("File deletion should succeed", file.delete())
        assertFalse("File should be deleted", file.exists())

        // When - Try to get image by ID
        val getResult = repository.getImageById(savedImage.id)

        // Then - Should fail and clean up database entry
        assertTrue("Get should fail when file is missing", getResult.isFailure)
        
        // Verify database entry was removed
        val remainingImages = repository.getSavedImages().first()
        assertEquals("Database entry should be removed", 0, remainingImages.size)
    }

    @Test
    fun cleanupOrphanedData_success() = runTest {
        // Given - Create image and then manually delete file to create orphaned entry
        val testBitmap = createTestBitmap()
        val metadata = ImageMetadata(
            originalFileName = "test_cleanup.jpg",
            width = 90,
            height = 90,
            appliedOperations = emptyList()
        )

        val saveResult = repository.saveImage(testBitmap, metadata)
        assertTrue("Save should succeed", saveResult.isSuccess)
        val savedImage = saveResult.getOrThrow()

        // Manually delete the file
        val file = File(savedImage.filePath)
        assertTrue("File deletion should succeed", file.delete())

        // When - Run cleanup
        val cleanupResult = repository.cleanupOrphanedData()

        // Then - Verify cleanup
        assertTrue("Cleanup should succeed", cleanupResult.isSuccess)
        val result = cleanupResult.getOrThrow()
        assertEquals("Should remove 1 orphaned entry", 1, result.orphanedEntriesRemoved)
        
        // Verify database is clean
        val remainingImages = repository.getSavedImages().first()
        assertEquals("No images should remain after cleanup", 0, remainingImages.size)
    }

    @Test
    fun multipleImages_saveAndRetrieve() = runTest {
        // Given
        val testBitmap1 = createTestBitmap()
        val testBitmap2 = createTestBitmap()
        
        val metadata1 = ImageMetadata(
            originalFileName = "test1.jpg",
            width = 100,
            height = 100,
            appliedOperations = listOf(ImageOperation.Filter(FilterType.BRIGHTNESS, 0.3f))
        )
        
        val metadata2 = ImageMetadata(
            originalFileName = "test2.jpg",
            width = 200,
            height = 150,
            appliedOperations = listOf(
                ImageOperation.Resize(200, 150),
                ImageOperation.Filter(FilterType.CONTRAST, 0.8f)
            )
        )

        // When - Save multiple images
        val saveResult1 = repository.saveImage(testBitmap1, metadata1)
        val saveResult2 = repository.saveImage(testBitmap2, metadata2)

        // Then - Verify both saves succeeded
        assertTrue("First save should succeed", saveResult1.isSuccess)
        assertTrue("Second save should succeed", saveResult2.isSuccess)
        
        val savedImage1 = saveResult1.getOrThrow()
        val savedImage2 = saveResult2.getOrThrow()
        
        assertNotEquals("Images should have different IDs", savedImage1.id, savedImage2.id)

        // When - Retrieve all images
        val allImages = repository.getSavedImages().first()

        // Then - Verify both images are retrieved
        assertEquals("Should have 2 saved images", 2, allImages.size)
        
        val imageIds = allImages.map { it.id }.toSet()
        assertTrue("Should contain first image", imageIds.contains(savedImage1.id))
        assertTrue("Should contain second image", imageIds.contains(savedImage2.id))
    }

    private fun createTestBitmap(): Bitmap {
        return Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color.RED)
        }
    }
}