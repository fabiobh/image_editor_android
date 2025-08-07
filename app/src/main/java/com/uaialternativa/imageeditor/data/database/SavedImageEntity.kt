package com.uaialternativa.imageeditor.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.uaialternativa.imageeditor.domain.model.ImageOperation

/**
 * Room entity representing a saved edited image with metadata
 */
@Entity(tableName = "saved_images")
@TypeConverters(Converters::class)
data class SavedImageEntity(
    @PrimaryKey 
    val id: String,
    val fileName: String,
    val filePath: String,
    val originalFileName: String?,
    val width: Int,
    val height: Int,
    val fileSize: Long,
    val createdAt: Long,
    val modifiedAt: Long,
    val appliedOperations: List<ImageOperation> = emptyList()
)