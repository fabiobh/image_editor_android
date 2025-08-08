package com.uaialternativa.imageeditor.ui.editor.crop

import android.graphics.Rect
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Composable that provides a crop overlay with draggable handles for image cropping
 */
@Composable
fun CropOverlay(
    imageSize: IntSize,
    initialCropBounds: Rect? = null,
    onCropBoundsChanged: (Rect) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    
    // Initialize crop bounds to center of image if not provided
    val defaultCropBounds = remember(imageSize) {
        val width = imageSize.width / 2
        val height = imageSize.height / 2
        val left = imageSize.width / 4
        val top = imageSize.height / 4
        Rect(left, top, left + width, top + height)
    }
    
    var cropBounds by remember(initialCropBounds, imageSize) {
        mutableStateOf(initialCropBounds ?: defaultCropBounds)
    }
    
    // Initialize crop bounds if not provided
    if (initialCropBounds == null) {
        onCropBoundsChanged(defaultCropBounds)
    }
    
    // Convert image coordinates to screen coordinates
    fun imageToScreen(imageCoord: Int, imageSize: Int, screenSize: Int): Float {
        return (imageCoord.toFloat() / imageSize.toFloat()) * screenSize.toFloat()
    }
    
    // Convert screen coordinates to image coordinates
    fun screenToImage(screenCoord: Float, imageSize: Int, screenSize: Int): Int {
        return ((screenCoord / screenSize.toFloat()) * imageSize.toFloat()).roundToInt()
    }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned { coordinates ->
                containerSize = coordinates.size
            }
    ) {
        if (containerSize != IntSize.Zero && imageSize != IntSize.Zero) {
            // Calculate scale factor to fit image in container
            val scaleX = containerSize.width.toFloat() / imageSize.width.toFloat()
            val scaleY = containerSize.height.toFloat() / imageSize.height.toFloat()
            val scale = min(scaleX, scaleY)
            
            val scaledImageWidth = (imageSize.width * scale).roundToInt()
            val scaledImageHeight = (imageSize.height * scale).roundToInt()
            
            // Center the scaled image in the container
            val offsetX = (containerSize.width - scaledImageWidth) / 2
            val offsetY = (containerSize.height - scaledImageHeight) / 2
            
            // Convert crop bounds to screen coordinates
            val screenLeft = offsetX + imageToScreen(cropBounds.left, imageSize.width, scaledImageWidth)
            val screenTop = offsetY + imageToScreen(cropBounds.top, imageSize.height, scaledImageHeight)
            val screenRight = offsetX + imageToScreen(cropBounds.right, imageSize.width, scaledImageWidth)
            val screenBottom = offsetY + imageToScreen(cropBounds.bottom, imageSize.height, scaledImageHeight)
            
            // Draw crop overlay
            Canvas(
                modifier = Modifier.fillMaxSize()
            ) {
                drawCropOverlay(
                    cropLeft = screenLeft,
                    cropTop = screenTop,
                    cropRight = screenRight,
                    cropBottom = screenBottom,
                    containerSize = size
                )
            }
            
            // Handle size for draggable corners
            val handleSize = with(density) { 24.dp.toPx() }
            val handleSizeDp = 24.dp
            
            // Top-left handle
            CropHandle(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            (screenLeft - handleSize / 2).roundToInt(),
                            (screenTop - handleSize / 2).roundToInt()
                        )
                    }
                    .size(handleSizeDp)
                    .pointerInput(Unit) {
                        detectDragGestures { _, dragAmount ->
                            val newLeft = screenToImage(
                                screenLeft + dragAmount.x - offsetX,
                                imageSize.width,
                                scaledImageWidth
                            ).coerceIn(0, cropBounds.right - 50)
                            
                            val newTop = screenToImage(
                                screenTop + dragAmount.y - offsetY,
                                imageSize.height,
                                scaledImageHeight
                            ).coerceIn(0, cropBounds.bottom - 50)
                            
                            val newBounds = Rect(newLeft, newTop, cropBounds.right, cropBounds.bottom)
                            cropBounds = newBounds
                            onCropBoundsChanged(newBounds)
                        }
                    }
            )
            
            // Top-right handle
            CropHandle(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            (screenRight - handleSize / 2).roundToInt(),
                            (screenTop - handleSize / 2).roundToInt()
                        )
                    }
                    .size(handleSizeDp)
                    .pointerInput(Unit) {
                        detectDragGestures { _, dragAmount ->
                            val newRight = screenToImage(
                                screenRight + dragAmount.x - offsetX,
                                imageSize.width,
                                scaledImageWidth
                            ).coerceIn(cropBounds.left + 50, imageSize.width)
                            
                            val newTop = screenToImage(
                                screenTop + dragAmount.y - offsetY,
                                imageSize.height,
                                scaledImageHeight
                            ).coerceIn(0, cropBounds.bottom - 50)
                            
                            val newBounds = Rect(cropBounds.left, newTop, newRight, cropBounds.bottom)
                            cropBounds = newBounds
                            onCropBoundsChanged(newBounds)
                        }
                    }
            )
            
            // Bottom-left handle
            CropHandle(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            (screenLeft - handleSize / 2).roundToInt(),
                            (screenBottom - handleSize / 2).roundToInt()
                        )
                    }
                    .size(handleSizeDp)
                    .pointerInput(Unit) {
                        detectDragGestures { _, dragAmount ->
                            val newLeft = screenToImage(
                                screenLeft + dragAmount.x - offsetX,
                                imageSize.width,
                                scaledImageWidth
                            ).coerceIn(0, cropBounds.right - 50)
                            
                            val newBottom = screenToImage(
                                screenBottom + dragAmount.y - offsetY,
                                imageSize.height,
                                scaledImageHeight
                            ).coerceIn(cropBounds.top + 50, imageSize.height)
                            
                            val newBounds = Rect(newLeft, cropBounds.top, cropBounds.right, newBottom)
                            cropBounds = newBounds
                            onCropBoundsChanged(newBounds)
                        }
                    }
            )
            
            // Bottom-right handle
            CropHandle(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            (screenRight - handleSize / 2).roundToInt(),
                            (screenBottom - handleSize / 2).roundToInt()
                        )
                    }
                    .size(handleSizeDp)
                    .pointerInput(Unit) {
                        detectDragGestures { _, dragAmount ->
                            val newRight = screenToImage(
                                screenRight + dragAmount.x - offsetX,
                                imageSize.width,
                                scaledImageWidth
                            ).coerceIn(cropBounds.left + 50, imageSize.width)
                            
                            val newBottom = screenToImage(
                                screenBottom + dragAmount.y - offsetY,
                                imageSize.height,
                                scaledImageHeight
                            ).coerceIn(cropBounds.top + 50, imageSize.height)
                            
                            val newBounds = Rect(cropBounds.left, cropBounds.top, newRight, newBottom)
                            cropBounds = newBounds
                            onCropBoundsChanged(newBounds)
                        }
                    }
            )
            
            // Center drag handle for moving the entire crop area
            CropHandle(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            ((screenLeft + screenRight) / 2 - handleSize / 2).roundToInt(),
                            ((screenTop + screenBottom) / 2 - handleSize / 2).roundToInt()
                        )
                    }
                    .size(handleSizeDp)
                    .pointerInput(Unit) {
                        detectDragGestures { _, dragAmount ->
                            val deltaX = screenToImage(
                                dragAmount.x,
                                imageSize.width,
                                scaledImageWidth
                            )
                            val deltaY = screenToImage(
                                dragAmount.y,
                                imageSize.height,
                                scaledImageHeight
                            )
                            
                            val cropWidth = cropBounds.width()
                            val cropHeight = cropBounds.height()
                            
                            val newLeft = (cropBounds.left + deltaX).coerceIn(
                                0,
                                imageSize.width - cropWidth
                            )
                            val newTop = (cropBounds.top + deltaY).coerceIn(
                                0,
                                imageSize.height - cropHeight
                            )
                            
                            val newBounds = Rect(
                                newLeft,
                                newTop,
                                newLeft + cropWidth,
                                newTop + cropHeight
                            )
                            cropBounds = newBounds
                            onCropBoundsChanged(newBounds)
                        }
                    },
                isCenter = true
            )
        }
    }
}

