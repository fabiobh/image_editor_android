package com.uaialternativa.imageeditor.data.repository

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.uaialternativa.imageeditor.data.database.SavedImageDao
import com.uaialternativa.imageeditor.data.database.SavedImageEntity
import com.uaialternativa.imageeditor.data.file.FileManager
import com.uaialternativa.imageeditor.domain.model.ImageEditorError
import com.uaialternativa.imageeditor.domain.model.SavedImage
import com.uaialternativa.imageeditor.domain.repository.ImageMetadata
import com.uaialternativa.imageeditor.domain.repository.ImageRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of ImageRepository that combines database and file operations
 * Handles image storage, metadata management, and proper error handling
 */
@Singleton
class ImageRepositoryImpl @Inject constructor(
    private val savedImageDao: SavedImageDao,
    private val fileManager: FileManager
) : ImageRepository {

    override suspend fun getSavedImages(): Flow<List<SavedImage>> {
        return savedImageDao.getAllImages()
            .map { entities -> entities.map { it.toDomainModel() } }
            .catch { exception ->
                throw ImageEditorError.DatabaseError(
                    message = "Failed to retrieve saved images",
                    cause = exception
                )
            }
    }

    override suspend fun saveImage(image: Bitmap, metadata: ImageMetadata): Result<SavedImage> {
        return try {
            // Generate unique ID for the image
            val imageId = UUID.randomUUID().toString()
            val currentTime = System.currentTimeMillis()
            
            // Save bitmap to file system
            val fileResult = fileManager.saveBitmap(image)
            if (fileResult.isFailure) {
                return Result.failure(
                    ImageEditorError.ImageSavingError(
                        message = "Failed to save image to file system",
                        cause = fileResult.exceptionOrNull()
                    )
                )
            }
            
            val savedFile = fileResult.getOrThrow()
            
            // Extract file metadata
            val fileSize = savedFile.length()
            val fileName = savedFile.name
            val filePath = savedFile.absolutePath
            
            // Create entity for database
            val entity = SavedImageEntity(
                id = imageId,
                fileName = fileName,
                filePath = filePath,
                originalFileName = metadata.originalFileName,
                width = metadata.width,
                height = metadata.height,
                fileSize = fileSize,
                createdAt = currentTime,
                modifiedAt = currentTime,
                appliedOperations = metadata.appliedOperations
            )
            
            // Save to database
            try {
                savedImageDao.insertImage(entity)
            } catch (exception: Exception) {
                // If database save fails, clean up the file
                fileManager.deleteFile(filePath)
                return Result.failure(
                    ImageEditorError.DatabaseError(
                        message = "Failed to save image metadata to database",
                        cause = exception
                    )
                )
            }
            
            // Return success with domain model
            Result.success(entity.toDomainModel())
            
        } catch (exception: Exception) {
            Result.failure(
                ImageEditorError.ImageSavingError(
                    message = "Unexpected error while saving image",
                    cause = exception
                )
            )
        }
    }

    override suspend fun deleteImage(imageId: String): Result<Unit> {
        return try {
            // First get the image entity to get file path
            val entity = savedImageDao.getImageById(imageId)
            if (entity == null) {
                return Result.failure(
                    ImageEditorError.DatabaseError(
                        message = "Image with ID $imageId not found"
                    )
                )
            }
            
            // Delete from database first
            val deletedRows = savedImageDao.deleteImageById(imageId)
            if (deletedRows == 0) {
                return Result.failure(
                    ImageEditorError.DatabaseError(
                        message = "Failed to delete image from database"
                    )
                )
            }
            
            // Delete file from file system
            val fileDeleteResult = fileManager.deleteFile(entity.filePath)
            if (fileDeleteResult.isFailure) {
                // Log warning but don't fail the operation since database deletion succeeded
                // The file might have been manually deleted or corrupted
                // TODO: Add proper logging here
            }
            
            Result.success(Unit)
            
        } catch (exception: Exception) {
            Result.failure(
                ImageEditorError.DatabaseError(
                    message = "Failed to delete image",
                    cause = exception
                )
            )
        }
    }

    override suspend fun getImageFile(imagePath: String): Result<File> {
        return fileManager.getFile(imagePath)
            .recoverCatching { exception ->
                throw when (exception) {
                    is ImageEditorError -> exception
                    else -> ImageEditorError.FileSystemError(
                        message = "Failed to get image file: $imagePath",
                        cause = exception
                    )
                }
            }
    }

    /**
     * Additional method to get image by ID with file validation
     */
    suspend fun getImageById(imageId: String): Result<SavedImage> {
        return try {
            val entity = savedImageDao.getImageById(imageId)
            if (entity == null) {
                Result.failure(
                    ImageEditorError.DatabaseError(
                        message = "Image with ID $imageId not found"
                    )
                )
            } else {
                // Validate that the file still exists
                val fileExists = fileManager.isValidFile(entity.filePath)
                if (!fileExists) {
                    // File is missing, remove from database
                    savedImageDao.deleteImageById(imageId)
                    Result.failure(
                        ImageEditorError.FileSystemError(
                            message = "Image file is missing and has been removed from database"
                        )
                    )
                } else {
                    Result.success(entity.toDomainModel())
                }
            }
        } catch (exception: Exception) {
            Result.failure(
                ImageEditorError.DatabaseError(
                    message = "Failed to get image by ID",
                    cause = exception
                )
            )
        }
    }

    /**
     * Additional method to clean up orphaned files and database entries
     */
    suspend fun cleanupOrphanedData(): Result<CleanupResult> {
        return try {
            var orphanedFiles = 0
            var orphanedEntries = 0
            
            // Get all files and database entries
            val filesResult = fileManager.getAllEditedImageFiles()
            val allEntities = mutableListOf<SavedImageEntity>()
            
            // Collect all entities (we need to convert Flow to list)
            savedImageDao.getAllImages().collect { entities ->
                allEntities.clear()
                allEntities.addAll(entities)
            }
            
            if (filesResult.isSuccess) {
                val files = filesResult.getOrThrow()
                val entityPaths = allEntities.map { it.filePath }.toSet()
                
                // Find orphaned files (files without database entries)
                files.forEach { file ->
                    if (file.absolutePath !in entityPaths) {
                        fileManager.deleteFile(file.absolutePath)
                        orphanedFiles++
                    }
                }
            }
            
            // Find orphaned database entries (entries without files)
            allEntities.forEach { entity ->
                if (!fileManager.isValidFile(entity.filePath)) {
                    savedImageDao.deleteImageById(entity.id)
                    orphanedEntries++
                }
            }
            
            Result.success(CleanupResult(orphanedFiles, orphanedEntries))
            
        } catch (exception: Exception) {
            Result.failure(
                ImageEditorError.UnknownError(
                    message = "Failed to cleanup orphaned data",
                    cause = exception
                )
            )
        }
    }

    /**
     * Validates if a file is a valid image by attempting to decode its dimensions
     */
    private fun isValidImageFile(file: File): Boolean {
        return try {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(file.absolutePath, options)
            options.outWidth > 0 && options.outHeight > 0
        } catch (exception: Exception) {
            false
        }
    }

    /**
     * Extension function to convert SavedImageEntity to SavedImage domain model
     */
    private fun SavedImageEntity.toDomainModel(): SavedImage {
        return SavedImage(
            id = id,
            fileName = fileName,
            filePath = filePath,
            originalFileName = originalFileName,
            width = width,
            height = height,
            fileSize = fileSize,
            createdAt = createdAt,
            modifiedAt = modifiedAt,
            appliedOperations = appliedOperations
        )
    }

    /**
     * Data class for cleanup operation results
     */
    data class CleanupResult(
        val orphanedFilesRemoved: Int,
        val orphanedEntriesRemoved: Int
    )
}