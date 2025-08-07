package com.uaialternativa.imageeditor.domain.model

/**
 * Domain model representing a saved edited image with its metadata
 */
data class SavedImage(
    val id: String,
    val fileName: String,
    val filePath: String,
    val originalFileName: String?,
    val width: Int,
    val height: Int,
    val fileSize: Long,
    val createdAt: Long,
    val modifiedAt: Long,
    val appliedOperations: List<ImageOperation>
)