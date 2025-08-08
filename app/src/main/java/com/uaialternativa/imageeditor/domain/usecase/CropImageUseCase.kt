package com.uaialternativa.imageeditor.domain.usecase

import android.graphics.Bitmap
import android.graphics.Rect
import com.uaialternativa.imageeditor.domain.repository.ImageProcessingRepository
import javax.inject.Inject

/**
 * Use case for cropping images to specified bounds
 */
class CropImageUseCase @Inject constructor(
    private val imageProcessingRepository: ImageProcessingRepository
) {
    /**
     * Execute the use case to crop an image
     * @param bitmap The original bitmap
     * @param bounds The crop bounds
     * @return Result containing the cropped bitmap or error
     */
    suspend operator fun invoke(
        bitmap: Bitmap,
        bounds: Rect
    ): Result<Bitmap> {
        // Validate crop bounds
        val validBounds = validateCropBounds(bitmap, bounds)
        
        return imageProcessingRepository.cropImage(bitmap, validBounds)
    }
    
    /**
     * Validate and adjust crop bounds to ensure they are within image dimensions
     */
    private fun validateCropBounds(bitmap: Bitmap, bounds: Rect): Rect {
        val imageWidth = bitmap.width
        val imageHeight = bitmap.height
        
        return Rect(
            bounds.left.coerceIn(0, imageWidth),
            bounds.top.coerceIn(0, imageHeight),
            bounds.right.coerceIn(bounds.left, imageWidth),
            bounds.bottom.coerceIn(bounds.top, imageHeight)
        )
    }
}