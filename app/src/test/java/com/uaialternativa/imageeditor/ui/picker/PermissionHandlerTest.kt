package com.uaialternativa.imageeditor.ui.picker

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.core.content.ContextCompat
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PermissionHandlerTest {

    private lateinit var context: Context
    private lateinit var permissionHandler: PermissionHandler

    @Before
    fun setup() {
        context = mockk()
        permissionHandler = PermissionHandler(context)
        mockkStatic(ContextCompat::class)
        mockkStatic("android.os.Build\$VERSION")
    }

    @After
    fun tearDown() {
        unmockkStatic(ContextCompat::class)
        unmockkStatic("android.os.Build\$VERSION")
    }

    @Test
    fun `getRequiredPermission returns READ_MEDIA_IMAGES for Android 13+`() {
        // Arrange
        every { Build.VERSION.SDK_INT } returns Build.VERSION_CODES.TIRAMISU

        // Act
        val permission = PermissionHandler.getRequiredPermission()

        // Assert
        assertEquals(Manifest.permission.READ_MEDIA_IMAGES, permission)
    }

    @Test
    fun `getRequiredPermission returns READ_EXTERNAL_STORAGE for Android 12 and below`() {
        // Arrange
        every { Build.VERSION.SDK_INT } returns Build.VERSION_CODES.S

        // Act
        val permission = PermissionHandler.getRequiredPermission()

        // Assert
        assertEquals(Manifest.permission.READ_EXTERNAL_STORAGE, permission)
    }

    @Test
    fun `hasImagePickerPermission returns true when permission is granted`() {
        // Arrange
        every { Build.VERSION.SDK_INT } returns Build.VERSION_CODES.TIRAMISU
        every { 
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES) 
        } returns PackageManager.PERMISSION_GRANTED

        // Act
        val hasPermission = permissionHandler.hasImagePickerPermission()

        // Assert
        assertTrue(hasPermission)
    }

    @Test
    fun `hasImagePickerPermission returns false when permission is denied`() {
        // Arrange
        every { Build.VERSION.SDK_INT } returns Build.VERSION_CODES.TIRAMISU
        every { 
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES) 
        } returns PackageManager.PERMISSION_DENIED

        // Act
        val hasPermission = permissionHandler.hasImagePickerPermission()

        // Assert
        assertFalse(hasPermission)
    }

    @Test
    fun `shouldShowPermissionRationale returns true when rationale should be shown`() {
        // Arrange
        val activity = mockk<ComponentActivity>()
        every { Build.VERSION.SDK_INT } returns Build.VERSION_CODES.TIRAMISU
        every { 
            activity.shouldShowRequestPermissionRationale(Manifest.permission.READ_MEDIA_IMAGES) 
        } returns true

        // Act
        val shouldShow = permissionHandler.shouldShowPermissionRationale(activity)

        // Assert
        assertTrue(shouldShow)
    }

    @Test
    fun `shouldShowPermissionRationale returns false when rationale should not be shown`() {
        // Arrange
        val activity = mockk<ComponentActivity>()
        every { Build.VERSION.SDK_INT } returns Build.VERSION_CODES.TIRAMISU
        every { 
            activity.shouldShowRequestPermissionRationale(Manifest.permission.READ_MEDIA_IMAGES) 
        } returns false

        // Act
        val shouldShow = permissionHandler.shouldShowPermissionRationale(activity)

        // Assert
        assertFalse(shouldShow)
    }
}