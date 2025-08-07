package com.uaialternativa.imageeditor.domain.usecase

import android.graphics.Bitmap
import com.uaialternativa.imageeditor.domain.repository.ImageProcessingRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

class ResizeImageUseCaseTest {
    
    private lateinit var imageProcessingRepository: ImageProcessingRepository
    private lateinit var resizeImageUseCase: ResizeImageUseCase
    private lateinit var mockBitmap: Bitmap
    private lateinit var mockResizedBitmap: Bitmap
    
    @Before
    fun setup() {
        imageProcessingRepository = mockk()
        resizeImageUseCase = ResizeImageUseCase(imageProcessingRepository)
        mockBitmap = mockk {
            coEvery { width } returns 1920
            coEvery { height } returns 1080
        }
        mockResizedBitmap = mockk()
    }
    
    @Test
    fun `invoke should resize image successfully with valid dimensions`() = runTest {
        // Given
        val newWidth = 1280
        val newHeight = 720
        coEvery { imageProcessingRepository.resizeImage(mockBitmap, newWidth, newHeight) } returns Result.success(mockResizedBitmap)
        
        // When
        val result = resizeImageUseCase(mockBitmap, newWidth, newHeight)
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(mockResizedBitmap, result.getOrNull())
        coVerify(exactly = 1) { imageProcessingRepository.resizeImage(mockBitmap, newWidth, newHeight) }
    }
    
    @Test
    fun `invoke should return failure when width is zero or negative`() = runTest {
        // Given
        val invalidWidth = 0
        val validHeight = 720
        
        // When
        val result = resizeImageUseCase(mockBitmap, invalidWidth, validHeight)
        
        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
        assertEquals("Width and height must be positive", result.exceptionOrNull()?.message)
        coVerify(exactly = 0) { imageProcessingRepository.resizeImage(any(), any(), any()) }
    }
    
    @Test
    fun `invoke should return failure when height is zero or negative`() = runTest {
        // Given
        val validWidth = 1280
        val invalidHeight = -100
        
        // When
        val result = resizeImageUseCase(mockBitmap, validWidth, invalidHeight)
        
        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
        assertEquals("Width and height must be positive", result.exceptionOrNull()?.message)
        coVerify(exactly = 0) { imageProcessingRepository.resizeImage(any(), any(), any()) }
    }
    
    @Test
    fun `invoke should return failure when repository fails`() = runTest {
        // Given
        val newWidth = 1280
        val newHeight = 720
        val exception = RuntimeException("Resize operation failed")
        coEvery { imageProcessingRepository.resizeImage(mockBitmap, newWidth, newHeight) } returns Result.failure(exception)
        
        // When
        val result = resizeImageUseCase(mockBitmap, newWidth, newHeight)
        
        // Then
        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
        coVerify(exactly = 1) { imageProcessingRepository.resizeImage(mockBitmap, newWidth, newHeight) }
    }
    
    @Test
    fun `resizeWithAspectRatio should resize landscape image correctly`() = runTest {
        // Given
        val maxWidth = 1280
        val maxHeight = 720
        val expectedWidth = 1280
        val expectedHeight = 720 // 1280 / (1920/1080) = 720
        coEvery { imageProcessingRepository.resizeImage(mockBitmap, expectedWidth, expectedHeight) } returns Result.success(mockResizedBitmap)
        
        // When
        val result = resizeImageUseCase.resizeWithAspectRatio(mockBitmap, maxWidth, maxHeight)
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(mockResizedBitmap, result.getOrNull())
        coVerify(exactly = 1) { imageProcessingRepository.resizeImage(mockBitmap, expectedWidth, expectedHeight) }
    }
    
    @Test
    fun `resizeWithAspectRatio should resize portrait image correctly`() = runTest {
        // Given
        val portraitBitmap = mockk<Bitmap> {
            coEvery { width } returns 1080
            coEvery { height } returns 1920
        }
        val maxWidth = 720
        val maxHeight = 1280
        val expectedWidth = 720 // 1280 * (1080/1920) = 720
        val expectedHeight = 1280
        coEvery { imageProcessingRepository.resizeImage(portraitBitmap, expectedWidth, expectedHeight) } returns Result.success(mockResizedBitmap)
        
        // When
        val result = resizeImageUseCase.resizeWithAspectRatio(portraitBitmap, maxWidth, maxHeight)
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(mockResizedBitmap, result.getOrNull())
        coVerify(exactly = 1) { imageProcessingRepository.resizeImage(portraitBitmap, expectedWidth, expectedHeight) }
    }
    
    @Test
    fun `resizeWithAspectRatio should not upscale when original is smaller`() = runTest {
        // Given
        val smallBitmap = mockk<Bitmap> {
            coEvery { width } returns 800
            coEvery { height } returns 600
        }
        val maxWidth = 1920
        val maxHeight = 1080
        val expectedWidth = 800 // Original width (smaller than max)
        val expectedHeight = 600 // 800 / (800/600) = 600
        coEvery { imageProcessingRepository.resizeImage(smallBitmap, expectedWidth, expectedHeight) } returns Result.success(mockResizedBitmap)
        
        // When
        val result = resizeImageUseCase.resizeWithAspectRatio(smallBitmap, maxWidth, maxHeight)
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(mockResizedBitmap, result.getOrNull())
        coVerify(exactly = 1) { imageProcessingRepository.resizeImage(smallBitmap, expectedWidth, expectedHeight) }
    }
    
    @Test
    fun `resizeWithAspectRatio should return failure when repository fails`() = runTest {
        // Given
        val maxWidth = 1280
        val maxHeight = 720
        val exception = RuntimeException("Resize with aspect ratio failed")
        coEvery { imageProcessingRepository.resizeImage(any(), any(), any()) } returns Result.failure(exception)
        
        // When
        val result = resizeImageUseCase.resizeWithAspectRatio(mockBitmap, maxWidth, maxHeight)
        
        // Then
        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
    }
}