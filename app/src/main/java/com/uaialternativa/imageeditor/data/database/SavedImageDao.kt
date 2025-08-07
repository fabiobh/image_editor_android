package com.uaialternativa.imageeditor.data.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedImageDao {
    @Query("SELECT * FROM saved_images ORDER BY modifiedAt DESC")
    fun getAllImages(): Flow<List<SavedImageEntity>>
    
    @Insert
    suspend fun insertImage(image: SavedImageEntity)
    
    @Delete
    suspend fun deleteImage(image: SavedImageEntity)
    
    @Query("DELETE FROM saved_images WHERE id = :imageId")
    suspend fun deleteImageById(imageId: String)
    
    @Query("SELECT * FROM saved_images WHERE id = :imageId")
    suspend fun getImageById(imageId: String): SavedImageEntity?
}