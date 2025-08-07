package com.uaialternativa.imageeditor.data.file

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages file operations for the image editor app.
 * Handles image storage in app-specific directory with proper validation and cleanup.
 */
@Singleton
class FileManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val EDITED_IMAGES_DIR = "edited_images"
        private const val IMAGE_PREFIX = "edited_image"
        private const val IMAGE_EXTENSION = ".jpg"
        private const val MAX_FILE_SIZE_MB = 50L
        private const val MAX_FILE_SIZE_BYTES = MAX_FILE_SIZE_MB * 1024 * 1024
        
        // Supported image formats for validation
        private val SUPPORTED_EXTENSIONS = setOf(".jpg", ".jpeg", ".png", ".webp")
        private val SUPPORTED_MIME_TYPES = setOf(
            "image/jpeg", 
            "image/png", 
            "image/webp"
        )
    }

    private val editedImagesDir: File by lazy {
        File(context.filesDir, EDITED_IMAGES_DIR).apply {
            if (!exists()) {
                mkdirs()
            }
        }
    }

    /**
     * Saves a bitmap to the app's edited images directory
     * @param bitmap The bitmap to save
     * @param quality JPEG compression quality (0-100)
     * @return Result containing the saved file or error
     */
    suspend fun saveBitmap(
        bitmap: Bitmap,
        quality: Int = 90
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val fileName = generateUniqueFileName()
            val file = File(editedImagesDir, fileName)
            
            // Validate bitmap
            if (bitmap.isRecycled) {
                return@withContext Result.failure(
                    IllegalArgumentException("Cannot save recycled bitmap")
                )
            }
            
            // Save bitmap to file
            FileOutputStream(file).use { outputStream ->
                val success = bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
                if (!success) {
                    return@withContext Result.failure(
                        IOException("Failed to compress bitmap to file")
                    )
                }
            }
            
            // Validate saved file
            if (!file.exists() || file.length() == 0L) {
                return@withContext Result.failure(
                    IOException("File was not saved properly")
                )
            }
            
            Result.success(file)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Deletes an image file from storage
     * @param filePath The path to the file to delete
     * @return Result indicating success or failure
     */
    suspend fun deleteFile(filePath: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val file = File(filePath)
            
            // Validate file path is within our directory
            if (!isValidFilePath(file)) {
                return@withContext Result.failure(
                    SecurityException("File path is not within app directory")
                )
            }
            
            if (file.exists() && file.delete()) {
                Result.success(Unit)
            } else {
                Result.failure(IOException("Failed to delete file: $filePath"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Gets a file from the given path with validation
     * @param filePath The path to the file
     * @return Result containing the file or error
     */
    suspend fun getFile(filePath: String): Result<File> = withContext(Dispatchers.IO) {
        try {
            val file = File(filePath)
            
            // Validate file path
            if (!isValidFilePath(file)) {
                return@withContext Result.failure(
                    SecurityException("File path is not within app directory")
                )
            }
            
            if (!file.exists()) {
                return@withContext Result.failure(
                    IOException("File does not exist: $filePath")
                )
            }
            
            if (!file.canRead()) {
                return@withContext Result.failure(
                    IOException("Cannot read file: $filePath")
                )
            }
            
            Result.success(file)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Validates if a file exists and is readable
     * @param filePath The path to validate
     * @return true if file is valid, false otherwise
     */
    suspend fun isValidFile(filePath: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val file = File(filePath)
            isValidFilePath(file) && file.exists() && file.canRead() && file.length() > 0
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Gets the size of a file in bytes
     * @param filePath The path to the file
     * @return Result containing file size or error
     */
    suspend fun getFileSize(filePath: String): Result<Long> = withContext(Dispatchers.IO) {
        try {
            val file = File(filePath)
            
            if (!isValidFilePath(file)) {
                return@withContext Result.failure(
                    SecurityException("File path is not within app directory")
                )
            }
            
            if (!file.exists()) {
                return@withContext Result.failure(
                    IOException("File does not exist: $filePath")
                )
            }
            
            Result.success(file.length())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Cleans up old or invalid files from the edited images directory
     * @param maxAgeMillis Maximum age of files to keep (default: 30 days)
     * @return Result containing number of files cleaned up
     */
    suspend fun cleanupOldFiles(
        maxAgeMillis: Long = 30L * 24 * 60 * 60 * 1000 // 30 days
    ): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val currentTime = System.currentTimeMillis()
            var cleanedCount = 0
            
            editedImagesDir.listFiles()?.forEach { file ->
                if (file.isFile) {
                    val fileAge = currentTime - file.lastModified()
                    if (fileAge > maxAgeMillis || file.length() == 0L) {
                        if (file.delete()) {
                            cleanedCount++
                        }
                    }
                }
            }
            
            Result.success(cleanedCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Gets all files in the edited images directory
     * @return Result containing list of files or error
     */
    suspend fun getAllEditedImageFiles(): Result<List<File>> = withContext(Dispatchers.IO) {
        try {
            val files = editedImagesDir.listFiles()?.filter { file ->
                file.isFile && file.length() > 0 && isValidImageFile(file)
            } ?: emptyList()
            
            Result.success(files.sortedByDescending { it.lastModified() })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Gets the total size of all edited images
     * @return Result containing total size in bytes
     */
    suspend fun getTotalStorageUsed(): Result<Long> = withContext(Dispatchers.IO) {
        try {
            val totalSize = editedImagesDir.listFiles()?.sumOf { file ->
                if (file.isFile) file.length() else 0L
            } ?: 0L
            
            Result.success(totalSize)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Validates if an image format is supported
     * @param uri The URI to validate
     * @param mimeType The MIME type of the image
     * @return true if format is supported
     */
    fun isSupportedImageFormat(uri: Uri?, mimeType: String?): Boolean {
        // Check MIME type
        if (mimeType != null && SUPPORTED_MIME_TYPES.contains(mimeType.lowercase())) {
            return true
        }
        
        // Check file extension from URI
        uri?.path?.let { path ->
            val extension = path.substringAfterLast('.', "").lowercase()
            if (extension.isNotEmpty() && SUPPORTED_EXTENSIONS.contains(".$extension")) {
                return true
            }
        }
        
        return false
    }

    /**
     * Generates a unique filename for edited images
     * @return A unique filename with timestamp and UUID
     */
    private fun generateUniqueFileName(): String {
        val timestamp = System.currentTimeMillis()
        val uuid = UUID.randomUUID().toString().take(8)
        return "${IMAGE_PREFIX}_${timestamp}_${uuid}${IMAGE_EXTENSION}"
    }

    /**
     * Validates if a file path is within the app's directory structure
     * @param file The file to validate
     * @return true if path is valid and secure
     */
    private fun isValidFilePath(file: File): Boolean {
        return try {
            val canonicalFile = file.canonicalFile
            val canonicalAppDir = editedImagesDir.canonicalFile
            canonicalFile.path.startsWith(canonicalAppDir.path)
        } catch (e: IOException) {
            false
        }
    }

    /**
     * Validates if a file is a valid image file
     * @param file The file to validate
     * @return true if file is a valid image
     */
    private fun isValidImageFile(file: File): Boolean {
        val extension = file.extension.lowercase()
        return SUPPORTED_EXTENSIONS.contains(".$extension") && 
               file.length() in 1..MAX_FILE_SIZE_BYTES
    }
}