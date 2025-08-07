package com.uaialternativa.imageeditor.data.processing

import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.Rect
import com.uaialternativa.imageeditor.data.processing.filters.*
import com.uaialternativa.imageeditor.domain.model.FilterType
import com.uaialternativa.imageeditor.domain.repository.ImageProcessingRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of ImageProcessingRepository that handles image transformations and filtering
 */
@Singleton
class ImageProcessingRepositoryImpl @Inject constructor() : ImageProcessingRepository {
    
    private val filters = mapOf(
        FilterType.BRIGHTNESS to BrightnessFilter(),
        FilterType.CONTRAST to ContrastFilter(),
        FilterType.SATURATION to SaturationFilter(),
        FilterType.BLUR to BlurFilter(),
        FilterType.SHARPEN to SharpenFilter(),
        FilterType.SEPIA to SepiaFilter(),
        FilterType.GRAYSCALE to GrayscaleFilter()
    )
    
    override suspend fun cropImage(bitmap: Bitmap, bounds: Rect): Result<Bitmap> = withContext(Dispatchers.Default) {
        try {
            // Validate bitmap
            if (!isBitmapValid(bitmap)) {
                return@withContext Result.failure(IllegalArgumentException("Invalid bitmap"))
            }
            
            // Validate bounds
            if (bounds.left < 0 || bounds.top < 0 || 
                bounds.right > bitmap.width || bounds.bottom > bitmap.height ||
                bounds.width() <= 0 || bounds.height() <= 0) {
                return@withContext Result.failure(IllegalArgumentException("Invalid crop bounds"))
            }
            
            val croppedBitmap = Bitmap.createBitmap(
                bitmap,
                bounds.left,
                bounds.top,
                bounds.width(),
                bounds.height()
            )
            
            Result.success(croppedBitmap)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun resizeImage(bitmap: Bitmap, width: Int, height: Int): Result<Bitmap> = withContext(Dispatchers.Default) {
        try {
            // Validate bitmap
            if (!isBitmapValid(bitmap)) {
                return@withContext Result.failure(IllegalArgumentException("Invalid bitmap"))
            }
            
            // Validate dimensions
            if (width <= 0 || height <= 0) {
                return@withContext Result.failure(IllegalArgumentException("Invalid dimensions"))
            }
            
            // Check if resize is needed
            if (bitmap.width == width && bitmap.height == height) {
                return@withContext Result.success(bitmap.copy(bitmap.config ?: Bitmap.Config.ARGB_8888, false))
            }
            
            val matrix = Matrix().apply {
                val scaleX = width.toFloat() / bitmap.width
                val scaleY = height.toFloat() / bitmap.height
                setScale(scaleX, scaleY)
            }
            
            val resizedBitmap = Bitmap.createBitmap(
                bitmap,
                0,
                0,
                bitmap.width,
                bitmap.height,
                matrix,
                true
            )
            
            Result.success(resizedBitmap)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun applyFilter(bitmap: Bitmap, filter: FilterType, intensity: Float): Result<Bitmap> = withContext(Dispatchers.Default) {
        try {
            // Validate bitmap
            if (!isBitmapValid(bitmap)) {
                return@withContext Result.failure(IllegalArgumentException("Invalid bitmap"))
            }
            
            // Validate intensity
            val clampedIntensity = intensity.coerceIn(0f, 1f)
            
            // If intensity is 0, return original bitmap
            if (clampedIntensity == 0f) {
                return@withContext Result.success(bitmap.copy(bitmap.config ?: Bitmap.Config.ARGB_8888, false))
            }
            
            val imageFilter = filters[filter] 
                ?: return@withContext Result.failure(IllegalArgumentException("Unsupported filter type: $filter"))
            
            val filteredBitmap = imageFilter.apply(bitmap, clampedIntensity)
            
            Result.success(filteredBitmap)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Recycle bitmap safely to free memory
     */
    private fun recycleBitmapSafely(bitmap: Bitmap?) {
        try {
            if (bitmap != null && !bitmap.isRecycled) {
                bitmap.recycle()
            }
        } catch (e: Exception) {
            // Ignore recycling errors - bitmap might already be recycled
        }
    }
    
    /**
     * Check if bitmap is valid for processing
     */
    private fun isBitmapValid(bitmap: Bitmap?): Boolean {
        return bitmap != null && !bitmap.isRecycled && bitmap.width > 0 && bitmap.height > 0
    }
}