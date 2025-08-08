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
 * Filter for adjusting image contrast
 */
class ContrastFilter : ImageFilter {
    
    override suspend fun apply(bitmap: Bitmap, intensity: Float): Bitmap = withContext(Dispatchers.Default) {
        val resultBitmap = Bitmap.createBitmap(bitmap.width, bitmap.height, bitmap.config ?: Bitmap.Config.ARGB_8888)
        val canvas = Canvas(resultBitmap)
        
        // Contrast adjustment: intensity range 0.0 to 2.0, where 1.0 = no change
        val contrast = 1f + intensity
        val translate = (1f - contrast) / 2f * 255f
        
        val colorMatrix = ColorMatrix().apply {
            set(floatArrayOf(
                contrast, 0f, 0f, 0f, translate,
                0f, contrast, 0f, 0f, translate,
                0f, 0f, contrast, 0f, translate,
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