package com.uaialternativa.imageeditor.ui.picker

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.core.content.ContextCompat

/**
 * Handler for managing image picker permissions
 */
class PermissionHandler(private val context: Context) {
    
    companion object {
        /**
         * Get the required permission based on Android version
         */
        fun getRequiredPermission(): String {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Manifest.permission.READ_MEDIA_IMAGES
            } else {
                Manifest.permission.READ_EXTERNAL_STORAGE
            }
        }
    }
    
    /**
     * Check if the required permission is granted
     */
    fun hasImagePickerPermission(): Boolean {
        val permission = getRequiredPermission()
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }
    
    /**
     * Request the required permission
     */
    fun requestImagePickerPermission(launcher: ManagedActivityResultLauncher<String, Boolean>) {
        val permission = getRequiredPermission()
        launcher.launch(permission)
    }
    
    /**
     * Check if we should show rationale for the permission
     */
    fun shouldShowPermissionRationale(activity: androidx.activity.ComponentActivity): Boolean {
        val permission = getRequiredPermission()
        return activity.shouldShowRequestPermissionRationale(permission)
    }
}