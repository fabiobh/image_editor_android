package com.uaialternativa.imageeditor.data.database

import android.graphics.Rect
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.uaialternativa.imageeditor.domain.model.FilterType
import com.uaialternativa.imageeditor.domain.model.ImageOperation
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

/**
 * Instrumented tests for SavedImageDao
 * These tests require Android context and run on device/emulator
 */
@RunWith(AndroidJUnit4::class)
class SavedImageDaoTest {
    
    private lateinit var database: AppDatabase
    private lateinit var dao: SavedImageDao
    
    @Before
    fun setup() {
        database = AppDatabase.createInMemory(ApplicationProvider.getApplicationContext())
        dao = database.savedImageDao()
    }
    
    @After
    fun teardown() {
        database.close()
    }
    
    @Test
    fun insertAndGetImage() = runTest {
        val image = createTestImage()
        
        dao.insertImage(image)
        val retrieved = dao.getImageById(image.id)
        
        assertNotNull(retrieved)
        assertEquals(image, retrieved)
    }
    
    @Test
    fun getAllImages_returnsImagesOrderedByModifiedDate() = runTest {
        val now = System.currentTimeMillis()
        val image1 = createTestImage(id = "1", modifiedAt = now - 1000)
        val image2 = createTestImage(id = "2", modifiedAt = now)
        val image3 = createTestImage(id = "3", modifiedAt = now - 2000)
        
        dao.insertImages(listOf(image1, image2, image3))
        
        val images = dao.getAllImages().first()
        
        assertEquals(3, images.size)
        assertEquals("2", images[0].id) // Most recent first
        assertEquals("1", images[1].id)
        assertEquals("3", images[2].id) // Oldest last
    }
    
    @Test
    fun deleteImageById() = runTest {
        val image = createTestImage()
        dao.insertImage(image)
        
        val deletedCount = dao.deleteImageById(image.id)
        val retrieved = dao.getImageById(image.id)
        
        assertEquals(1, deletedCount)
        assertNull(retrieved)
    }
    
    @Test
    fun deleteImagesByIds() = runTest {
        val images = listOf(
            createTestImage(id = "1"),
            createTestImage(id = "2"),
            createTestImage(id = "3")
        )
        dao.insertImages(images)
        
        val deletedCount = dao.deleteImagesByIds(listOf("1", "3"))
        val remaining = dao.getAllImages().first()
        
        assertEquals(2, deletedCount)
        assertEquals(1, remaining.size)
        assertEquals("2", remaining[0].id)
    }
    
    @Test
    fun updateImage() = runTest {
        val originalImage = createTestImage()
        dao.insertImage(originalImage)
        
        val updatedImage = originalImage.copy(
            fileName = "updated_file.jpg",
            modifiedAt = System.currentTimeMillis()
        )
        dao.updateImage(updatedImage)
        
        val retrieved = dao.getImageById(originalImage.id)
        
        assertNotNull(retrieved)
        assertEquals("updated_file.jpg", retrieved!!.fileName)
        assertEquals(updatedImage.modifiedAt, retrieved.modifiedAt)
    }
    
    @Test
    fun getImageCount() = runTest {
        assertEquals(0, dao.getImageCount())
        
        dao.insertImages(listOf(
            createTestImage(id = "1"),
            createTestImage(id = "2"),
            createTestImage(id = "3")
        ))
        
        assertEquals(3, dao.getImageCount())
    }
    
    @Test
    fun getTotalFileSize() = runTest {
        val images = listOf(
            createTestImage(id = "1", fileSize = 1000L),
            createTestImage(id = "2", fileSize = 2000L),
            createTestImage(id = "3", fileSize = 3000L)
        )
        dao.insertImages(images)
        
        val totalSize = dao.getTotalFileSize()
        
        assertEquals(6000L, totalSize)
    }
    
    @Test
    fun searchImages() = runTest {
        val images = listOf(
            createTestImage(id = "1", fileName = "sunset_beach.jpg"),
            createTestImage(id = "2", fileName = "mountain_view.jpg", originalFileName = "original_sunset.jpg"),
            createTestImage(id = "3", fileName = "city_night.jpg")
        )
        dao.insertImages(images)
        
        val sunsetResults = dao.searchImages("sunset").first()
        val mountainResults = dao.searchImages("mountain").first()
        val emptyResults = dao.searchImages("nonexistent").first()
        
        assertEquals(2, sunsetResults.size) // Matches both fileName and originalFileName
        assertEquals(1, mountainResults.size)
        assertEquals(0, emptyResults.size)
    }
    
