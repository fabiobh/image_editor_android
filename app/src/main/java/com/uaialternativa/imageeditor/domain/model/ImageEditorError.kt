package com.uaialternativa.imageeditor.domain.model

/**
 * Sealed class representing different types of errors that can occur in the image editor
 */
sealed class ImageEditorError : Exception() {
    /**
     * Error when device runs out of memory during image processing
     */
    object InsufficientMemory : ImageEditorError() {
        override val message: String = "Insufficient memory to process the image"
    }
    
    /**
     * Error when storage permission is denied
     */
    object StoragePermissionDenied : ImageEditorError() {
        override val message: String = "Storage permission is required to save images"
    }
    
    /**
     * Error when image format is not supported
     */
    object UnsupportedImageFormat : ImageEditorError() {
        override val message: String = "The selected image format is not supported"
    }
    
    /**
     * Error when image processing operation fails
     */
    data class ProcessingFailed(val operation: String, override val cause: Throwable? = null) : ImageEditorError() {
        override val message: String = "Failed to process image: $operation"
    }
    
    /**
     * Error when database operation fails
     */
    data class DatabaseError(override val message: String, override val cause: Throwable? = null) : ImageEditorError()
    
    /**
     * Error when file system operation fails
     */
    data class FileSystemError(override val message: String, override val cause: Throwable? = null) : ImageEditorError()
    
    /**
     * Error when image loading fails
     */
    data class ImageLoadingError(override val message: String, override val cause: Throwable? = null) : ImageEditorError()
    
    /**
     * Error when image saving fails
     */
    data class ImageSavingError(override val message: String, override val cause: Throwable? = null) : ImageEditorError()
    
    /**
     * Generic error for unexpected situations
     */
    data class UnknownError(override val message: String, override val cause: Throwable? = null) : ImageEditorError()
}