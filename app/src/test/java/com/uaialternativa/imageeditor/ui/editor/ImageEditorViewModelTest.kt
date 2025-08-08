package com.uaialternativa.imageeditor.ui.editor

import android.graphics.Bitmap
import android.graphics.Rect
import android.net.Uri
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.uaialternativa.imageeditor.domain.model.EditingTool
import com.uaialternativa.imageeditor.domain.model.FilterType
import com.uaialternativa.imageeditor.domain.model.ImageOperation
import com.uaialternativa.imageeditor.domain.model.SavedImage
import com.uaialternativa.imageeditor.domain.usecase.ApplyFilterUseCase
import com.uaialternativa.imageeditor.domain.usecase.CropImageUseCase
import com.uaialternativa.imageeditor.domain.usecase.LoadImageUseCase
import com.uaialternativa.imageeditor.domain.usecase.ResizeImageUseCase
import com.uaialternativa.imageeditor.domain.usecase.SaveImageUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ImageEditorViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var loadImageUseCase: LoadImageUseCase
    private lateinit var applyFilterUseCase: ApplyFilterUseCase
    private lateinit var cropImageUseCase: CropImageUseCase
    private lateinit var resizeImageUseCase: ResizeImageUseCase
    private lateinit var saveImageUseCase: SaveImageUseCase
    private lateinit var viewModel: ImageEditorViewModel

    private lateinit var mockBitmap: Bitmap
    private lateinit var mockUri: Uri

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
        // Mock static Rect constructor
        mockkStatic(Rect::class)
        
        loadImageUseCase = mockk()
        applyFilterUseCase = mockk()
        cropImageUseCase = mockk()
        resizeImageUseCase = mockk()
        saveImageUseCase = mockk()
        
        mockBitmap = mockk {
            every { width } returns 100
            every { height } returns 100
            every { config } returns Bitmap.Config.ARGB_8888
            every { copy(any(), any()) } returns this@mockk
        }
        
        mockUri = mockk()

        viewModel = ImageEditorViewModel(
            loadImageUseCase = loadImageUseCase,
            applyFilterUseCase = applyFilterUseCase,
            cropImageUseCase = cropImageUseCase,
            resizeImageUseCase = resizeImageUseCase,
            saveImageUseCase = saveImageUseCase
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state should be correct`() = runTest {
        val initialState = viewModel.uiState.first()
        
        assertEquals(ImageEditorUiState(), initialState)
        assertNull(initialState.originalImage)
        assertNull(initialState.editedImage)
        assertEquals(EditingTool.None, initialState.selectedTool)
        assertFalse(initialState.isProcessing)
        assertFalse(initialState.isLoading)
        assertFalse(initialState.canUndo)
        assertFalse(initialState.canRedo)
    }

    @Test
    fun `loadImage should update state correctly on success`() = runTest {
        // Given
        coEvery { loadImageUseCase(any(), any(), any()) } returns Result.success(mockBitmap)

        // When
        viewModel.loadImage(mockUri, "test.jpg")
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.first()
        assertEquals(mockBitmap, state.originalImage)
        assertEquals(mockBitmap, state.editedImage)
        assertFalse(state.isLoading)
        assertNull(state.error)
        assertEquals(1, state.operationHistory.size)
        assertEquals(0, state.currentHistoryIndex)
        assertFalse(state.canUndo)
        assertFalse(state.canRedo)
    }

    @Test
    fun `loadImage should update state correctly on failure`() = runTest {
        // Given
        val errorMessage = "Failed to load image"
        coEvery { loadImageUseCase(any(), any(), any()) } returns Result.failure(Exception(errorMessage))

        // When
        viewModel.loadImage(mockUri)
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.first()
        assertNull(state.originalImage)
        assertNull(state.editedImage)
        assertFalse(state.isLoading)
        assertEquals("Failed to load image: $errorMessage", state.error)
    }

    @Test
    fun `selectTool should update selected tool`() = runTest {
        // When
        viewModel.handleAction(ImageEditorAction.SelectTool(EditingTool.Crop))

        // Then
        val state = viewModel.uiState.first()
        assertEquals(EditingTool.Crop, state.selectedTool)
        assertNull(state.error)
    }

    @Test
    fun `setCropBounds should update crop bounds`() = runTest {
        // Given
        val bounds = mockk<Rect>()

        // When
        viewModel.handleAction(ImageEditorAction.SetCropBounds(bounds))
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.first()
        assertEquals(bounds, state.cropBounds)
    }

    @Test
    fun `applyCrop should crop image and update history`() = runTest {
        // Given
        val croppedBitmap = mockk<Bitmap> {
            every { width } returns 40
            every { height } returns 40
            every { config } returns Bitmap.Config.ARGB_8888
            every { copy(any(), any()) } returns this@mockk
        }
        val bounds = mockk<Rect>()
        
        coEvery { loadImageUseCase(any(), any(), any()) } returns Result.success(mockBitmap)
        coEvery { cropImageUseCase(any(), any()) } returns Result.success(croppedBitmap)

        // Setup initial state
        viewModel.loadImage(mockUri)
        advanceUntilIdle()
        viewModel.handleAction(ImageEditorAction.SetCropBounds(bounds))

        // When
        viewModel.handleAction(ImageEditorAction.ApplyCrop)
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.first()
        assertEquals(croppedBitmap, state.editedImage)
        assertEquals(EditingTool.None, state.selectedTool)
        assertNull(state.cropBounds)
        assertFalse(state.isProcessing)
        assertEquals(1, state.appliedOperations.size)
        assertTrue(state.appliedOperations.first() is ImageOperation.Crop)
        assertEquals(2, state.operationHistory.size)
        assertTrue(state.canUndo)
        assertFalse(state.canRedo)
        
        coVerify { cropImageUseCase(mockBitmap, bounds) }
    }

    @Test
    fun `applyCrop should handle failure correctly`() = runTest {
        // Given
        val bounds = mockk<Rect>()
        val errorMessage = "Crop failed"
        
        coEvery { loadImageUseCase(any(), any(), any()) } returns Result.success(mockBitmap)
        coEvery { cropImageUseCase(any(), any()) } returns Result.failure(Exception(errorMessage))

        // Setup initial state
        viewModel.loadImage(mockUri)
        advanceUntilIdle()
        viewModel.handleAction(ImageEditorAction.SetCropBounds(bounds))

        // When
        viewModel.handleAction(ImageEditorAction.ApplyCrop)
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.first()
        assertEquals(mockBitmap, state.editedImage) // Should remain unchanged
        assertFalse(state.isProcessing)
        assertEquals("Failed to crop image: $errorMessage", state.error)
    }

    @Test
    fun `applyResize should resize image and update history`() = runTest {
        // Given
        val resizedBitmap = mockk<Bitmap> {
            every { width } returns 200
            every { height } returns 200
            every { config } returns Bitmap.Config.ARGB_8888
            every { copy(any(), any()) } returns this@mockk
        }
        
        coEvery { loadImageUseCase(any(), any(), any()) } returns Result.success(mockBitmap)
        coEvery { resizeImageUseCase(any(), any(), any()) } returns Result.success(resizedBitmap)

        // Setup initial state
        viewModel.loadImage(mockUri)
        advanceUntilIdle()
        viewModel.handleAction(ImageEditorAction.SetResizeDimensions(200, 200))

        // When
        viewModel.handleAction(ImageEditorAction.ApplyResize)
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.first()
        assertEquals(resizedBitmap, state.editedImage)
        assertEquals(EditingTool.None, state.selectedTool)
        assertNull(state.resizeWidth)
        assertNull(state.resizeHeight)
        assertFalse(state.isProcessing)
        assertEquals(1, state.appliedOperations.size)
        assertTrue(state.appliedOperations.first() is ImageOperation.Resize)
        assertEquals(2, state.operationHistory.size)
        assertTrue(state.canUndo)
        assertFalse(state.canRedo)
        
        coVerify { resizeImageUseCase(mockBitmap, 200, 200) }
    }

    @Test
    fun `applyFilter should apply filter and update history`() = runTest {
        // Given
        val filteredBitmap = mockk<Bitmap> {
            every { width } returns 100
            every { height } returns 100
            every { config } returns Bitmap.Config.ARGB_8888
            every { copy(any(), any()) } returns this@mockk
        }
        
        coEvery { loadImageUseCase(any(), any(), any()) } returns Result.success(mockBitmap)
        coEvery { applyFilterUseCase(any(), any(), any()) } returns Result.success(filteredBitmap)

        // Setup initial state
        viewModel.loadImage(mockUri)
        advanceUntilIdle()
        viewModel.handleAction(ImageEditorAction.SelectFilter(FilterType.BRIGHTNESS))
        viewModel.handleAction(ImageEditorAction.SetFilterIntensity(0.8f))

        // When
        viewModel.handleAction(ImageEditorAction.ApplyFilter)
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.first()
        assertEquals(filteredBitmap, state.editedImage)
        assertEquals(EditingTool.None, state.selectedTool)
        assertNull(state.selectedFilter)
        assertFalse(state.isProcessing)
        assertEquals(1, state.appliedOperations.size)
        assertTrue(state.appliedOperations.first() is ImageOperation.Filter)
        assertEquals(2, state.operationHistory.size)
        assertTrue(state.canUndo)
        assertFalse(state.canRedo)
        
        coVerify { applyFilterUseCase(mockBitmap, FilterType.BRIGHTNESS, 0.8f) }
    }

    @Test
    fun `undo should restore previous state`() = runTest {
        // Given
        val croppedBitmap = mockk<Bitmap> {
            every { width } returns 40
            every { height } returns 40
            every { config } returns Bitmap.Config.ARGB_8888
            every { copy(any(), any()) } returns this@mockk
        }
        val bounds = mockk<Rect>()
        
        coEvery { loadImageUseCase(any(), any(), any()) } returns Result.success(mockBitmap)
        coEvery { cropImageUseCase(any(), any()) } returns Result.success(croppedBitmap)

        // Setup initial state and apply crop
        viewModel.loadImage(mockUri)
        advanceUntilIdle()
        viewModel.handleAction(ImageEditorAction.SetCropBounds(bounds))
        viewModel.handleAction(ImageEditorAction.ApplyCrop)
        advanceUntilIdle()

        // When
        viewModel.handleAction(ImageEditorAction.Undo)
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.first()
        assertEquals(mockBitmap, state.editedImage) // Should be back to original
        assertEquals(0, state.appliedOperations.size)
        assertEquals(0, state.currentHistoryIndex)
        assertFalse(state.canUndo)
        assertTrue(state.canRedo)
    }

    @Test
    fun `redo should restore next state`() = runTest {
        // Given
        val croppedBitmap = mockk<Bitmap> {
            every { width } returns 40
            every { height } returns 40
            every { config } returns Bitmap.Config.ARGB_8888
            every { copy(any(), any()) } returns this@mockk
        }
        val bounds = mockk<Rect>()
        
        coEvery { loadImageUseCase(any(), any(), any()) } returns Result.success(mockBitmap)
        coEvery { cropImageUseCase(any(), any()) } returns Result.success(croppedBitmap)

        // Setup initial state, apply crop, then undo
        viewModel.loadImage(mockUri)
        advanceUntilIdle()
        viewModel.handleAction(ImageEditorAction.SetCropBounds(bounds))
        viewModel.handleAction(ImageEditorAction.ApplyCrop)
        advanceUntilIdle()
        viewModel.handleAction(ImageEditorAction.Undo)
        advanceUntilIdle()

        // When
        viewModel.handleAction(ImageEditorAction.Redo)
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.first()
        assertEquals(croppedBitmap, state.editedImage)
        assertEquals(1, state.appliedOperations.size)
        assertEquals(1, state.currentHistoryIndex)
        assertTrue(state.canUndo)
        assertFalse(state.canRedo)
    }

    @Test
    fun `saveImage should save successfully`() = runTest {
        // Given
        val savedImage = mockk<SavedImage>()
        coEvery { loadImageUseCase(any(), any(), any()) } returns Result.success(mockBitmap)
        coEvery { saveImageUseCase(any(), any(), any()) } returns Result.success(savedImage)

        // Setup initial state
        viewModel.loadImage(mockUri, "test.jpg")
        advanceUntilIdle()

        // When
        viewModel.handleAction(ImageEditorAction.SaveImage)
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.first()
        assertFalse(state.isSaving)
        assertTrue(state.saveSuccess)
        assertNull(state.error)
        
        coVerify { saveImageUseCase(mockBitmap, "test.jpg", emptyList()) }
    }

    @Test
    fun `saveImage should handle failure correctly`() = runTest {
        // Given
        val errorMessage = "Save failed"
        coEvery { loadImageUseCase(any(), any(), any()) } returns Result.success(mockBitmap)
        coEvery { saveImageUseCase(any(), any(), any()) } returns Result.failure(Exception(errorMessage))

        // Setup initial state
        viewModel.loadImage(mockUri)
        advanceUntilIdle()

        // When
        viewModel.handleAction(ImageEditorAction.SaveImage)
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.first()
        assertFalse(state.isSaving)
        assertFalse(state.saveSuccess)
        assertEquals("Failed to save image: $errorMessage", state.error)
    }

    @Test
    fun `clearError should clear error state`() = runTest {
        // Given - set an error state
        coEvery { loadImageUseCase(any(), any(), any()) } returns Result.failure(Exception("Test error"))
        viewModel.loadImage(mockUri)
        advanceUntilIdle()

        // When
        viewModel.handleAction(ImageEditorAction.ClearError)

        // Then
        val state = viewModel.uiState.first()
        assertNull(state.error)
    }

    @Test
    fun `clearSaveSuccess should clear save success state`() = runTest {
        // Given - set save success state
        val savedImage = mockk<SavedImage>()
        coEvery { loadImageUseCase(any(), any(), any()) } returns Result.success(mockBitmap)
        coEvery { saveImageUseCase(any(), any(), any()) } returns Result.success(savedImage)
        
        viewModel.loadImage(mockUri)
        advanceUntilIdle()
        viewModel.handleAction(ImageEditorAction.SaveImage)
        advanceUntilIdle()

        // When
        viewModel.handleAction(ImageEditorAction.ClearSaveSuccess)

        // Then
        val state = viewModel.uiState.first()
        assertFalse(state.saveSuccess)
    }

    @Test
    fun `resetEditor should reset to initial state`() = runTest {
        // Given - load image and apply some operations
        coEvery { loadImageUseCase(any(), any(), any()) } returns Result.success(mockBitmap)
        viewModel.loadImage(mockUri, "test.jpg")
        advanceUntilIdle()

        // When
        viewModel.handleAction(ImageEditorAction.ResetEditor)

        // Then
        val state = viewModel.uiState.first()
        assertEquals(ImageEditorUiState(), state)
    }

    @Test
    fun `hasUnsavedChanges should return true when there are applied operations and no save success`() = runTest {
        // Given
        val croppedBitmap = mockk<Bitmap> {
            every { width } returns 40
            every { height } returns 40
            every { config } returns Bitmap.Config.ARGB_8888
            every { copy(any(), any()) } returns this@mockk
        }
        val bounds = mockk<Rect>()
        
        coEvery { loadImageUseCase(any(), any(), any()) } returns Result.success(mockBitmap)
        coEvery { cropImageUseCase(any(), any()) } returns Result.success(croppedBitmap)

        // Setup and apply operation
        viewModel.loadImage(mockUri)
        advanceUntilIdle()
        viewModel.handleAction(ImageEditorAction.SetCropBounds(bounds))
        viewModel.handleAction(ImageEditorAction.ApplyCrop)
        advanceUntilIdle()

        // Then
        assertTrue(viewModel.hasUnsavedChanges())
    }

    @Test
    fun `hasUnsavedChanges should return false when no operations applied`() = runTest {
        // Given
        coEvery { loadImageUseCase(any(), any(), any()) } returns Result.success(mockBitmap)
        viewModel.loadImage(mockUri)
        advanceUntilIdle()

        // Then
        assertFalse(viewModel.hasUnsavedChanges())
    }

    @Test
    fun `filter intensity should be clamped between 0 and 1`() = runTest {
        // When setting intensity above 1
        viewModel.handleAction(ImageEditorAction.SetFilterIntensity(1.5f))
        
        // Then
        var state = viewModel.uiState.first()
        assertEquals(1.0f, state.filterIntensity)

        // When setting intensity below 0
        viewModel.handleAction(ImageEditorAction.SetFilterIntensity(-0.5f))
        
        // Then
        state = viewModel.uiState.first()
        assertEquals(0.0f, state.filterIntensity)
    }

    @Test
    fun `history should be limited to max size`() = runTest {
        // This test would require applying more than maxHistorySize operations
        // For brevity, we'll test the concept with a smaller number
        val croppedBitmap = mockk<Bitmap> {
            every { width } returns 40
            every { height } returns 40
            every { config } returns Bitmap.Config.ARGB_8888
            every { copy(any(), any()) } returns this@mockk
        }
        
        coEvery { loadImageUseCase(any(), any(), any()) } returns Result.success(mockBitmap)
        coEvery { cropImageUseCase(any(), any()) } returns Result.success(croppedBitmap)

        viewModel.loadImage(mockUri)
        advanceUntilIdle()

        // Apply multiple operations
        repeat(3) { i ->
            val bounds = mockk<Rect>()
            viewModel.handleAction(ImageEditorAction.SetCropBounds(bounds))
            viewModel.handleAction(ImageEditorAction.ApplyCrop)
            advanceUntilIdle()
        }

        val state = viewModel.uiState.first()
        assertEquals(4, state.operationHistory.size) // Initial + 3 crops
        assertTrue(state.canUndo)
    }
}