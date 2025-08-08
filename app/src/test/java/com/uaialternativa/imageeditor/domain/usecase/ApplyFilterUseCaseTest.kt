package com.uaialternativa.imageeditor.domain.usecase

import android.graphics.Bitmap
import com.uaialternativa.imageeditor.domain.model.FilterType
import com.uaialternativa.imageeditor.domain.repository.ImageProcessingRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

class ApplyFilterUseCaseTest {
    
    private lateinit var imageProcessingRepository: ImageProcessingRepository
    private lateinit var applyFilterUseCase: ApplyFilterUseCase
    private lateinit var mockBitmap: Bitmap
    private lateinit var mockFilteredBitmap: Bitmap
    
    @Before
    fun setup() {
        imageProcessingRepository = mockk()
        applyFilterUseCase = ApplyFilterUseCase(imageProcessingRepository)
        mockBitmap = mockk()
        mockFilteredBitmap = mockk()
    }
    
    @Test
    fun `invoke should apply filter successfully with default intensity`() = runTest {
        // Given
        val filterType = FilterType.BRIGHTNESS
        coEvery { imageProcessingRepository.applyFilter(mockBitmap, filterType, 1.0f) } returns Result.success(mockFilteredBitmap)
        
        // When
        val result = applyFilterUseCase(mockBitmap, filterType)
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(mockFilteredBitmap, result.getOrNull())
        coVerify(exactly = 1) { imageProcessingRepository.applyFilter(mockBitmap, filterType, 1.0f) }
    }
    
    @Test
    fun `invoke should apply filter successfully with custom intensity`() = runTest {
        // Given
        val filterType = FilterType.CONTRAST
        val intensity = 0.7f
        coEvery { imageProcessingRepository.applyFilter(mockBitmap, filterType, intensity) } returns Result.success(mockFilteredBitmap)
        
        // When
        val result = applyFilterUseCase(mockBitmap, filterType, intensity)
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(mockFilteredBitmap, result.getOrNull())
        coVerify(exactly = 1) { imageProcessingRepository.applyFilter(mockBitmap, filterType, intensity) }
    }
    
    @Test
    fun `invoke should clamp intensity to valid range when too high`() = runTest {
        // Given
        val filterType = FilterType.SATURATION
        val intensity = 1.5f // Above maximum
        val clampedIntensity = 1.0f
        coEvery { imageProcessingRepository.applyFilter(mockBitmap, filterType, clampedIntensity) } returns Result.success(mockFilteredBitmap)
        
        // When
        val result = applyFilterUseCase(mockBitmap, filterType, intensity)
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(mockFilteredBitmap, result.getOrNull())
        coVerify(exactly = 1) { imageProcessingRepository.applyFilter(mockBitmap, filterType, clampedIntensity) }
    }
    
    @Test
    fun `invoke should clamp intensity to valid range when too low`() = runTest {
        // Given
        val filterType = FilterType.BLUR
        val intensity = -0.5f // Below minimum
        val clampedIntensity = 0.0f
        coEvery { imageProcessingRepository.applyFilter(mockBitmap, filterType, clampedIntensity) } returns Result.success(mockFilteredBitmap)
        
        // When
        val result = applyFilterUseCase(mockBitmap, filterType, intensity)
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(mockFilteredBitmap, result.getOrNull())
        coVerify(exactly = 1) { imageProcessingRepository.applyFilter(mockBitmap, filterType, clampedIntensity) }
    }
    
    @Test
    fun `invoke should return failure when repository fails`() = runTest {
        // Given
        val filterType = FilterType.SEPIA
        val exception = RuntimeException("Filter application failed")
        coEvery { imageProcessingRepository.applyFilter(mockBitmap, filterType, 1.0f) } returns Result.failure(exception)
        
        // When
        val result = applyFilterUseCase(mockBitmap, filterType)
        
        // Then
        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
        coVerify(exactly = 1) { imageProcessingRepository.applyFilter(mockBitmap, filterType, 1.0f) }
    }
    
    @Test
    fun `invoke should handle all filter types`() = runTest {
        // Test all filter types to ensure they're supported
        val filterTypes = FilterType.values()
        
        filterTypes.forEach { filterType ->
            // Given
            coEvery { imageProcessingRepository.applyFilter(mockBitmap, filterType, 1.0f) } returns Result.success(mockFilteredBitmap)
            
            // When
            val result = applyFilterUseCase(mockBitmap, filterType)
            
            // Then
            assertTrue("Filter $filterType should be supported", result.isSuccess)
            coVerify(exactly = 1) { imageProcessingRepository.applyFilter(mockBitmap, filterType, 1.0f) }
        }
    }
}