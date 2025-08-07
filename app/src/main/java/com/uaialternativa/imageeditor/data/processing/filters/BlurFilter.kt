package com.uaialternativa.imageeditor.data.processing.filters

import android.graphics.Bitmap
import com.uaialternativa.imageeditor.data.processing.ImageFilter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Filter for applying blur effect to images
 * Note: This implementation uses a simple box blur for compatibility
 */
class BlurFilter : ImageFilter {
    
    override suspend fun apply(bitmap: Bitmap, intensity: Float): Bitmap = withContext(Dispatchers.Default) {
        // Simple box blur implementation
        val radius = (intensity * 10f).coerceIn(0f, 10f).toInt()
        if (radius == 0) return@withContext bitmap.copy(bitmap.config ?: Bitmap.Config.ARGB_8888, false)
        
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        
        // Apply horizontal blur
        val tempPixels = IntArray(width * height)
        for (y in 0 until height) {
            for (x in 0 until width) {
                var r = 0
                var g = 0
                var b = 0
                var a = 0
                var count = 0
                
                for (dx in -radius..radius) {
                    val nx = (x + dx).coerceIn(0, width - 1)
                    val pixel = pixels[y * width + nx]
                    
                    a += (pixel shr 24) and 0xFF
                    r += (pixel shr 16) and 0xFF
                    g += (pixel shr 8) and 0xFF
                    b += pixel and 0xFF
                    count++
                }
                
                tempPixels[y * width + x] = (a / count shl 24) or
                        (r / count shl 16) or
                        (g / count shl 8) or
                        (b / count)
            }
        }
        
        // Apply vertical blur
        for (x in 0 until width) {
            for (y in 0 until height) {
                var r = 0
                var g = 0
                var b = 0
                var a = 0
                var count = 0
                
                for (dy in -radius..radius) {
                    val ny = (y + dy).coerceIn(0, height - 1)
                    val pixel = tempPixels[ny * width + x]
                    
                    a += (pixel shr 24) and 0xFF
                    r += (pixel shr 16) and 0xFF
                    g += (pixel shr 8) and 0xFF
                    b += pixel and 0xFF
                    count++
                }
                
                pixels[y * width + x] = (a / count shl 24) or
                        (r / count shl 16) or
                        (g / count shl 8) or
                        (b / count)
            }
        }
        
        val resultBitmap = Bitmap.createBitmap(width, height, bitmap.config ?: Bitmap.Config.ARGB_8888)
        resultBitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        
        resultBitmap
    }
}