package com.uaialternativa.imageeditor.data.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for SavedImage operations
 */
@Dao
interface SavedImageDao {
    
    /**
     * Get all saved images ordered by modification date (newest first)
     */
    @Query("SELECT * FROM saved_images ORDER BY modifiedAt DESC")
    fun getAllImages(): Flow<List<SavedImageEntity>>
    
    /**
     * Get a specific image by ID
     */
    @Query("SELECT * FROM saved_images WHERE id = :imageId")
    suspend fun getImageById(imageId: String): SavedImageEntity?
    
    /**
     * Get images created within a date range
     */
    @Query("SELECT * FROM saved_images WHERE createdAt BETWEEN :startDate AND :endDate ORDER BY modifiedAt DESC")
    fun getImagesByDateRange(startDate: Long, endDate: Long): Flow<List<SavedImageEntity>>
    
    /**
     * Search images by filename
     */
    @Query("SELECT * FROM saved_images WHERE fileName LIKE '%' || :query || '%' OR originalFileName LIKE '%' || :query || '%' ORDER BY modifiedAt DESC")
    fun searchImages(query: String): Flow<List<SavedImageEntity>>
    
    /**
     * Get total count of saved images
     */
    @Query("SELECT COUNT(*) FROM saved_images")
    suspend fun getImageCount(): Int
    
    /**
     * Get total file size of all saved images
     */
    @Query("SELECT SUM(fileSize) FROM saved_images")
    suspend fun getTotalFileSize(): Long?
    
    /**
     * Insert a new image (replace if exists)
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertImage(image: SavedImageEntity)
    
    /**
     * Insert multiple images
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertImages(images: List<SavedImageEntity>)
    
    /**
     * Update an existing image
     */
    @Update
    suspend fun updateImage(image: SavedImageEntity)
    
    /**
     * Delete a specific image
     */
    @Delete
    suspend fun deleteImage(image: SavedImageEntity)
    
    /**
     * Delete image by ID
     */
    @Query("DELETE FROM saved_images WHERE id = :imageId")
    suspend fun deleteImageById(imageId: String): Int
    
    /**
     * Delete multiple images by IDs
     */
    @Query("DELETE FROM saved_images WHERE id IN (:imageIds)")
    suspend fun deleteImagesByIds(imageIds: List<String>): Int
    
    /**
     * Delete all images
     */
    @Query("DELETE FROM saved_images")
    suspend fun deleteAllImages()
    
    /**
     * Delete images older than specified date
     */
    @Query("DELETE FROM saved_images WHERE createdAt < :cutoffDate")
    suspend fun deleteImagesOlderThan(cutoffDate: Long): Int
}