package com.uaialternativa.imageeditor.domain.repository

import android.graphics.Bitmap
import android.graphics.Rect
import com.uaialternativa.imageeditor.domain.model.FilterType

/**
 * Repository interface for image processing operations
 */
interface ImageProcessingRepository {
    /**
     * Crop an image to the specified bounds
     * @param bitmap The original bitmap
     * @param bounds The crop bounds
     * @return Result containing the cropped bitmap or error
     */
    suspend fun cropImage(bitmap: Bitmap, bounds: Rect): Result<Bitmap>
    
    /**
     * Resize an image to new dimensions
     * @param bitmap The original bitmap
     * @param width The new width
     * @param height The new height
     * @return Result containing the resized bitmap or error
     */
    suspend fun resizeImage(bitmap: Bitmap, width: Int, height: Int): Result<Bitmap>
    
    /**
     * Apply a filter to an image
     * @param bitmap The original bitmap
     * @param filter The type of filter to apply
     * @param intensity The intensity of the filter (0.0 to 1.0)
     * @return Result containing the filtered bitmap or error
     */
    suspend fun applyFilter(bitmap: Bitmap, filter: FilterType, intensity: Float): Result<Bitmap>
}