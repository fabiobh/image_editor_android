package com.uaialternativa.imageeditor.data.processing

import android.graphics.Bitmap

/**
 * Interface for image filters
 */
interface ImageFilter {
    /**
     * Apply the filter to a bitmap
     * @param bitmap The input bitmap
     * @param intensity The filter intensity (0.0 to 1.0)
     * @return The filtered bitmap
     */
    suspend fun apply(bitmap: Bitmap, intensity: Float): Bitmap
}