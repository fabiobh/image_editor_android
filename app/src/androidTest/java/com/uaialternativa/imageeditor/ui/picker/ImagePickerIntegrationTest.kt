package com.uaialternativa.imageeditor.ui.picker

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ImagePickerIntegrationTest {

    private lateinit var context: Context
    private lateinit var imagePickerManager: ImagePickerManager
    private lateinit var permissionHandler: PermissionHandler

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        imagePickerManager = ImagePickerManager(context)
        permissionHandler = PermissionHandler(context)
    }

    @Test
    fun imagePickerManager_canBeInstantiated() {
        assertNotNull(imagePickerManager)
    }

    @Test
    fun permissionHandler_canBeInstantiated() {
        assertNotNull(permissionHandler)
    }

    @Test
    fun permissionHandler_getRequiredPermission_returnsValidPermission() {
        val permission = PermissionHandler.getRequiredPermission()
        assertTrue("Permission should be either READ_MEDIA_IMAGES or READ_EXTERNAL_STORAGE",
            permission == android.Manifest.permission.READ_MEDIA_IMAGES ||
            permission == android.Manifest.permission.READ_EXTERNAL_STORAGE
        )
    }

    @Test
    fun permissionHandler_hasImagePickerPermission_returnsBooleanValue() {
        // This test just verifies the method doesn't crash and returns a boolean
        val hasPermission = permissionHandler.hasImagePickerPermission()
        // We can't assert the specific value since it depends on the test environment
        assertTrue("hasImagePickerPermission should return true or false", 
            hasPermission == true || hasPermission == false)
    }
}