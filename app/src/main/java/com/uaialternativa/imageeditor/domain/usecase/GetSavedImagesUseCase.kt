package com.uaialternativa.imageeditor.domain.usecase

import com.uaialternativa.imageeditor.domain.model.SavedImage
import com.uaialternativa.imageeditor.domain.repository.ImageRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for retrieving all saved images from the gallery
 */
class GetSavedImagesUseCase @Inject constructor(
    private val imageRepository: ImageRepository
) {
    /**
     * Execute the use case to get all saved images
     * @return Flow of list of saved images
     */
    suspend operator fun invoke(): Flow<List<SavedImage>> {
        return imageRepository.getSavedImages()
    }
}