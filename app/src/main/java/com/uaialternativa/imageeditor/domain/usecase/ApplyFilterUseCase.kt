package com.uaialternativa.imageeditor.domain.usecase

import android.graphics.Bitmap
import com.uaialternativa.imageeditor.domain.model.FilterType
import com.uaialternativa.imageeditor.domain.repository.ImageProcessingRepository
import javax.inject.Inject

/**
 * Use case for applying filters to images
 */
class ApplyFilterUseCase @Inject constructor(
    private val imageProcessingRepository: ImageProcessingRepository
) {
    /**
     * Execute the use case to apply a filter to an image
     * @param bitmap The original bitmap
     * @param filterType The type of filter to apply
     * @param intensity The intensity of the filter (0.0 to 1.0)
     * @return Result containing the filtered bitmap or error
     */
    suspend operator fun invoke(
        bitmap: Bitmap,
        filterType: FilterType,
        intensity: Float = 1.0f
    ): Result<Bitmap> {
        // Validate intensity parameter
        val validIntensity = intensity.coerceIn(0.0f, 1.0f)
        
        return imageProcessingRepository.applyFilter(bitmap, filterType, validIntensity)
    }
}