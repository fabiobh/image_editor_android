package com.uaialternativa.imageeditor.domain.usecase

import android.graphics.Bitmap
import com.uaialternativa.imageeditor.domain.model.ImageOperation
import com.uaialternativa.imageeditor.domain.model.SavedImage
import com.uaialternativa.imageeditor.domain.repository.ImageMetadata
import com.uaialternativa.imageeditor.domain.repository.ImageRepository
import javax.inject.Inject

/**
 * Use case for saving edited images with metadata
 */
class SaveImageUseCase @Inject constructor(
    private val imageRepository: ImageRepository
) {
    /**
     * Execute the use case to save an edited image
     * @param image The bitmap to save
     * @param originalFileName The original filename if available
     * @param appliedOperations List of operations applied to the image
     * @return Result containing the saved image or error
     */
    suspend operator fun invoke(
        image: Bitmap,
        originalFileName: String? = null,
        appliedOperations: List<ImageOperation> = emptyList()
    ): Result<SavedImage> {
        val metadata = ImageMetadata(
            originalFileName = originalFileName,
            width = image.width,
            height = image.height,
            appliedOperations = appliedOperations
        )
        
        return imageRepository.saveImage(image, metadata)
    }
}