package com.uaialternativa.imageeditor.ui.gallery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uaialternativa.imageeditor.domain.model.ImageEditorError
import com.uaialternativa.imageeditor.domain.usecase.DeleteImageUseCase
import com.uaialternativa.imageeditor.domain.usecase.GetSavedImagesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for managing gallery screen state and operations
 */
@HiltViewModel
class GalleryViewModel @Inject constructor(
    private val getSavedImagesUseCase: GetSavedImagesUseCase,
    private val deleteImageUseCase: DeleteImageUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(GalleryUiState())
    val uiState: StateFlow<GalleryUiState> = _uiState.asStateFlow()

    init {
        loadImages()
    }

    /**
     * Load all saved images from the repository
     */
    fun loadImages() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                getSavedImagesUseCase()
                    .catch { throwable ->
                        handleError(throwable, "Failed to load images")
                    }
                    .collect { images ->
                        _uiState.value = _uiState.value.copy(
                            images = images,
                            isLoading = false,
                            error = null
                        )
                    }
            } catch (e: Exception) {
                handleError(e, "Failed to load images")
            }
        }
    }

    /**
     * Delete an image from storage and database
     * @param imageId The ID of the image to delete
     */
    fun deleteImage(imageId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isDeleting = true,
                deletingImageId = imageId,
                error = null
            )

            try {
                val result = deleteImageUseCase(imageId)
                
                result.fold(
                    onSuccess = {
                        _uiState.value = _uiState.value.copy(
                            isDeleting = false,
                            deletingImageId = null
                        )
                        // Images will be automatically updated through the Flow from loadImages()
                    },
                    onFailure = { throwable ->
                        handleError(throwable, "Failed to delete image")
                        _uiState.value = _uiState.value.copy(
                            isDeleting = false,
                            deletingImageId = null
                        )
                    }
                )
            } catch (e: Exception) {
                handleError(e, "Failed to delete image")
                _uiState.value = _uiState.value.copy(
                    isDeleting = false,
                    deletingImageId = null
                )
            }
        }
    }

    /**
     * Clear any error messages
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    /**
     * Refresh the gallery by reloading images
     */
    fun refresh() {
        loadImages()
    }

    /**
     * Handle errors and update UI state with appropriate error messages
     */
    private fun handleError(throwable: Throwable, defaultMessage: String) {
        val errorMessage = when (throwable) {
            is ImageEditorError.DatabaseError -> throwable.message
            is ImageEditorError.FileSystemError -> throwable.message
            is ImageEditorError.StoragePermissionDenied -> throwable.message
            is ImageEditorError.InsufficientMemory -> throwable.message
            is ImageEditorError -> throwable.message ?: defaultMessage
            else -> defaultMessage
        }

        _uiState.value = _uiState.value.copy(
            error = errorMessage,
            isLoading = false
        )
    }
}