    @Test
    fun getImagesByDateRange() = runTest {
        val baseTime = System.currentTimeMillis()
        val images = listOf(
            createTestImage(id = "1", createdAt = baseTime - 3000),
            createTestImage(id = "2", createdAt = baseTime - 1000),
            createTestImage(id = "3", createdAt = baseTime + 1000)
        )
        dao.insertImages(images)
        
        val rangeResults = dao.getImagesByDateRange(
            baseTime - 2000,
            baseTime
        ).first()
        
        assertEquals(1, rangeResults.size)
        assertEquals("2", rangeResults[0].id)
    }
    
    @Test
    fun deleteImagesOlderThan() = runTest {
        val baseTime = System.currentTimeMillis()
        val images = listOf(
            createTestImage(id = "1", createdAt = baseTime - 3000),
            createTestImage(id = "2", createdAt = baseTime - 1000),
            createTestImage(id = "3", createdAt = baseTime + 1000)
        )
        dao.insertImages(images)
        
        val deletedCount = dao.deleteImagesOlderThan(baseTime - 2000)
        val remaining = dao.getAllImages().first()
        
        assertEquals(1, deletedCount)
        assertEquals(2, remaining.size)
        assertTrue(remaining.none { it.id == "1" })
    }
    
    @Test
    fun deleteAllImages() = runTest {
        dao.insertImages(listOf(
            createTestImage(id = "1"),
            createTestImage(id = "2"),
            createTestImage(id = "3")
        ))
        
        dao.deleteAllImages()
        val remaining = dao.getAllImages().first()
        
        assertEquals(0, remaining.size)
    }
    
    @Test
    fun insertImage_withReplaceStrategy() = runTest {
        val originalImage = createTestImage(fileName = "original.jpg")
        dao.insertImage(originalImage)
        
        val updatedImage = originalImage.copy(fileName = "updated.jpg")
        dao.insertImage(updatedImage) // Should replace due to OnConflictStrategy.REPLACE
        
        val retrieved = dao.getImageById(originalImage.id)
        val allImages = dao.getAllImages().first()
        
        assertNotNull(retrieved)
        assertEquals("updated.jpg", retrieved!!.fileName)
        assertEquals(1, allImages.size) // Should still be only one image
    }
    
    @Test
    fun persistImageOperations() = runTest {
        val operations = listOf(
            ImageOperation.Crop(Rect(10, 20, 100, 200)),
            ImageOperation.Resize(800, 600),
            ImageOperation.Filter(FilterType.BRIGHTNESS, 0.8f),
            ImageOperation.Filter(FilterType.SEPIA, 0.5f)
        )
        
        val image = createTestImage(appliedOperations = operations)
        dao.insertImage(image)
        
        val retrieved = dao.getImageById(image.id)
        
        assertNotNull(retrieved)
        assertEquals(4, retrieved!!.appliedOperations.size)
        
        // Verify crop operation
        val crop = retrieved.appliedOperations[0] as ImageOperation.Crop
        assertEquals(Rect(10, 20, 100, 200), crop.bounds)
        
        // Verify resize operation
        val resize = retrieved.appliedOperations[1] as ImageOperation.Resize
        assertEquals(800, resize.width)
        assertEquals(600, resize.height)
        
        // Verify filter operations
        val brightnessFilter = retrieved.appliedOperations[2] as ImageOperation.Filter
        assertEquals(FilterType.BRIGHTNESS, brightnessFilter.type)
        assertEquals(0.8f, brightnessFilter.intensity, 0.001f)
        
        val sepiaFilter = retrieved.appliedOperations[3] as ImageOperation.Filter
        assertEquals(FilterType.SEPIA, sepiaFilter.type)
        assertEquals(0.5f, sepiaFilter.intensity, 0.001f)
    }
    
    private fun createTestImage(
        id: String = UUID.randomUUID().toString(),
        fileName: String = "test_image.jpg",
        filePath: String = "/path/to/test_image.jpg",
        originalFileName: String? = null,
        width: Int = 1920,
        height: Int = 1080,
        fileSize: Long = 1024L,
        createdAt: Long = System.currentTimeMillis(),
        modifiedAt: Long = System.currentTimeMillis(),
        appliedOperations: List<ImageOperation> = emptyList()
    ): SavedImageEntity {
        return SavedImageEntity(
            id = id,
            fileName = fileName,
            filePath = filePath,
            originalFileName = originalFileName,
            width = width,
            height = height,
            fileSize = fileSize,
            createdAt = createdAt,
            modifiedAt = modifiedAt,
            appliedOperations = appliedOperations
        )
    }
}