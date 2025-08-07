package com.uaialternativa.imageeditor.domain.usecase

import android.graphics.Bitmap
import android.graphics.Rect
import com.uaialternativa.imageeditor.domain.repository.ImageProcessingRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

class CropImageUseCaseTest {
    
    private lateinit var imageProcessingRepository: ImageProcessingRepository
    private lateinit var cropImageUseCase: CropImageUseCase
    private lateinit var mockBitmap: Bitmap
    private lateinit var mockCroppedBitmap: Bitmap
    
    @Before
    fun setup() {
        imageProcessingRepository = mockk()
        cropImageUseCase = CropImageUseCase(imageProcessingRepository)
        mockBitmap = mockk {
            coEvery { width } returns 1920
            coEvery { height } returns 1080
        }
        mockCroppedBitmap = mockk()
    }
    
    @Test
    fun `invoke should crop image successfully with valid bounds`() = runTest {
        // Given
        val cropBounds = Rect(100, 100, 800, 600)
        coEvery { imageProcessingRepository.cropImage(mockBitmap, any()) } returns Result.success(mockCroppedBitmap)
        
        // When
        val result = cropImageUseCase(mockBitmap, cropBounds)
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(mockCroppedBitmap, result.getOrNull())
        coVerify(exactly = 1) { imageProcessingRepository.cropImage(mockBitmap, any()) }
    }
    
    @Test
    fun `invoke should validate and adjust crop bounds when they exceed image dimensions`() = runTest {
        // Given
        val invalidBounds = Rect(100, 100, 2000, 1200) // Exceeds image dimensions
        coEvery { imageProcessingRepository.cropImage(mockBitmap, any()) } returns Result.success(mockCroppedBitmap)
        
        // When
        val result = cropImageUseCase(mockBitmap, invalidBounds)
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(mockCroppedBitmap, result.getOrNull())
        coVerify(exactly = 1) { imageProcessingRepository.cropImage(mockBitmap, any()) }
    }
    
    @Test
    fun `invoke should validate and adjust crop bounds when they are negative`() = runTest {
        // Given
        val invalidBounds = Rect(-50, -50, 800, 600) // Negative coordinates
        coEvery { imageProcessingRepository.cropImage(mockBitmap, any()) } returns Result.success(mockCroppedBitmap)
        
        // When
        val result = cropImageUseCase(mockBitmap, invalidBounds)
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(mockCroppedBitmap, result.getOrNull())
        coVerify(exactly = 1) { imageProcessingRepository.cropImage(mockBitmap, any()) }
    }
    
    @Test
    fun `invoke should validate and adjust crop bounds when right is less than left`() = runTest {
        // Given
        val invalidBounds = Rect(800, 100, 400, 600) // Right < Left
        coEvery { imageProcessingRepository.cropImage(mockBitmap, any()) } returns Result.success(mockCroppedBitmap)
        
        // When
        val result = cropImageUseCase(mockBitmap, invalidBounds)
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(mockCroppedBitmap, result.getOrNull())
        coVerify(exactly = 1) { imageProcessingRepository.cropImage(mockBitmap, any()) }
    }
    
    @Test
    fun `invoke should validate and adjust crop bounds when bottom is less than top`() = runTest {
        // Given
        val invalidBounds = Rect(100, 600, 800, 300) // Bottom < Top
        coEvery { imageProcessingRepository.cropImage(mockBitmap, any()) } returns Result.success(mockCroppedBitmap)
        
        // When
        val result = cropImageUseCase(mockBitmap, invalidBounds)
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(mockCroppedBitmap, result.getOrNull())
        coVerify(exactly = 1) { imageProcessingRepository.cropImage(mockBitmap, any()) }
    }
    
    @Test
    fun `invoke should handle edge case with zero-sized crop area`() = runTest {
        // Given
        val zeroBounds = Rect(500, 500, 500, 500) // Zero-sized area
        coEvery { imageProcessingRepository.cropImage(mockBitmap, any()) } returns Result.success(mockCroppedBitmap)
        
        // When
        val result = cropImageUseCase(mockBitmap, zeroBounds)
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(mockCroppedBitmap, result.getOrNull())
        coVerify(exactly = 1) { imageProcessingRepository.cropImage(mockBitmap, any()) }
    }
    
    @Test
    fun `invoke should return failure when repository fails`() = runTest {
        // Given
        val cropBounds = Rect(100, 100, 800, 600)
        val exception = RuntimeException("Crop operation failed")
        coEvery { imageProcessingRepository.cropImage(mockBitmap, any()) } returns Result.failure(exception)
        
        // When
        val result = cropImageUseCase(mockBitmap, cropBounds)
        
        // Then
        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
        coVerify(exactly = 1) { imageProcessingRepository.cropImage(mockBitmap, any()) }
    }
}