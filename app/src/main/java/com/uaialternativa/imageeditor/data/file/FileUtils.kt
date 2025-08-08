package com.uaialternativa.imageeditor.data.file

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.DecimalFormat
import kotlin.math.log10
import kotlin.math.pow

/**
 * Utility functions for file operations and path management
 */
object FileUtils {
    
    /**
     * Formats file size in human-readable format
     * @param bytes Size in bytes
     * @return Formatted string (e.g., "1.5 MB", "256 KB")
     */
    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (log10(bytes.toDouble()) / log10(1024.0)).toInt()
        
        return DecimalFormat("#,##0.#").format(
            bytes / 1024.0.pow(digitGroups.toDouble())
        ) + " " + units[digitGroups]
    }
    
    /**
     * Gets the file extension from a filename
     * @param filename The filename to extract extension from
     * @return The file extension (without dot) or empty string
     */
    fun getFileExtension(filename: String): String {
        return filename.substringAfterLast('.', "").lowercase()
    }
    
    /**
     * Gets the filename without extension
     * @param filename The full filename
     * @return Filename without extension
     */
    fun getFileNameWithoutExtension(filename: String): String {
        return filename.substringBeforeLast('.')
    }
    
    /**
     * Validates if a filename is safe (no path traversal attempts)
     * @param filename The filename to validate
     * @return true if filename is safe
     */
    fun isSafeFilename(filename: String): Boolean {
        if (filename.isBlank()) return false
        
        // Check for path traversal attempts
        if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
            return false
        }
        
        // Check for reserved characters
        val reservedChars = charArrayOf('<', '>', ':', '"', '|', '?', '*')
        if (filename.any { it in reservedChars }) {
            return false
        }
        
        // Check length
        if (filename.length > 255) return false
        
        return true
    }
    
    /**
     * Sanitizes a filename by removing or replacing invalid characters
     * @param filename The filename to sanitize
     * @return Sanitized filename
     */
    fun sanitizeFilename(filename: String): String {
        if (filename.isBlank()) return "untitled"
        
        return filename
            .replace(Regex("[<>:\"|?*]"), "_")
            .replace(Regex("[/\\\\]"), "_")
            .replace("..", "_")
            .take(255)
            .trim()
            .ifEmpty { "untitled" }
    }
    
    /**
     * Gets the MIME type of a file from its URI
     * @param context Android context
     * @param uri The URI to get MIME type for
     * @return MIME type or null if not found
     */
    suspend fun getMimeType(context: Context, uri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.getType(uri)
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Gets the display name of a file from its URI
     * @param context Android context
     * @param uri The URI to get display name for
     * @return Display name or null if not found
     */
    suspend fun getDisplayName(context: Context, uri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.query(
                uri,
                arrayOf(MediaStore.Images.Media.DISPLAY_NAME),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME)
                    if (nameIndex >= 0) {
                        cursor.getString(nameIndex)
                    } else null
                } else null
            }
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Gets the size of a file from its URI
     * @param context Android context
     * @param uri The URI to get size for
     * @return File size in bytes or -1 if not found
     */
    suspend fun getFileSize(context: Context, uri: Uri): Long = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.query(
                uri,
                arrayOf(MediaStore.Images.Media.SIZE),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val sizeIndex = cursor.getColumnIndex(MediaStore.Images.Media.SIZE)
                    if (sizeIndex >= 0) {
                        cursor.getLong(sizeIndex)
                    } else -1L
                } else -1L
            } ?: -1L
        } catch (e: Exception) {
            -1L
        }
    }
    
    /**
     * Checks if there's enough storage space for a file
     * @param directory The directory to check
     * @param requiredBytes The required space in bytes
     * @return true if there's enough space
     */
    fun hasEnoughSpace(directory: File, requiredBytes: Long): Boolean {
        return try {
            directory.usableSpace >= requiredBytes
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Gets available storage space in a directory
     * @param directory The directory to check
     * @return Available space in bytes or -1 if error
     */
    fun getAvailableSpace(directory: File): Long {
        return try {
            directory.usableSpace
        } catch (e: Exception) {
            -1L
        }
    }
    
    /**
     * Creates a backup filename by appending a number
     * @param originalPath The original file path
     * @return A new path with backup suffix
     */
    fun createBackupFilename(originalPath: String): String {
        val file = File(originalPath)
        val nameWithoutExt = getFileNameWithoutExtension(file.name)
        val extension = getFileExtension(file.name)
        val parent = file.parent ?: ""
        
        var counter = 1
        var backupPath: String
        
        do {
            val backupName = if (extension.isNotEmpty()) {
                "${nameWithoutExt}_backup_$counter.$extension"
            } else {
                "${nameWithoutExt}_backup_$counter"
            }
            backupPath = File(parent, backupName).path
            counter++
        } while (File(backupPath).exists() && counter < 1000)
        
        return backupPath
    }
    
    /**
     * Validates if a path is within a specific directory (prevents path traversal)
     * @param path The path to validate
     * @param allowedDirectory The directory that should contain the path
     * @return true if path is safe and within allowed directory
     */
    fun isPathWithinDirectory(path: String, allowedDirectory: File): Boolean {
        return try {
            val file = File(path).canonicalFile
            val allowed = allowedDirectory.canonicalFile
            file.path.startsWith(allowed.path + File.separator) || file.path == allowed.path
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Deletes a directory and all its contents recursively
     * @param directory The directory to delete
     * @return true if deletion was successful
     */
    fun deleteDirectoryRecursively(directory: File): Boolean {
        return try {
            if (directory.isDirectory) {
                directory.listFiles()?.forEach { file ->
                    if (file.isDirectory) {
                        deleteDirectoryRecursively(file)
                    } else {
                        file.delete()
                    }
                }
            }
            directory.delete()
        } catch (e: Exception) {
            false
        }
    }
}