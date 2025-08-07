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
 * Filter for applying sepia tone effect
 */
class SepiaFilter : ImageFilter {
    
    override suspend fun apply(bitmap: Bitmap, intensity: Float): Bitmap = withContext(Dispatchers.Default) {
        val resultBitmap = Bitmap.createBitmap(bitmap.width, bitmap.height, bitmap.config ?: Bitmap.Config.ARGB_8888)
        val canvas = Canvas(resultBitmap)
        
        // Sepia color matrix with intensity blending
        val sepiaR = 0.393f * intensity + (1f - intensity)
        val sepiaG = 0.769f * intensity
        val sepiaB = 0.189f * intensity
        
        val sepiaMatrix = ColorMatrix().apply {
            set(floatArrayOf(
                sepiaR, sepiaG, sepiaB, 0f, 0f,
                0.349f * intensity, 0.686f * intensity + (1f - intensity), 0.168f * intensity, 0f, 0f,
                0.272f * intensity, 0.534f * intensity, 0.131f * intensity + (1f - intensity), 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            ))
        }
        
        val paint = Paint().apply {
            colorFilter = ColorMatrixColorFilter(sepiaMatrix)
        }
        
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        
        resultBitmap
    }
}