/**
 * Individual draggable handle for crop corners and center
 */
@Composable
private fun CropHandle(
    modifier: Modifier = Modifier,
    isCenter: Boolean = false
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(
                if (isCenter) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                } else {
                    MaterialTheme.colorScheme.primary
                }
            )
    )
}

/**
 * Draws the crop overlay with dimmed areas outside the crop bounds
 */
private fun DrawScope.drawCropOverlay(
    cropLeft: Float,
    cropTop: Float,
    cropRight: Float,
    cropBottom: Float,
    containerSize: Size
) {
    val overlayColor = Color.Black.copy(alpha = 0.5f)
    val strokeColor = Color.White
    val strokeWidth = 2.dp.toPx()
    
    // Draw dimmed overlay outside crop area
    // Top area
    drawRect(
        color = overlayColor,
        topLeft = Offset(0f, 0f),
        size = Size(containerSize.width, cropTop)
    )
    
    // Bottom area
    drawRect(
        color = overlayColor,
        topLeft = Offset(0f, cropBottom),
        size = Size(containerSize.width, containerSize.height - cropBottom)
    )
    
    // Left area
    drawRect(
        color = overlayColor,
        topLeft = Offset(0f, cropTop),
        size = Size(cropLeft, cropBottom - cropTop)
    )
    
    // Right area
    drawRect(
        color = overlayColor,
        topLeft = Offset(cropRight, cropTop),
        size = Size(containerSize.width - cropRight, cropBottom - cropTop)
    )
    
    // Draw crop bounds rectangle
    drawRect(
        color = strokeColor,
        topLeft = Offset(cropLeft, cropTop),
        size = Size(cropRight - cropLeft, cropBottom - cropTop),
        style = Stroke(width = strokeWidth)
    )
    
    // Draw rule of thirds grid lines
    val cropWidth = cropRight - cropLeft
    val cropHeight = cropBottom - cropTop
    val gridStrokeWidth = 1.dp.toPx()
    val gridColor = strokeColor.copy(alpha = 0.6f)
    
    // Vertical grid lines
    drawLine(
        color = gridColor,
        start = Offset(cropLeft + cropWidth / 3, cropTop),
        end = Offset(cropLeft + cropWidth / 3, cropBottom),
        strokeWidth = gridStrokeWidth
    )
    drawLine(
        color = gridColor,
        start = Offset(cropLeft + 2 * cropWidth / 3, cropTop),
        end = Offset(cropLeft + 2 * cropWidth / 3, cropBottom),
        strokeWidth = gridStrokeWidth
    )
    
    // Horizontal grid lines
    drawLine(
        color = gridColor,
        start = Offset(cropLeft, cropTop + cropHeight / 3),
        end = Offset(cropRight, cropTop + cropHeight / 3),
        strokeWidth = gridStrokeWidth
    )
    drawLine(
        color = gridColor,
        start = Offset(cropLeft, cropTop + 2 * cropHeight / 3),
        end = Offset(cropRight, cropTop + 2 * cropHeight / 3),
        strokeWidth = gridStrokeWidth
    )
}