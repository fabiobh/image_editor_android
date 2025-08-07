package com.uaialternativa.imageeditor.domain.usecase

import com.uaialternativa.imageeditor.domain.model.ImageOperation
import com.uaialternativa.imageeditor.domain.model.SavedImage
import com.uaialternativa.imageeditor.domain.repository.ImageRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

class GetSavedImagesUseCaseTest {
    
    private lateinit var imageRepository: ImageRepository
    private lateinit var getSavedImagesUseCase: GetSavedImagesUseCase
    
    @Before
    fun setup() {
        imageRepository = mockk()
        getSavedImagesUseCase = GetSavedImagesUseCase(imageRepository)
    }
    
    @Test
    fun `invoke should return flow of saved images from repository`() = runTest {
        // Given
        val savedImages = listOf(
            SavedImage(
                id = "1",
                fileName = "image1.jpg",
                filePath = "/path/to/image1.jpg",
                originalFileName = "original1.jpg",
                width = 1920,
                height = 1080,
                fileSize = 1024L,
                createdAt = System.currentTimeMillis(),
                modifiedAt = System.currentTimeMillis(),
                appliedOperations = emptyList()
            ),
            SavedImage(
                id = "2",
                fileName = "image2.jpg",
                filePath = "/path/to/image2.jpg",
                originalFileName = "original2.jpg",
                width = 1280,
                height = 720,
                fileSize = 512L,
                createdAt = System.currentTimeMillis(),
                modifiedAt = System.currentTimeMillis(),
                appliedOperations = listOf(ImageOperation.Resize(1280, 720))
            )
        )
        val expectedFlow = flowOf(savedImages)
        coEvery { imageRepository.getSavedImages() } returns expectedFlow
        
        // When
        val result = getSavedImagesUseCase()
        
        // Then
        assertEquals(expectedFlow, result)
        coVerify(exactly = 1) { imageRepository.getSavedImages() }
    }
    
    @Test
    fun `invoke should return empty flow when no images exist`() = runTest {
        // Given
        val emptyFlow = flowOf(emptyList<SavedImage>())
        coEvery { imageRepository.getSavedImages() } returns emptyFlow
        
        // When
        val result = getSavedImagesUseCase()
        
        // Then
        assertEquals(emptyFlow, result)
        coVerify(exactly = 1) { imageRepository.getSavedImages() }
    }
}