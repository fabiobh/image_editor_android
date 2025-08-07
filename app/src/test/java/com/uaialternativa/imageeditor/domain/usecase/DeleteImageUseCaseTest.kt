package com.uaialternativa.imageeditor.domain.usecase

import com.uaialternativa.imageeditor.domain.repository.ImageRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

class DeleteImageUseCaseTest {
    
    private lateinit var imageRepository: ImageRepository
    private lateinit var deleteImageUseCase: DeleteImageUseCase
    
    @Before
    fun setup() {
        imageRepository = mockk()
        deleteImageUseCase = DeleteImageUseCase(imageRepository)
    }
    
    @Test
    fun `invoke should delete image successfully`() = runTest {
        // Given
        val imageId = "test_image_id"
        coEvery { imageRepository.deleteImage(imageId) } returns Result.success(Unit)
        
        // When
        val result = deleteImageUseCase(imageId)
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(Unit, result.getOrNull())
        coVerify(exactly = 1) { imageRepository.deleteImage(imageId) }
    }
    
    @Test
    fun `invoke should return failure when repository fails`() = runTest {
        // Given
        val imageId = "test_image_id"
        val exception = RuntimeException("Delete failed")
        coEvery { imageRepository.deleteImage(imageId) } returns Result.failure(exception)
        
        // When
        val result = deleteImageUseCase(imageId)
        
        // Then
        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
        coVerify(exactly = 1) { imageRepository.deleteImage(imageId) }
    }
    
    @Test
    fun `invoke should handle empty image id`() = runTest {
        // Given
        val imageId = ""
        coEvery { imageRepository.deleteImage(imageId) } returns Result.success(Unit)
        
        // When
        val result = deleteImageUseCase(imageId)
        
        // Then
        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { imageRepository.deleteImage(imageId) }
    }
}