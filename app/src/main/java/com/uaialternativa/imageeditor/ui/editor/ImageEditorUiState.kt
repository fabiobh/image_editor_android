package com.uaialternativa.imageeditor.ui.editor

import android.graphics.Bitmap
import android.graphics.Rect
import com.uaialternativa.imageeditor.domain.model.EditingTool
import com.uaialternativa.imageeditor.domain.model.FilterType
import com.uaialternativa.imageeditor.domain.model.ImageOperation

/**
 * UI state for the image editor screen
 */
data class ImageEditorUiState(
    val originalImage: Bitmap? = null,
    val editedImage: Bitmap? = null,
    val selectedTool: EditingTool = EditingTool.None,
    val isProcessing: Boolean = false,
    val isLoading: Boolean = false,
    val cropBounds: Rect? = null,
    val resizeWidth: Int? = null,
    val resizeHeight: Int? = null,
    val selectedFilter: FilterType? = null,
    val filterIntensity: Float = 1.0f,
    val appliedFilters: List<AppliedFilter> = emptyList(),
    val appliedOperations: List<ImageOperation> = emptyList(),
    val operationHistory: List<ImageEditorHistoryState> = emptyList(),
    val currentHistoryIndex: Int = -1,
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val error: String? = null,
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false
)

/**
 * Represents a state in the editing history for undo/redo functionality
 */
data class ImageEditorHistoryState(
    val image: Bitmap,
    val operations: List<ImageOperation>,
    val description: String
)

/**
 * Represents an applied filter with its settings
 */
data class AppliedFilter(
    val id: String,
    val filterType: FilterType,
    val intensity: Float,
    val appliedAt: Long = System.currentTimeMillis()
)

/**
 * Represents different types of editing actions that can be performed
 */
sealed class ImageEditorAction {
    object LoadImage : ImageEditorAction()
    data class SelectTool(val tool: EditingTool) : ImageEditorAction()
    data class SetCropBounds(val bounds: Rect) : ImageEditorAction()
    object ApplyCrop : ImageEditorAction()
    data class SetResizeDimensions(val width: Int, val height: Int) : ImageEditorAction()
    object ApplyResize : ImageEditorAction()
    data class SelectFilter(val filterType: FilterType) : ImageEditorAction()
    data class SetFilterIntensity(val intensity: Float) : ImageEditorAction()
    object ApplyFilter : ImageEditorAction()
    data class RemoveFilter(val filterId: String) : ImageEditorAction()
    object ClearAllFilters : ImageEditorAction()
    object Undo : ImageEditorAction()
    object Redo : ImageEditorAction()
    object SaveImage : ImageEditorAction()
    object ClearError : ImageEditorAction()
    object ClearSaveSuccess : ImageEditorAction()
    object ResetEditor : ImageEditorAction()
}