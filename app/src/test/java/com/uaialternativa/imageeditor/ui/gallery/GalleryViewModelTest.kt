package com.uaialternativa.imageeditor.ui.gallery

import com.uaialternativa.imageeditor.domain.model.ImageEditorError
import com.uaialternativa.imageeditor.domain.model.ImageOperation
import com.uaialternativa.imageeditor.domain.model.SavedImage
import com.uaialternativa.imageeditor.domain.usecase.DeleteImageUseCase
import com.uaialternativa.imageeditor.domain.usecase.GetSavedImagesUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GalleryViewModelTest {

    private lateinit var viewModel: GalleryViewModel
    private val getSavedImagesUseCase: GetSavedImagesUseCase = mockk()
    private val deleteImageUseCase: DeleteImageUseCase = mockk()
    
    private val testDispatcher = StandardTestDispatcher()
    private val testScheduler = testDispatcher.scheduler

    private val sampleImages = listOf(
        SavedImage(
            id = "1",
            fileName = "image1.jpg",
            filePath = "/path/to/image1.jpg",
            originalFileName = "original1.jpg",
            width = 1920,
            height = 1080,
            fileSize = 1024000,
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
            fileSize = 512000,
            createdAt = System.currentTimeMillis(),
            modifiedAt = System.currentTimeMillis(),
            appliedOperations = listOf(ImageOperation.Crop(android.graphics.Rect(0, 0, 100, 100)))
        )
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
        // Default mock behavior - return empty list
        coEvery { getSavedImagesUseCase() } returns flowOf(emptyList())
    }
    
    private fun createViewModel() {
        viewModel = GalleryViewModel(getSavedImagesUseCase, deleteImageUseCase)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state should be correct after initialization`() = runTest {
        // Given - ViewModel is initialized
        createViewModel()
        advanceUntilIdle() // Complete initialization
        
        // When - checking final state after initialization
        val finalState = viewModel.uiState.value
        
        // Then - state should have correct values after initialization
        assertTrue(finalState.images.isEmpty()) // Empty list from mock
        assertFalse(finalState.isLoading) // Loading should be complete
        assertNull(finalState.error)
        assertFalse(finalState.isDeleting)
        assertNull(finalState.deletingImageId)
    }

    @Test
    fun `loadImages should update state with images when successful`() = runTest {
        // Given
        coEvery { getSavedImagesUseCase() } returns flowOf(sampleImages)
        createViewModel()
        
        // When
        advanceUntilIdle() // Let init complete
        
        // Then
        val state = viewModel.uiState.value
        assertEquals(sampleImages, state.images)
        assertFalse(state.isLoading)
        assertNull(state.error)
    }

    @Test
    fun `loadImages should handle database error correctly`() = runTest {
        // Given
        val error = ImageEditorError.DatabaseError("Database connection failed")
        coEvery { getSavedImagesUseCase() } throws error
        createViewModel()
        
        // When
        advanceUntilIdle() // Let init complete
        
        // Then
        val state = viewModel.uiState.value
        assertTrue(state.images.isEmpty())
        assertFalse(state.isLoading)
        assertEquals("Database connection failed", state.error)
    }

    @Test
    fun `loadImages should handle generic error correctly`() = runTest {
        // Given
        val error = RuntimeException("Network error")
        coEvery { getSavedImagesUseCase() } throws error
        createViewModel()
        
        // When
        advanceUntilIdle() // Let init complete
        
        // Then
        val state = viewModel.uiState.value
        assertTrue(state.images.isEmpty())
        assertFalse(state.isLoading)
        assertEquals("Failed to load images", state.error)
    }

    @Test
    fun `deleteImage should complete successfully`() = runTest {
        // Given
        val imageId = "1"
        coEvery { deleteImageUseCase(imageId) } returns Result.success(Unit)
        createViewModel()
        advanceUntilIdle() // Complete initialization
        
        // When
        viewModel.deleteImage(imageId)
        advanceUntilIdle() // Complete deletion
        
        // Then - check final state
        val finalState = viewModel.uiState.value
        assertFalse(finalState.isDeleting)
        assertNull(finalState.deletingImageId)
        assertNull(finalState.error)
        
        // Verify use case was called
        coVerify { deleteImageUseCase(imageId) }
    }

    @Test
    fun `deleteImage should handle deletion failure correctly`() = runTest {
        // Given
        val imageId = "1"
        val error = ImageEditorError.FileSystemError("Failed to delete file")
        coEvery { deleteImageUseCase(imageId) } returns Result.failure(error)
        createViewModel()
        advanceUntilIdle() // Complete initialization
        
        // When
        viewModel.deleteImage(imageId)
        advanceUntilIdle()
        
        // Then
        val state = viewModel.uiState.value
        assertFalse(state.isDeleting)
        assertNull(state.deletingImageId)
        assertEquals("Failed to delete file", state.error)
    }

    @Test
    fun `deleteImage should handle exception during deletion`() = runTest {
        // Given
        val imageId = "1"
        coEvery { deleteImageUseCase(imageId) } throws RuntimeException("Unexpected error")
        createViewModel()
        advanceUntilIdle() // Complete initialization
        
        // When
        viewModel.deleteImage(imageId)
        advanceUntilIdle()
        
        // Then
        val state = viewModel.uiState.value
        assertFalse(state.isDeleting)
        assertNull(state.deletingImageId)
        assertEquals("Failed to delete image", state.error)
    }

    @Test
    fun `clearError should remove error from state`() = runTest {
        // Given - state with error
        coEvery { getSavedImagesUseCase() } throws RuntimeException("Test error")
        createViewModel()
        advanceUntilIdle()
        
        // Verify error exists
        assertTrue(viewModel.uiState.value.error != null)
        
        // When
        viewModel.clearError()
        
        // Then
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `refresh should call loadImages`() = runTest {
        // Given
        coEvery { getSavedImagesUseCase() } returns flowOf(sampleImages)
        createViewModel()
        advanceUntilIdle() // Complete initialization
        
        // When
        viewModel.refresh()
        advanceUntilIdle()
        
        // Then
        coVerify(exactly = 2) { getSavedImagesUseCase() } // Once in init, once in refresh
        val state = viewModel.uiState.value
        assertEquals(sampleImages, state.images)
        assertFalse(state.isLoading)
    }

    @Test
    fun `multiple delete operations should be handled correctly`() = runTest {
        // Given
        val imageId1 = "1"
        val imageId2 = "2"
        coEvery { deleteImageUseCase(imageId1) } returns Result.success(Unit)
        coEvery { deleteImageUseCase(imageId2) } returns Result.success(Unit)
        createViewModel()
        advanceUntilIdle() // Complete initialization
        
        // When - perform first deletion
        viewModel.deleteImage(imageId1)
        advanceUntilIdle() // Complete first deletion
        
        // When - perform second deletion
        viewModel.deleteImage(imageId2)
        advanceUntilIdle() // Complete second deletion
        
        // Then - verify final state
        assertNull(viewModel.uiState.value.deletingImageId)
        assertFalse(viewModel.uiState.value.isDeleting)
        assertNull(viewModel.uiState.value.error)
        
        // Verify both use cases were called
        coVerify { deleteImageUseCase(imageId1) }
        coVerify { deleteImageUseCase(imageId2) }
    }

    @Test
    fun `error handling should work for different ImageEditorError types`() = runTest {
        // Test StoragePermissionDenied
        coEvery { getSavedImagesUseCase() } throws ImageEditorError.StoragePermissionDenied
        createViewModel()
        advanceUntilIdle()
        assertEquals("Storage permission is required to save images", viewModel.uiState.value.error)
        
        // Test InsufficientMemory
        coEvery { getSavedImagesUseCase() } throws ImageEditorError.InsufficientMemory
        createViewModel()
        advanceUntilIdle()
        assertEquals("Insufficient memory to process the image", viewModel.uiState.value.error)
        
        // Test ProcessingFailed
        coEvery { getSavedImagesUseCase() } throws ImageEditorError.ProcessingFailed("test operation")
        createViewModel()
        advanceUntilIdle()
        assertEquals("Failed to process image: test operation", viewModel.uiState.value.error)
    }

    @Test
    fun `state should be correct after successful image loading`() = runTest {
        // Given
        coEvery { getSavedImagesUseCase() } returns flowOf(sampleImages)
        
        // When - create ViewModel and complete loading
        createViewModel()
        advanceUntilIdle()
        
        // Then - check final state
        val finalState = viewModel.uiState.value
        assertFalse(finalState.isLoading)
        assertEquals(sampleImages, finalState.images)
        assertNull(finalState.error)
    }
}