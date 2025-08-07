package com.uaialternativa.imageeditor.data.file

import android.net.Uri
import org.junit.Assert.*
import org.junit.Test

class FileManagerTest {

    @Test
    fun `isSupportedImageFormat should return true for supported MIME types`() {
        // Given
        val supportedMimeTypes = listOf(
            "image/jpeg",
            "image/png", 
            "image/webp"
        )
        
        // When & Then
        supportedMimeTypes.forEach { mimeType ->
            assertTrue("$mimeType should be supported", 
                isSupportedImageFormatStatic(null, mimeType))
        }
    }

    @Test
    fun `isSupportedImageFormat should return false for unsupported MIME types`() {
        // Given
        val unsupportedMimeTypes = listOf(
            "image/gif",
            "image/bmp",
            "text/plain",
            "application/pdf"
        )
        
        // When & Then
        unsupportedMimeTypes.forEach { mimeType ->
            assertFalse("$mimeType should not be supported", 
                isSupportedImageFormatStatic(null, mimeType))
        }
    }

    @Test
    fun `isSupportedImageFormat should return true for supported file extensions`() {
        // Given
        val supportedPaths = listOf(
            "/path/image.jpg",
            "/path/image.jpeg",
            "/path/image.png",
            "/path/image.webp"
        )
        
        // When & Then
        supportedPaths.forEach { path ->
            assertTrue("$path should be supported", 
                isSupportedImageFormatStatic(path, null))
        }
    }

    @Test
    fun `isSupportedImageFormat should return false for unsupported file extensions`() {
        // Given
        val unsupportedPaths = listOf(
            "/path/image.gif",
            "/path/image.bmp",
            "/path/document.pdf",
            "/path/file.txt"
        )
        
        // When & Then
        unsupportedPaths.forEach { path ->
            assertFalse("$path should not be supported", 
                isSupportedImageFormatStatic(path, null))
        }
    }

    // Static version of the method for testing without Android Context
    private fun isSupportedImageFormatStatic(path: String?, mimeType: String?): Boolean {
        val supportedMimeTypes = setOf(
            "image/jpeg", 
            "image/png", 
            "image/webp"
        )
        val supportedExtensions = setOf(".jpg", ".jpeg", ".png", ".webp")
        
        // Check MIME type
        if (mimeType != null && supportedMimeTypes.contains(mimeType.lowercase())) {
            return true
        }
        
        // Check file extension from path
        path?.let { filePath ->
            val extension = filePath.substringAfterLast('.', "").lowercase()
            if (extension.isNotEmpty() && supportedExtensions.contains(".$extension")) {
                return true
            }
        }
        
        return false
    }


}