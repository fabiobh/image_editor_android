package com.uaialternativa.imageeditor.ui.editor.crop

import android.graphics.Rect
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CropOverlayTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun cropOverlay_displaysCorrectly() {
        var cropBounds: Rect? = null
        val imageSize = IntSize(800, 600)
        
        composeTestRule.setContent {
            Box(modifier = Modifier.size(400.dp, 300.dp)) {
                CropOverlay(
                    imageSize = imageSize,
                    initialCropBounds = null,
                    onCropBoundsChanged = { bounds ->
                        cropBounds = bounds
                    }
                )
            }
        }

        // Verify the crop overlay is displayed
        // Note: Since we don't have specific content descriptions for handles,
        // we'll verify the component renders without crashing
        composeTestRule.waitForIdle()
        
        // Verify that crop bounds are initialized
        assert(cropBounds != null)
        assert(cropBounds!!.width() > 0)
        assert(cropBounds!!.height() > 0)
    }

    @Test
    fun cropOverlay_initializesWithDefaultBounds() {
        var receivedBounds: Rect? = null
        val imageSize = IntSize(800, 600)
        
        composeTestRule.setContent {
            Box(modifier = Modifier.size(400.dp, 300.dp)) {
                CropOverlay(
                    imageSize = imageSize,
                    initialCropBounds = null,
                    onCropBoundsChanged = { bounds ->
                        receivedBounds = bounds
                    }
                )
            }
        }

        composeTestRule.waitForIdle()
        
        // Verify default bounds are set (center quarter of image)
        assert(receivedBounds != null)
        val bounds = receivedBounds!!
        
        // Default bounds should be centered and half the size
        assert(bounds.left == imageSize.width / 4)
        assert(bounds.top == imageSize.height / 4)
        assert(bounds.width() == imageSize.width / 2)
        assert(bounds.height() == imageSize.height / 2)
    }

    @Test
    fun cropOverlay_usesProvidedInitialBounds() {
        var receivedBounds: Rect? = null
        val imageSize = IntSize(800, 600)
        val initialBounds = Rect(100, 100, 300, 250)
        
        composeTestRule.setContent {
            Box(modifier = Modifier.size(400.dp, 300.dp)) {
                CropOverlay(
                    imageSize = imageSize,
                    initialCropBounds = initialBounds,
                    onCropBoundsChanged = { bounds ->
                        receivedBounds = bounds
                    }
                )
            }
        }

        composeTestRule.waitForIdle()
        
        // Verify initial bounds are used
        assert(receivedBounds == initialBounds)
    }

    @Test
    fun cropOverlay_boundsStayWithinImageLimits() {
        var receivedBounds: Rect? = null
        val imageSize = IntSize(800, 600)
        
        composeTestRule.setContent {
            Box(modifier = Modifier.size(400.dp, 300.dp)) {
                CropOverlay(
                    imageSize = imageSize,
                    initialCropBounds = null,
                    onCropBoundsChanged = { bounds ->
                        receivedBounds = bounds
                    }
                )
            }
        }

        composeTestRule.waitForIdle()
        
        // Verify bounds are within image limits
        val bounds = receivedBounds!!
        assert(bounds.left >= 0)
        assert(bounds.top >= 0)
        assert(bounds.right <= imageSize.width)
        assert(bounds.bottom <= imageSize.height)
        assert(bounds.width() > 0)
        assert(bounds.height() > 0)
    }

    @Test
    fun cropOverlay_maintainsMinimumSize() {
        var receivedBounds: Rect? = null
        val imageSize = IntSize(800, 600)
        
        composeTestRule.setContent {
            Box(modifier = Modifier.size(400.dp, 300.dp)) {
                CropOverlay(
                    imageSize = imageSize,
                    initialCropBounds = null,
                    onCropBoundsChanged = { bounds ->
                        receivedBounds = bounds
                    }
                )
            }
        }

        composeTestRule.waitForIdle()
        
        // Verify minimum size is maintained (at least 50px as defined in the implementation)
        val bounds = receivedBounds!!
        assert(bounds.width() >= 50)
        assert(bounds.height() >= 50)
    }
}