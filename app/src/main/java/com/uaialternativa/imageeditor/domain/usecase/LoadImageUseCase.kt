package com.uaialternativa.imageeditor.domain.usecase

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.FileNotFoundException
import java.io.IOException
import javax.inject.Inject

/**
 * Use case for loading images from URIs or file paths
 */
class LoadImageUseCase @Inject constructor(
    @ApplicationContext private val context: Context
) {
    /**
     * Execute the use case to load an image from a URI
     * @param uri The URI of the image to load
     * @param maxWidth Maximum width for the loaded image (optional)
     * @param maxHeight Maximum height for the loaded image (optional)
     * @return Result containing the loaded bitmap or error
     */
    suspend operator fun invoke(
        uri: Uri,
        maxWidth: Int? = null,
        maxHeight: Int? = null
    ): Result<Bitmap> = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return@withContext Result.failure(FileNotFoundException("Cannot open input stream for URI: $uri"))
            
            inputStream.use { stream ->
                val options = BitmapFactory.Options()
                
                // If max dimensions are specified, calculate sample size
                if (maxWidth != null && maxHeight != null) {
                    // First decode with inJustDecodeBounds=true to check dimensions
                    options.inJustDecodeBounds = true
                    BitmapFactory.decodeStream(stream, null, options)
                    
                    // Calculate sample size
                    options.inSampleSize = calculateInSampleSize(options, maxWidth, maxHeight)
                    options.inJustDecodeBounds = false
                    
                    // Reopen stream for actual decoding
                    val newInputStream = context.contentResolver.openInputStream(uri)
                        ?: return@withContext Result.failure(FileNotFoundException("Cannot reopen input stream for URI: $uri"))
                    
                    newInputStream.use { newStream ->
                        val bitmap = BitmapFactory.decodeStream(newStream, null, options)
                        if (bitmap != null) {
                            Result.success(bitmap)
                        } else {
                            Result.failure(IOException("Failed to decode bitmap from URI: $uri"))
                        }
                    }
                } else {
                    val bitmap = BitmapFactory.decodeStream(stream, null, options)
                    if (bitmap != null) {
                        Result.success(bitmap)
                    } else {
                        Result.failure(IOException("Failed to decode bitmap from URI: $uri"))
                    }
                }
            }
        } catch (e: FileNotFoundException) {
            Result.failure(e)
        } catch (e: IOException) {
            Result.failure(e)
        } catch (e: SecurityException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(IOException("Unexpected error loading image: ${e.message}", e))
        }
    }
    
    /**
     * Load an image from a file path
     * @param filePath The file path of the image to load
     * @param maxWidth Maximum width for the loaded image (optional)
     * @param maxHeight Maximum height for the loaded image (optional)
     * @return Result containing the loaded bitmap or error
     */
    suspend fun loadFromPath(
        filePath: String,
        maxWidth: Int? = null,
        maxHeight: Int? = null
    ): Result<Bitmap> = withContext(Dispatchers.IO) {
        try {
            val options = BitmapFactory.Options()
            
            // If max dimensions are specified, calculate sample size
            if (maxWidth != null && maxHeight != null) {
                // First decode with inJustDecodeBounds=true to check dimensions
                options.inJustDecodeBounds = true
                BitmapFactory.decodeFile(filePath, options)
                
                // Calculate sample size
                options.inSampleSize = calculateInSampleSize(options, maxWidth, maxHeight)
                options.inJustDecodeBounds = false
            }
            
            val bitmap = BitmapFactory.decodeFile(filePath, options)
            if (bitmap != null) {
                Result.success(bitmap)
            } else {
                Result.failure(IOException("Failed to decode bitmap from file: $filePath"))
            }
        } catch (e: Exception) {
            Result.failure(IOException("Error loading image from path: ${e.message}", e))
        }
    }
    
    /**
     * Calculate the sample size for efficient image loading
     */
    private fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1
        
        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            
            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }
        
        return inSampleSize
    }
}