package com.uaialternativa.imageeditor.data.processing.filters

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import com.uaialternativa.imageeditor.data.processing.ImageFilter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Filter for adjusting image brightness
 */
class BrightnessFilter : ImageFilter {
    
    override suspend fun apply(bitmap: Bitmap, intensity: Float): Bitmap = withContext(Dispatchers.Default) {
        val resultBitmap = Bitmap.createBitmap(bitmap.width, bitmap.height, bitmap.config ?: Bitmap.Config.ARGB_8888)
        val canvas = Canvas(resultBitmap)
        
        // Create brightness adjustment matrix
        // Intensity range: -1.0 (darkest) to 1.0 (brightest), 0.0 = no change
        val brightnessValue = intensity * 255f
        val colorMatrix = ColorMatrix().apply {
            set(floatArrayOf(
                1f, 0f, 0f, 0f, brightnessValue,
                0f, 1f, 0f, 0f, brightnessValue,
                0f, 0f, 1f, 0f, brightnessValue,
                0f, 0f, 0f, 1f, 0f
            ))
        }
        
        val paint = Paint().apply {
            colorFilter = ColorMatrixColorFilter(colorMatrix)
        }
        
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        
        resultBitmap
    }
}