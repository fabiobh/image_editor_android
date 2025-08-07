package com.uaialternativa.imageeditor.domain.model

import android.graphics.Rect

/**
 * Sealed class representing different image editing operations
 */
sealed class ImageOperation {
    /**
     * Crop operation with specified bounds
     */
    data class Crop(val bounds: Rect) : ImageOperation()
    
    /**
     * Resize operation with new dimensions
     */
    data class Resize(val width: Int, val height: Int) : ImageOperation()
    
    /**
     * Filter operation with type and intensity
     */
    data class Filter(val type: FilterType, val intensity: Float) : ImageOperation()
}