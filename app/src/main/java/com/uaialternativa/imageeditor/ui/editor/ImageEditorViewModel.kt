package com.uaialternativa.imageeditor.ui.editor

import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uaialternativa.imageeditor.domain.model.EditingTool
import com.uaialternativa.imageeditor.domain.model.FilterType
import com.uaialternativa.imageeditor.domain.model.ImageOperation
import com.uaialternativa.imageeditor.domain.usecase.ApplyFilterUseCase
import com.uaialternativa.imageeditor.domain.usecase.CropImageUseCase
import com.uaialternativa.imageeditor.domain.usecase.LoadImageUseCase
import com.uaialternativa.imageeditor.domain.usecase.ResizeImageUseCase
import com.uaialternativa.imageeditor.domain.usecase.SaveImageUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for managing image editor state and operations
 */
@HiltViewModel
class ImageEditorViewModel @Inject constructor(
    private val loadImageUseCase: LoadImageUseCase,
    private val applyFilterUseCase: ApplyFilterUseCase,
    private val cropImageUseCase: CropImageUseCase,
    private val resizeImageUseCase: ResizeImageUseCase,
    private val saveImageUseCase: SaveImageUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ImageEditorUiState())
    val uiState: StateFlow<ImageEditorUiState> = _uiState.asStateFlow()

    private val maxHistorySize = 20
    private var originalFileName: String? = null

    /**
     * Load an image from URI into the editor
     */
    fun loadImage(uri: Uri, fileName: String? = null) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                error = null
            )

            originalFileName = fileName
            
            loadImageUseCase(uri, maxWidth = 2048, maxHeight = 2048)
                .onSuccess { bitmap ->
                    val initialState = ImageEditorHistoryState(
                        image = bitmap.copy(bitmap.config ?: Bitmap.Config.ARGB_8888, false),
                        operations = emptyList(),
                        description = "Original image"
                    )
                    
                    _uiState.value = _uiState.value.copy(
                        originalImage = bitmap,
                        editedImage = bitmap,
                        isLoading = false,
                        operationHistory = listOf(initialState),
                        currentHistoryIndex = 0,
                        canUndo = false,
                        canRedo = false,
                        appliedOperations = emptyList()
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Failed to load image: ${error.message}"
                    )
                }
        }
    }

    /**
     * Handle different editor actions
     */
    fun handleAction(action: ImageEditorAction) {
        when (action) {
            is ImageEditorAction.SelectTool -> selectTool(action.tool)
            is ImageEditorAction.SetCropBounds -> setCropBounds(action.bounds)
            is ImageEditorAction.ApplyCrop -> applyCrop()
            is ImageEditorAction.SetResizeDimensions -> setResizeDimensions(action.width, action.height)
            is ImageEditorAction.ApplyResize -> applyResize()
            is ImageEditorAction.SelectFilter -> selectFilter(action.filterType)
            is ImageEditorAction.SetFilterIntensity -> setFilterIntensity(action.intensity)
            is ImageEditorAction.ApplyFilter -> applyFilter()
            is ImageEditorAction.RemoveFilter -> removeFilter(action.filterId)
            is ImageEditorAction.ClearAllFilters -> clearAllFilters()
            is ImageEditorAction.Undo -> undo()
            is ImageEditorAction.Redo -> redo()
            is ImageEditorAction.SaveImage -> saveImage()
            is ImageEditorAction.ClearError -> clearError()
            is ImageEditorAction.ClearSaveSuccess -> clearSaveSuccess()
            is ImageEditorAction.ResetEditor -> resetEditor()
            else -> { /* Handle other actions if needed */ }
        }
    }

    private fun selectTool(tool: EditingTool) {
        _uiState.value = _uiState.value.copy(
            selectedTool = tool,
            error = null
        )
    }

    private fun setCropBounds(bounds: android.graphics.Rect) {
        _uiState.value = _uiState.value.copy(cropBounds = bounds)
    }

    private fun applyCrop() {
        val currentState = _uiState.value
        val image = currentState.editedImage ?: return
        val bounds = currentState.cropBounds ?: return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isProcessing = true)

            cropImageUseCase(image, bounds)
                .onSuccess { croppedImage ->
                    val operation = ImageOperation.Crop(bounds)
                    val newOperations = currentState.appliedOperations + operation
                    
                    addToHistory(
                        image = croppedImage,
                        operations = newOperations,
                        description = "Crop applied"
                    )

                    _uiState.value = _uiState.value.copy(
                        editedImage = croppedImage,
                        appliedOperations = newOperations,
                        selectedTool = EditingTool.None,
                        cropBounds = null,
                        isProcessing = false
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isProcessing = false,
                        error = "Failed to crop image: ${error.message}"
                    )
                }
        }
    }

    private fun setResizeDimensions(width: Int, height: Int) {
        _uiState.value = _uiState.value.copy(
            resizeWidth = width,
            resizeHeight = height
        )
    }

    private fun applyResize() {
        val currentState = _uiState.value
        val image = currentState.editedImage ?: return
        val width = currentState.resizeWidth ?: return
        val height = currentState.resizeHeight ?: return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isProcessing = true)

            resizeImageUseCase(image, width, height)
                .onSuccess { resizedImage ->
                    val operation = ImageOperation.Resize(width, height)
                    val newOperations = currentState.appliedOperations + operation
                    
                    addToHistory(
                        image = resizedImage,
                        operations = newOperations,
                        description = "Resize applied (${width}x${height})"
                    )

                    _uiState.value = _uiState.value.copy(
                        editedImage = resizedImage,
                        appliedOperations = newOperations,
                        selectedTool = EditingTool.None,
                        resizeWidth = null,
                        resizeHeight = null,
                        isProcessing = false
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isProcessing = false,
                        error = "Failed to resize image: ${error.message}"
                    )
                }
        }
    }

    private fun selectFilter(filterType: FilterType) {
        _uiState.value = _uiState.value.copy(
            selectedFilter = filterType,
            filterIntensity = 1.0f
        )
    }

    private fun setFilterIntensity(intensity: Float) {
        _uiState.value = _uiState.value.copy(
            filterIntensity = intensity.coerceIn(0.0f, 1.0f)
        )
    }

    private fun applyFilter() {
        val currentState = _uiState.value
        val image = currentState.editedImage ?: return
        val filterType = currentState.selectedFilter ?: return
        val intensity = currentState.filterIntensity

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isProcessing = true)

            applyFilterUseCase(image, filterType, intensity)
                .onSuccess { filteredImage ->
                    val operation = ImageOperation.Filter(filterType, intensity)
                    val newOperations = currentState.appliedOperations + operation
                    
                    // Create applied filter for stacking
                    val appliedFilter = AppliedFilter(
                        id = java.util.UUID.randomUUID().toString(),
                        filterType = filterType,
                        intensity = intensity
                    )
                    val newAppliedFilters = currentState.appliedFilters + appliedFilter
                    
                    addToHistory(
                        image = filteredImage,
                        operations = newOperations,
                        description = "${filterType.name} filter applied"
                    )

                    _uiState.value = _uiState.value.copy(
                        editedImage = filteredImage,
                        appliedOperations = newOperations,
                        appliedFilters = newAppliedFilters,
                        selectedTool = EditingTool.None,
                        selectedFilter = null,
                        isProcessing = false
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isProcessing = false,
                        error = "Failed to apply filter: ${error.message}"
                    )
                }
        }
    }

    private fun removeFilter(filterId: String) {
        val currentState = _uiState.value
        val filterToRemove = currentState.appliedFilters.find { it.id == filterId } ?: return
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isProcessing = true)
            
            // Start with original image and reapply all filters except the one being removed
            val originalImage = currentState.originalImage ?: return@launch
            val remainingFilters = currentState.appliedFilters.filter { it.id != filterId }
            
            var resultImage = originalImage
            val newOperations = mutableListOf<ImageOperation>()
            
            // Reapply all other operations first (crop, resize)
            for (operation in currentState.appliedOperations) {
                when (operation) {
                    is ImageOperation.Crop -> {
                        cropImageUseCase(resultImage, operation.bounds)
                            .onSuccess { croppedImage ->
                                resultImage = croppedImage
                                newOperations.add(operation)
                            }
                            .onFailure { 
                                _uiState.value = _uiState.value.copy(
                                    isProcessing = false,
                                    error = "Failed to reapply crop when removing filter"
                                )
                                return@launch
                            }
                    }
                    is ImageOperation.Resize -> {
                        resizeImageUseCase(resultImage, operation.width, operation.height)
                            .onSuccess { resizedImage ->
                                resultImage = resizedImage
                                newOperations.add(operation)
                            }
                            .onFailure { 
                                _uiState.value = _uiState.value.copy(
                                    isProcessing = false,
                                    error = "Failed to reapply resize when removing filter"
                                )
                                return@launch
                            }
                    }
                    is ImageOperation.Filter -> {
                        // Only reapply filters that are not being removed
                        if (remainingFilters.any { it.filterType == operation.type && it.intensity == operation.intensity }) {
                            applyFilterUseCase(resultImage, operation.type, operation.intensity)
                                .onSuccess { filteredImage ->
                                    resultImage = filteredImage
                                    newOperations.add(operation)
                                }
                                .onFailure { 
                                    _uiState.value = _uiState.value.copy(
                                        isProcessing = false,
                                        error = "Failed to reapply filter when removing filter"
                                    )
                                    return@launch
                                }
                        }
                    }
                }
            }
            
            addToHistory(
                image = resultImage,
                operations = newOperations,
                description = "${filterToRemove.filterType.name} filter removed"
            )
            
            _uiState.value = _uiState.value.copy(
                editedImage = resultImage,
                appliedOperations = newOperations,
                appliedFilters = remainingFilters,
                isProcessing = false
            )
        }
    }

    private fun clearAllFilters() {
        val currentState = _uiState.value
        if (currentState.appliedFilters.isEmpty()) return
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isProcessing = true)
            
            // Start with original image and reapply only non-filter operations
            val originalImage = currentState.originalImage ?: return@launch
            var resultImage = originalImage
            val newOperations = mutableListOf<ImageOperation>()
            
            // Reapply only crop and resize operations
            for (operation in currentState.appliedOperations) {
                when (operation) {
                    is ImageOperation.Crop -> {
                        cropImageUseCase(resultImage, operation.bounds)
                            .onSuccess { croppedImage ->
                                resultImage = croppedImage
                                newOperations.add(operation)
                            }
                            .onFailure { 
                                _uiState.value = _uiState.value.copy(
                                    isProcessing = false,
                                    error = "Failed to reapply crop when clearing filters"
                                )
                                return@launch
                            }
                    }
                    is ImageOperation.Resize -> {
                        resizeImageUseCase(resultImage, operation.width, operation.height)
                            .onSuccess { resizedImage ->
                                resultImage = resizedImage
                                newOperations.add(operation)
                            }
                            .onFailure { 
                                _uiState.value = _uiState.value.copy(
                                    isProcessing = false,
                                    error = "Failed to reapply resize when clearing filters"
                                )
                                return@launch
                            }
                    }
                    is ImageOperation.Filter -> {
                        // Skip all filter operations
                    }
                }
            }
            
            addToHistory(
                image = resultImage,
                operations = newOperations,
                description = "All filters cleared"
            )
            
            _uiState.value = _uiState.value.copy(
                editedImage = resultImage,
                appliedOperations = newOperations,
                appliedFilters = emptyList(),
                isProcessing = false
            )
        }
    }

    private fun addToHistory(image: Bitmap, operations: List<ImageOperation>, description: String) {
        val currentState = _uiState.value
        val currentHistory = currentState.operationHistory
        val currentIndex = currentState.currentHistoryIndex

        // Remove any history after current index (for redo functionality)
        val trimmedHistory = if (currentIndex >= 0) {
            currentHistory.take(currentIndex + 1)
        } else {
            emptyList()
        }

        // Add new state to history
        val newHistoryState = ImageEditorHistoryState(
            image = image.copy(image.config ?: Bitmap.Config.ARGB_8888, false),
            operations = operations.toList(),
            description = description
        )

        val newHistory = (trimmedHistory + newHistoryState).takeLast(maxHistorySize)
        val newIndex = newHistory.size - 1

        _uiState.value = _uiState.value.copy(
            operationHistory = newHistory,
            currentHistoryIndex = newIndex,
            canUndo = newIndex > 0,
            canRedo = false
        )
    }

    private fun undo() {
        val currentState = _uiState.value
        val currentIndex = currentState.currentHistoryIndex
        
        if (currentIndex > 0) {
            val newIndex = currentIndex - 1
            val historyState = currentState.operationHistory[newIndex]
            
            // Rebuild applied filters from operations
            val appliedFilters = historyState.operations
                .filterIsInstance<ImageOperation.Filter>()
                .map { filterOp ->
                    AppliedFilter(
                        id = java.util.UUID.randomUUID().toString(),
                        filterType = filterOp.type,
                        intensity = filterOp.intensity
                    )
                }
            
            _uiState.value = _uiState.value.copy(
                editedImage = historyState.image,
                appliedOperations = historyState.operations,
                appliedFilters = appliedFilters,
                currentHistoryIndex = newIndex,
                canUndo = newIndex > 0,
                canRedo = true,
                selectedTool = EditingTool.None,
                cropBounds = null,
                selectedFilter = null
            )
        }
    }

    private fun redo() {
        val currentState = _uiState.value
        val currentIndex = currentState.currentHistoryIndex
        val historySize = currentState.operationHistory.size
        
        if (currentIndex < historySize - 1) {
            val newIndex = currentIndex + 1
            val historyState = currentState.operationHistory[newIndex]
            
            // Rebuild applied filters from operations
            val appliedFilters = historyState.operations
                .filterIsInstance<ImageOperation.Filter>()
                .map { filterOp ->
                    AppliedFilter(
                        id = java.util.UUID.randomUUID().toString(),
                        filterType = filterOp.type,
                        intensity = filterOp.intensity
                    )
                }
            
            _uiState.value = _uiState.value.copy(
                editedImage = historyState.image,
                appliedOperations = historyState.operations,
                appliedFilters = appliedFilters,
                currentHistoryIndex = newIndex,
                canUndo = true,
                canRedo = newIndex < historySize - 1,
                selectedTool = EditingTool.None,
                cropBounds = null,
                selectedFilter = null
            )
        }
    }

    private fun saveImage() {
        val currentState = _uiState.value
        val image = currentState.editedImage ?: return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true)

            saveImageUseCase(
                image = image,
                originalFileName = originalFileName,
                appliedOperations = currentState.appliedOperations
            )
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        saveSuccess = true
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        error = "Failed to save image: ${error.message}"
                    )
                }
        }
    }

    private fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    private fun clearSaveSuccess() {
        _uiState.value = _uiState.value.copy(saveSuccess = false)
    }

    private fun resetEditor() {
        _uiState.value = ImageEditorUiState()
        originalFileName = null
    }

    /**
     * Get preview of filter application without applying it permanently
     */
    fun getFilterPreview(filterType: FilterType, intensity: Float = 1.0f): Bitmap? {
        val image = _uiState.value.editedImage ?: return null
        
        // This would typically be done with a separate preview mechanism
        // For now, we'll return the current image as a placeholder
        return image
    }

    /**
     * Check if there are unsaved changes
     */
    fun hasUnsavedChanges(): Boolean {
        val currentState = _uiState.value
        return currentState.appliedOperations.isNotEmpty() && !currentState.saveSuccess
    }
}