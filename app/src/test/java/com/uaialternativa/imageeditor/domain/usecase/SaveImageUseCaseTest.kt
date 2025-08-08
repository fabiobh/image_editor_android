package com.uaialternativa.imageeditor.domain.usecase

import android.graphics.Bitmap
import com.uaialternativa.imageeditor.domain.model.FilterType
import com.uaialternativa.imageeditor.domain.model.ImageOperation
import com.uaialternativa.imageeditor.domain.model.SavedImage
import com.uaialternativa.imageeditor.domain.repository.ImageMetadata
import com.uaialternativa.imageeditor.domain.repository.ImageRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

class SaveImageUseCaseTest {
    
    private lateinit var imageRepository: ImageRepository
    private lateinit var saveImageUseCase: SaveImageUseCase
    private lateinit var mockBitmap: Bitmap
    
    @Before
    fun setup() {
        imageRepository = mockk()
        saveImageUseCase = SaveImageUseCase(imageRepository)
        mockBitmap = mockk {
            coEvery { width } returns 1920
            coEvery { height } returns 1080
        }
    }
    
    @Test
    fun `invoke should save image with metadata successfully`() = runTest {
        // Given
        val originalFileName = "original.jpg"
        val appliedOperations = listOf(
            ImageOperation.Resize(1920, 1080),
            ImageOperation.Filter(FilterType.BRIGHTNESS, 0.8f)
        )
        val expectedSavedImage = SavedImage(
            id = "1",
            fileName = "saved_image.jpg",
            filePath = "/path/to/saved_image.jpg",
            originalFileName = originalFileName,
            width = 1920,
            height = 1080,
            fileSize = 1024L,
            createdAt = System.currentTimeMillis(),
            modifiedAt = System.currentTimeMillis(),
            appliedOperations = appliedOperations
        )
        val expectedMetadata = ImageMetadata(
            originalFileName = originalFileName,
            width = 1920,
            height = 1080,
            appliedOperations = appliedOperations
        )
        
        coEvery { imageRepository.saveImage(mockBitmap, expectedMetadata) } returns Result.success(expectedSavedImage)
        
        // When
        val result = saveImageUseCase(mockBitmap, originalFileName, appliedOperations)
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(expectedSavedImage, result.getOrNull())
        coVerify(exactly = 1) { imageRepository.saveImage(mockBitmap, expectedMetadata) }
    }
    
    @Test
    fun `invoke should save image with default parameters when not provided`() = runTest {
        // Given
        val expectedSavedImage = SavedImage(
            id = "1",
            fileName = "saved_image.jpg",
            filePath = "/path/to/saved_image.jpg",
            originalFileName = null,
            width = 1920,
            height = 1080,
            fileSize = 1024L,
            createdAt = System.currentTimeMillis(),
            modifiedAt = System.currentTimeMillis(),
            appliedOperations = emptyList()
        )
        val expectedMetadata = ImageMetadata(
            originalFileName = null,
            width = 1920,
            height = 1080,
            appliedOperations = emptyList()
        )
        
        coEvery { imageRepository.saveImage(mockBitmap, expectedMetadata) } returns Result.success(expectedSavedImage)
        
        // When
        val result = saveImageUseCase(mockBitmap)
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(expectedSavedImage, result.getOrNull())
        coVerify(exactly = 1) { imageRepository.saveImage(mockBitmap, expectedMetadata) }
    }
    
    @Test
    fun `invoke should return failure when repository fails`() = runTest {
        // Given
        val exception = RuntimeException("Save failed")
        val expectedMetadata = ImageMetadata(
            originalFileName = null,
            width = 1920,
            height = 1080,
            appliedOperations = emptyList()
        )
        
        coEvery { imageRepository.saveImage(mockBitmap, expectedMetadata) } returns Result.failure(exception)
        
        // When
        val result = saveImageUseCase(mockBitmap)
        
        // Then
        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
        coVerify(exactly = 1) { imageRepository.saveImage(mockBitmap, expectedMetadata) }
    }
}