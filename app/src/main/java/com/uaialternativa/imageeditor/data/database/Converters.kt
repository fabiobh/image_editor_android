package com.uaialternativa.imageeditor.data.database

import android.graphics.Rect
import androidx.room.TypeConverter
import com.uaialternativa.imageeditor.domain.model.FilterType
import com.uaialternativa.imageeditor.domain.model.ImageOperation
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString

/**
 * Room type converters for complex data types
 */
class Converters {
    
    private val json = Json { 
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    
    @TypeConverter
    fun fromImageOperationList(operations: List<ImageOperation>): String {
        val serializableOperations = operations.map { it.toSerializable() }
        return json.encodeToString(serializableOperations)
    }
    
    @TypeConverter
    fun toImageOperationList(operationsJson: String): List<ImageOperation> {
        if (operationsJson.isEmpty()) return emptyList()
        return try {
            val serializableOperations = json.decodeFromString<List<SerializableImageOperation>>(operationsJson)
            serializableOperations.map { it.toDomainModel() }
        } catch (e: Exception) {
            emptyList()
        }
    }
}

/**
 * Serializable versions of ImageOperation for JSON conversion
 */
@Serializable
sealed class SerializableImageOperation {
    @Serializable
    data class Crop(
        val left: Int,
        val top: Int,
        val right: Int,
        val bottom: Int
    ) : SerializableImageOperation()
    
    @Serializable
    data class Resize(
        val width: Int,
        val height: Int
    ) : SerializableImageOperation()
    
    @Serializable
    data class Filter(
        @SerialName("filterType")
        val type: String,
        val intensity: Float
    ) : SerializableImageOperation()
}

/**
 * Extension functions for converting between domain models and serializable models
 */
private fun ImageOperation.toSerializable(): SerializableImageOperation {
    return when (this) {
        is ImageOperation.Crop -> SerializableImageOperation.Crop(
            left = bounds.left,
            top = bounds.top,
            right = bounds.right,
            bottom = bounds.bottom
        )
        is ImageOperation.Resize -> SerializableImageOperation.Resize(
            width = width,
            height = height
        )
        is ImageOperation.Filter -> SerializableImageOperation.Filter(
            type = type.name,
            intensity = intensity
        )
    }
}

private fun SerializableImageOperation.toDomainModel(): ImageOperation {
    return when (this) {
        is SerializableImageOperation.Crop -> ImageOperation.Crop(
            bounds = Rect(left, top, right, bottom)
        )
        is SerializableImageOperation.Resize -> ImageOperation.Resize(
            width = width,
            height = height
        )
        is SerializableImageOperation.Filter -> ImageOperation.Filter(
            type = FilterType.valueOf(type),
            intensity = intensity
        )
    }
}