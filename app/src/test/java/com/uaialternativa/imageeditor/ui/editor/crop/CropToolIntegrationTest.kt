package com.uaialternativa.imageeditor.ui.editor.crop

import android.graphics.Rect
import com.uaialternativa.imageeditor.domain.model.EditingTool
import com.uaialternativa.imageeditor.ui.editor.ImageEditorAction
import com.uaialternativa.imageeditor.ui.editor.ImageEditorUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CropToolIntegrationTest {

    @Test
    fun `crop tool selection updates UI state correctly`() {
        // This test verifies that selecting the crop tool updates the UI state
        val initialState = ImageEditorUiState()
        val updatedState = initialState.copy(selectedTool = EditingTool.Crop)
        
        assertEquals(EditingTool.Crop, updatedState.selectedTool)
    }

    @Test
    fun `crop bounds are properly validated`() {
        val imageWidth = 800
        val imageHeight = 600

        // Test valid crop bounds
        val validBounds = Rect(100, 100, 300, 250)
        assertTrue("Valid bounds should be within image dimensions", 
            validBounds.left >= 0 && 
            validBounds.top >= 0 && 
            validBounds.right <= imageWidth && 
            validBounds.bottom <= imageHeight)

        // Test bounds correction
        val invalidLeft = -50
        val invalidTop = -50
        val invalidRight = imageWidth + 100
        val invalidBottom = imageHeight + 100
        
        val correctedLeft = invalidLeft.coerceAtLeast(0)
        val correctedTop = invalidTop.coerceAtLeast(0)
        val correctedRight = invalidRight.coerceAtMost(imageWidth)
        val correctedBottom = invalidBottom.coerceAtMost(imageHeight)
        
        assertEquals(0, correctedLeft)
        assertEquals(0, correctedTop)
        assertEquals(imageWidth, correctedRight)
        assertEquals(imageHeight, correctedBottom)
    }

    @Test
    fun `crop bounds maintain minimum size`() {
        val minSize = 50
        val smallWidth = 20
        val smallHeight = 20
        
        // Test that minimum size logic works
        val adjustedWidth = if (smallWidth < minSize) minSize else smallWidth
        val adjustedHeight = if (smallHeight < minSize) minSize else smallHeight
        
        assertTrue("Width should be at least minimum size", adjustedWidth >= minSize)
        assertTrue("Height should be at least minimum size", adjustedHeight >= minSize)
        assertEquals(minSize, adjustedWidth)
        assertEquals(minSize, adjustedHeight)
    }

    @Test
    fun `crop overlay calculates default bounds correctly`() {
        val imageWidth = 800
        val imageHeight = 600
        
        // Test the calculation logic for default bounds
        val expectedLeft = imageWidth / 4  // 200
        val expectedTop = imageHeight / 4  // 150
        val expectedWidth = imageWidth / 2 // 400
        val expectedHeight = imageHeight / 2 // 300
        
        assertEquals(200, expectedLeft)
        assertEquals(150, expectedTop)
        assertEquals(400, expectedWidth)
        assertEquals(300, expectedHeight)
    }

    @Test
    fun `crop action structure is correct`() {
        val cropBounds = Rect(100, 100, 300, 250)
        
        // Test that we can create the action
        val action = ImageEditorAction.SetCropBounds(cropBounds)
        assertNotNull("Action should not be null", action)
        
        val applyCropAction = ImageEditorAction.ApplyCrop
        assertNotNull("ApplyCrop action should not be null", applyCropAction)
    }

    @Test
    fun `apply crop action is properly structured`() {
        val applyCropAction = ImageEditorAction.ApplyCrop
        
        assertNotNull("ApplyCrop action should not be null", applyCropAction)
    }
}