package com.uaialternativa.imageeditor.ui.picker

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream

/**
 * Manager class for handling image picker operations with validation and error handling
 */
class ImagePickerManager(
    private val context: Context
) {
    companion object {
        // Maximum file size: 50MB
        private const val MAX_FILE_SIZE_BYTES = 50 * 1024 * 1024L
        
        // Maximum image dimensions
        private const val MAX_IMAGE_WIDTH = 8192
        private const val MAX_IMAGE_HEIGHT = 8192
        
        // Supported image formats
        private val SUPPORTED_MIME_TYPES = setOf(
            "image/jpeg",
            "image/jpg", 
            "image/png",
            "image/webp",
            "image/bmp",
            "image/gif"
        )
        
        private val SUPPORTED_EXTENSIONS = setOf(
            "jpg", "jpeg", "png", "webp", "bmp", "gif"
        )
    }

    /**
     * Launch the image picker
     */
    fun launchImagePicker(launcher: ManagedActivityResultLauncher<PickVisualMediaRequest, Uri?>) {
        launcher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
    }

    /**
     * Validate and process the selected image URI
     */
    suspend fun validateAndProcessImage(uri: Uri): ImagePickerResult = withContext(Dispatchers.IO) {
        try {
            // Check if URI is accessible
            val contentResolver = context.contentResolver
            val inputStream = contentResolver.openInputStream(uri)
                ?: return@withContext ImagePickerResult.Error("Cannot access selected image")

            inputStream.use { stream ->
                // Get file information
                val fileInfo = getFileInfo(uri)
                
                // Validate file size
                if (fileInfo.size > MAX_FILE_SIZE_BYTES) {
                    return@withContext ImagePickerResult.Error(
                        "Image file is too large (${formatFileSize(fileInfo.size)}). Maximum size is ${formatFileSize(MAX_FILE_SIZE_BYTES)}"
                    )
                }
                
                // Validate MIME type
                if (!SUPPORTED_MIME_TYPES.contains(fileInfo.mimeType)) {
                    return@withContext ImagePickerResult.Error(
                        "Unsupported image format: ${fileInfo.mimeType}. Supported formats: JPEG, PNG, WebP, BMP, GIF"
                    )
                }
                
                // Validate file extension
                val extension = getFileExtension(fileInfo.name)
                if (extension != null && !SUPPORTED_EXTENSIONS.contains(extension.lowercase())) {
                    return@withContext ImagePickerResult.Error(
                        "Unsupported file extension: .$extension. Supported extensions: ${SUPPORTED_EXTENSIONS.joinToString(", ")}"
                    )
                }
                
                // Get image dimensions without loading the full bitmap
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                
                // Reset stream for dimension checking
                contentResolver.openInputStream(uri)?.use { dimensionStream ->
                    BitmapFactory.decodeStream(dimensionStream, null, options)
                }
                
                val imageWidth = options.outWidth
                val imageHeight = options.outHeight
                
                // Validate image dimensions
                if (imageWidth <= 0 || imageHeight <= 0) {
                    return@withContext ImagePickerResult.Error("Invalid image: Cannot determine image dimensions")
                }
                
                if (imageWidth > MAX_IMAGE_WIDTH || imageHeight > MAX_IMAGE_HEIGHT) {
                    return@withContext ImagePickerResult.Error(
                        "Image dimensions too large (${imageWidth}x${imageHeight}). Maximum dimensions: ${MAX_IMAGE_WIDTH}x${MAX_IMAGE_HEIGHT}"
                    )
                }
                
                // Validate that it's actually an image
                if (options.outMimeType == null) {
                    return@withContext ImagePickerResult.Error("Selected file is not a valid image")
                }
                
                return@withContext ImagePickerResult.Success(
                    uri = uri,
                    fileName = fileInfo.name,
                    fileSize = fileInfo.size,
                    mimeType = fileInfo.mimeType,
                    width = imageWidth,
                    height = imageHeight
                )
            }
        } catch (e: SecurityException) {
            ImagePickerResult.Error("Permission denied: Cannot access selected image")
        } catch (e: Exception) {
            ImagePickerResult.Error("Failed to process image: ${e.message}")
        }
    }

    /**
     * Get file information from URI
     */
    private fun getFileInfo(uri: Uri): FileInfo {
        val contentResolver = context.contentResolver
        var name = "unknown"
        var size = 0L
        var mimeType = "application/octet-stream"

        // Try to get file info from content resolver
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                
                if (nameIndex != -1) {
                    cursor.getString(nameIndex)?.let { name = it }
                }
                
                if (sizeIndex != -1) {
                    size = cursor.getLong(sizeIndex)
                }
            }
        }

        // Get MIME type
        contentResolver.getType(uri)?.let { mimeType = it }

        // If size is still 0, try to get it from input stream
        if (size == 0L) {
            try {
                contentResolver.openInputStream(uri)?.use { stream ->
                    size = stream.available().toLong()
                }
            } catch (e: Exception) {
                // Ignore, keep size as 0
            }
        }

        return FileInfo(name, size, mimeType)
    }

    /**
     * Get file extension from filename
     */
    private fun getFileExtension(fileName: String): String? {
        return fileName.substringAfterLast('.', "").takeIf { it.isNotEmpty() }
    }

    /**
     * Format file size for display
     */
    private fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
            else -> "${bytes / (1024 * 1024 * 1024)} GB"
        }
    }

    /**
     * Data class for file information
     */
    private data class FileInfo(
        val name: String,
        val size: Long,
        val mimeType: String
    )
}

/**
 * Result of image picker validation and processing
 */
sealed class ImagePickerResult {
    data class Success(
        val uri: Uri,
        val fileName: String,
        val fileSize: Long,
        val mimeType: String,
        val width: Int,
        val height: Int
    ) : ImagePickerResult()
    
    data class Error(val message: String) : ImagePickerResult()
    
    object Cancelled : ImagePickerResult()
}