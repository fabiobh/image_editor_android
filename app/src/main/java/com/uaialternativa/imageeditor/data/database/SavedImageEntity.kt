package com.uaialternativa.imageeditor.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_images")
data class SavedImageEntity(
    @PrimaryKey val id: String,
    val fileName: String,
    val filePath: String,
    val originalFileName: String?,
    val width: Int,
    val height: Int,
    val fileSize: Long,
    val createdAt: Long,
    val modifiedAt: Long,
    val appliedOperationsJson: String // Serialized list of operations
)