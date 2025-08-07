package com.uaialternativa.imageeditor.domain.usecase

import android.graphics.Bitmap
import com.uaialternativa.imageeditor.domain.repository.ImageProcessingRepository
import javax.inject.Inject

/**
 * Use case for resizing images to new dimensions
 */
class ResizeImageUseCase @Inject constructor(
    private val imageProcessingRepository: ImageProcessingRepository
) {
    /**
     * Execute the use case to resize an image
     * @param bitmap The original bitmap
     * @param width The new width
     * @param height The new height
     * @return Result containing the resized bitmap or error
     */
    suspend operator fun invoke(
        bitmap: Bitmap,
        width: Int,
        height: Int
    ): Result<Bitmap> {
        // Validate dimensions
        if (width <= 0 || height <= 0) {
            return Result.failure(IllegalArgumentException("Width and height must be positive"))
        }
        
        return imageProcessingRepository.resizeImage(bitmap, width, height)
    }
    
    /**
     * Resize image while maintaining aspect ratio
     * @param bitmap The original bitmap
     * @param maxWidth Maximum width
     * @param maxHeight Maximum height
     * @return Result containing the resized bitmap or error
     */
    suspend fun resizeWithAspectRatio(
        bitmap: Bitmap,
        maxWidth: Int,
        maxHeight: Int
    ): Result<Bitmap> {
        val originalWidth = bitmap.width
        val originalHeight = bitmap.height
        
        val aspectRatio = originalWidth.toFloat() / originalHeight.toFloat()
        
        val (newWidth, newHeight) = if (originalWidth > originalHeight) {
            // Landscape orientation
            val width = minOf(maxWidth, originalWidth)
            val height = (width / aspectRatio).toInt()
            width to height
        } else {
            // Portrait orientation
            val height = minOf(maxHeight, originalHeight)
            val width = (height * aspectRatio).toInt()
            width to height
        }
        
        return invoke(bitmap, newWidth, newHeight)
    }
}