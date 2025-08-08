package com.uaialternativa.imageeditor.ui.gallery

import com.uaialternativa.imageeditor.domain.model.SavedImage

/**
 * UI state for the gallery screen
 */
data class GalleryUiState(
    val images: List<SavedImage> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val isDeleting: Boolean = false,
    val deletingImageId: String? = null
)