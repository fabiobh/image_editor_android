package com.uaialternativa.imageeditor.data.repository

import android.graphics.Bitmap
import android.graphics.Rect
import com.uaialternativa.imageeditor.data.database.SavedImageDao
import com.uaialternativa.imageeditor.data.database.SavedImageEntity
import com.uaialternativa.imageeditor.data.file.FileManager
import com.uaialternativa.imageeditor.domain.model.FilterType
import com.uaialternativa.imageeditor.domain.model.ImageEditorError
import com.uaialternativa.imageeditor.domain.model.ImageOperation
import com.uaialternativa.imageeditor.domain.repository.ImageMetadata
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.File
import java.io.IOException

class ImageRepositoryImplTest {

    private lateinit var repository: ImageRepositoryImpl
    private lateinit var mockDao: SavedImageDao
    private lateinit var mockFileManager: FileManager
    private lateinit var mockBitmap: Bitmap
    private lateinit var mockFile: File

    @Before
    fun setup() {
        mockDao = mockk()
        mockFileManager = mockk()
        mockBitmap = mockk()
        mockFile = mockk()
        
        repository = ImageRepositoryImpl(mockDao, mockFileManager)
        
        // Setup common mock behaviors
        every { mockBitmap.isRecycled } returns false
        every { mockFile.absolutePath } returns "/test/path/image.jpg"
        every { mockFile.name } returns "image.jpg"
        every { mockFile.length() } returns 1024L
    }

    @Test
    fun `getSavedImages returns mapped domain models`() = runTest {
        // Given
        val entities = listOf(
            createTestEntity("1"),
            createTestEntity("2")
        )
        coEvery { mockDao.getAllImages() } returns flowOf(entities)

        // When
        val result = mutableListOf<List<com.uaialternativa.imageeditor.domain.model.SavedImage>>()
        repository.getSavedImages().collect { result.add(it) }

        // Then
        assertEquals(1, result.size)
        assertEquals(2, result[0].size)
        assertEquals("1", result[0][0].id)
        assertEquals("2", result[0][1].id)
    }

    @Test
    fun `getSavedImages handles database exception`() = runTest {
        // Given
        val exception = RuntimeException("Database error")
        coEvery { mockDao.getAllImages() } throws exception

        // When & Then
        try {
            repository.getSavedImages().collect { }
            fail("Expected exception to be thrown")
        } catch (e: Exception) {
            // The exception should be wrapped or propagated
            assertTrue(e is RuntimeException || e is ImageEditorError.DatabaseError)
        }
    }

    @Test
    fun `saveImage successfully saves bitmap and metadata`() = runTest {
        // Given
        val metadata = createTestMetadata()
        val entitySlot = slot<SavedImageEntity>()
        
        coEvery { mockFileManager.saveBitmap(mockBitmap) } returns Result.success(mockFile)
        coEvery { mockDao.insertImage(capture(entitySlot)) } returns Unit

        // When
        val result = repository.saveImage(mockBitmap, metadata)

        // Then
        assertTrue(result.isSuccess)
        val savedImage = result.getOrThrow()
        
        // Verify entity was saved with correct data
        val capturedEntity = entitySlot.captured
        assertEquals(savedImage.id, capturedEntity.id)
        assertEquals("image.jpg", capturedEntity.fileName)
        assertEquals("/test/path/image.jpg", capturedEntity.filePath)
        assertEquals("original.jpg", capturedEntity.originalFileName)
        assertEquals(800, capturedEntity.width)
        assertEquals(600, capturedEntity.height)
        assertEquals(1024L, capturedEntity.fileSize)
        assertEquals(metadata.appliedOperations, capturedEntity.appliedOperations)
        
        coVerify { mockFileManager.saveBitmap(mockBitmap) }
        coVerify { mockDao.insertImage(any()) }
    }

    @Test
    fun `saveImage fails when file save fails`() = runTest {
        // Given
        val metadata = createTestMetadata()
        val fileException = IOException("File save failed")
        
        coEvery { mockFileManager.saveBitmap(mockBitmap) } returns Result.failure(fileException)

        // When
        val result = repository.saveImage(mockBitmap, metadata)

        // Then
        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull() as ImageEditorError.ImageSavingError
        assertEquals("Failed to save image to file system", exception.message)
        assertEquals(fileException, exception.cause)
        
        coVerify(exactly = 0) { mockDao.insertImage(any()) }
    }

    @Test
    fun `saveImage cleans up file when database save fails`() = runTest {
        // Given
        val metadata = createTestMetadata()
        val dbException = RuntimeException("Database error")
        
        coEvery { mockFileManager.saveBitmap(mockBitmap) } returns Result.success(mockFile)
        coEvery { mockDao.insertImage(any()) } throws dbException
        coEvery { mockFileManager.deleteFile("/test/path/image.jpg") } returns Result.success(Unit)

        // When
        val result = repository.saveImage(mockBitmap, metadata)

        // Then
        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull() as ImageEditorError.DatabaseError
        assertEquals("Failed to save image metadata to database", exception.message)
        assertEquals(dbException, exception.cause)
        
        coVerify { mockFileManager.deleteFile("/test/path/image.jpg") }
    }

    @Test
    fun `deleteImage successfully removes from database and file system`() = runTest {
        // Given
        val imageId = "test-id"
        val entity = createTestEntity(imageId)
        
        coEvery { mockDao.getImageById(imageId) } returns entity
        coEvery { mockDao.deleteImageById(imageId) } returns 1
        coEvery { mockFileManager.deleteFile(entity.filePath) } returns Result.success(Unit)

        // When
        val result = repository.deleteImage(imageId)

        // Then
        assertTrue(result.isSuccess)
        coVerify { mockDao.getImageById(imageId) }
        coVerify { mockDao.deleteImageById(imageId) }
        coVerify { mockFileManager.deleteFile(entity.filePath) }
    }

