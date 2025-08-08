package com.uaialternativa.imageeditor.domain.usecase

import com.uaialternativa.imageeditor.domain.repository.ImageRepository
import javax.inject.Inject

/**
 * Use case for deleting saved images from storage and database
 */
class DeleteImageUseCase @Inject constructor(
    private val imageRepository: ImageRepository
) {
    /**
     * Execute the use case to delete an image
     * @param imageId The ID of the image to delete
     * @return Result indicating success or failure
     */
    suspend operator fun invoke(imageId: String): Result<Unit> {
        return imageRepository.deleteImage(imageId)
    }
}