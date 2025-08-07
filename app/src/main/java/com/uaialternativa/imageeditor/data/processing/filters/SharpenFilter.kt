package com.uaialternativa.imageeditor.data.processing.filters

import android.graphics.Bitmap
import com.uaialternativa.imageeditor.data.processing.ImageFilter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min

/**
 * Filter for sharpening images using convolution
 */
class SharpenFilter : ImageFilter {
    
    override suspend fun apply(bitmap: Bitmap, intensity: Float): Bitmap = withContext(Dispatchers.Default) {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        
        val resultPixels = IntArray(width * height)
        
        // Sharpen kernel with adjustable intensity
        val kernel = floatArrayOf(
            0f, -intensity, 0f,
            -intensity, 1f + 4f * intensity, -intensity,
            0f, -intensity, 0f
        )
        
        for (y in 1 until height - 1) {
            for (x in 1 until width - 1) {
                var r = 0f
                var g = 0f
                var b = 0f
                var a = 0f
                
                var kernelIndex = 0
                for (ky in -1..1) {
                    for (kx in -1..1) {
                        val pixelIndex = (y + ky) * width + (x + kx)
                        val pixel = pixels[pixelIndex]
                        val weight = kernel[kernelIndex++]
                        
                        a += ((pixel shr 24) and 0xFF) * weight
                        r += ((pixel shr 16) and 0xFF) * weight
                        g += ((pixel shr 8) and 0xFF) * weight
                        b += (pixel and 0xFF) * weight
                    }
                }
                
                // Clamp values to valid range
                val clampedA = max(0, min(255, a.toInt()))
                val clampedR = max(0, min(255, r.toInt()))
                val clampedG = max(0, min(255, g.toInt()))
                val clampedB = max(0, min(255, b.toInt()))
                
                resultPixels[y * width + x] = (clampedA shl 24) or
                        (clampedR shl 16) or
                        (clampedG shl 8) or
                        clampedB
            }
        }
        
        // Copy edge pixels unchanged
        for (y in 0 until height) {
            for (x in 0 until width) {
                if (x == 0 || x == width - 1 || y == 0 || y == height - 1) {
                    resultPixels[y * width + x] = pixels[y * width + x]
                }
            }
        }
        
        val resultBitmap = Bitmap.createBitmap(width, height, bitmap.config ?: Bitmap.Config.ARGB_8888)
        resultBitmap.setPixels(resultPixels, 0, width, 0, 0, width, height)
        
        resultBitmap
    }
}