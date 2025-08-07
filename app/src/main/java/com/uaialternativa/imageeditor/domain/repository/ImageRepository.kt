package com.uaialternativa.imageeditor.domain.repository

import android.graphics.Bitmap
import com.uaialternativa.imageeditor.domain.model.SavedImage
import kotlinx.coroutines.flow.Flow
import java.io.File

/**
 * Repository interface for managing saved images and their metadata
 */
interface ImageRepository {
    /**
     * Get all saved images as a Flow for reactive updates
     */
    suspend fun getSavedImages(): Flow<List<SavedImage>>
    
    /**
     * Save an edited image with metadata
     * @param image The bitmap to save
     * @param metadata Additional metadata for the image
     * @return Result containing the saved image or error
     */
    suspend fun saveImage(image: Bitmap, metadata: ImageMetadata): Result<SavedImage>
    
    /**
     * Delete an image by its ID
     * @param imageId The ID of the image to delete
     * @return Result indicating success or failure
     */
    suspend fun deleteImage(imageId: String): Result<Unit>
    
    /**
     * Get the file for a saved image
     * @param imagePath The path to the image file
     * @return Result containing the file or error
     */
    suspend fun getImageFile(imagePath: String): Result<File>
}

/**
 * Data class containing metadata for saving images
 */
data class ImageMetadata(
    val originalFileName: String?,
    val width: Int,
    val height: Int,
    val appliedOperations: List<com.uaialternativa.imageeditor.domain.model.ImageOperation>
)