    @Test
    fun `deleteImage fails when image not found`() = runTest {
        // Given
        val imageId = "non-existent-id"
        
        coEvery { mockDao.getImageById(imageId) } returns null

        // When
        val result = repository.deleteImage(imageId)

        // Then
        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull() as ImageEditorError.DatabaseError
        assertEquals("Image with ID $imageId not found", exception.message)
        
        coVerify(exactly = 0) { mockDao.deleteImageById(any()) }
        coVerify(exactly = 0) { mockFileManager.deleteFile(any()) }
    }

    @Test
    fun `deleteImage fails when database deletion fails`() = runTest {
        // Given
        val imageId = "test-id"
        val entity = createTestEntity(imageId)
        
        coEvery { mockDao.getImageById(imageId) } returns entity
        coEvery { mockDao.deleteImageById(imageId) } returns 0 // No rows deleted

        // When
        val result = repository.deleteImage(imageId)

        // Then
        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull() as ImageEditorError.DatabaseError
        assertEquals("Failed to delete image from database", exception.message)
        
        coVerify(exactly = 0) { mockFileManager.deleteFile(any()) }
    }

    @Test
    fun `deleteImage succeeds even when file deletion fails`() = runTest {
        // Given
        val imageId = "test-id"
        val entity = createTestEntity(imageId)
        val fileException = IOException("File deletion failed")
        
        coEvery { mockDao.getImageById(imageId) } returns entity
        coEvery { mockDao.deleteImageById(imageId) } returns 1
        coEvery { mockFileManager.deleteFile(entity.filePath) } returns Result.failure(fileException)

        // When
        val result = repository.deleteImage(imageId)

        // Then
        assertTrue(result.isSuccess) // Should still succeed even if file deletion fails
        coVerify { mockDao.deleteImageById(imageId) }
        coVerify { mockFileManager.deleteFile(entity.filePath) }
    }

    @Test
    fun `getImageFile returns file when valid`() = runTest {
        // Given
        val imagePath = "/test/path/image.jpg"
        
        coEvery { mockFileManager.getFile(imagePath) } returns Result.success(mockFile)
        every { mockFile.absolutePath } returns imagePath

        // When
        val result = repository.getImageFile(imagePath)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(mockFile, result.getOrThrow())
        coVerify { mockFileManager.getFile(imagePath) }
    }

    @Test
    fun `getImageFile fails when file manager fails`() = runTest {
        // Given
        val imagePath = "/test/path/image.jpg"
        val fileException = IOException("File not found")
        
        coEvery { mockFileManager.getFile(imagePath) } returns Result.failure(fileException)

        // When
        val result = repository.getImageFile(imagePath)

        // Then
        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull() as ImageEditorError.FileSystemError
        assertEquals("Failed to get image file: $imagePath", exception.message)
        assertEquals(fileException, exception.cause)
    }

    @Test
    fun `getImageById returns image when found and file exists`() = runTest {
        // Given
        val imageId = "test-id"
        val entity = createTestEntity(imageId)
        
        coEvery { mockDao.getImageById(imageId) } returns entity
        coEvery { mockFileManager.isValidFile(entity.filePath) } returns true

        // When
        val result = repository.getImageById(imageId)

        // Then
        assertTrue(result.isSuccess)
        val savedImage = result.getOrThrow()
        assertEquals(imageId, savedImage.id)
        assertEquals(entity.fileName, savedImage.fileName)
    }

    @Test
    fun `getImageById removes entry when file is missing`() = runTest {
        // Given
        val imageId = "test-id"
        val entity = createTestEntity(imageId)
        
        coEvery { mockDao.getImageById(imageId) } returns entity
        coEvery { mockFileManager.isValidFile(entity.filePath) } returns false
        coEvery { mockDao.deleteImageById(imageId) } returns 1

        // When
        val result = repository.getImageById(imageId)

        // Then
        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull() as ImageEditorError.FileSystemError
        assertEquals("Image file is missing and has been removed from database", exception.message)
        
        coVerify { mockDao.deleteImageById(imageId) }
    }

    @Test
    fun `getImageById fails when image not found`() = runTest {
        // Given
        val imageId = "non-existent-id"
        
        coEvery { mockDao.getImageById(imageId) } returns null

        // When
        val result = repository.getImageById(imageId)

        // Then
        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull() as ImageEditorError.DatabaseError
        assertEquals("Image with ID $imageId not found", exception.message)
    }

    private fun createTestEntity(id: String): SavedImageEntity {
        return SavedImageEntity(
            id = id,
            fileName = "test_image_$id.jpg",
            filePath = "/test/path/test_image_$id.jpg",
            originalFileName = "original_$id.jpg",
            width = 800,
            height = 600,
            fileSize = 1024L,
            createdAt = System.currentTimeMillis(),
            modifiedAt = System.currentTimeMillis(),
            appliedOperations = listOf(
                ImageOperation.Crop(Rect(0, 0, 400, 300)),
                ImageOperation.Filter(FilterType.BRIGHTNESS, 0.5f)
            )
        )
    }

    private fun createTestMetadata(): ImageMetadata {
        return ImageMetadata(
            originalFileName = "original.jpg",
            width = 800,
            height = 600,
            appliedOperations = listOf(
                ImageOperation.Resize(800, 600),
                ImageOperation.Filter(FilterType.CONTRAST, 0.3f)
            )
        )
    }
}