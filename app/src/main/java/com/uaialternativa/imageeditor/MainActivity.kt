package com.uaialternativa.imageeditor

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import com.uaialternativa.imageeditor.R
import com.uaialternativa.imageeditor.ui.editor.ImageEditorScreen
import com.uaialternativa.imageeditor.ui.editor.ImageEditorViewModel
import com.uaialternativa.imageeditor.ui.gallery.GalleryScreen
import com.uaialternativa.imageeditor.ui.picker.ImagePickerManager
import com.uaialternativa.imageeditor.ui.picker.ImagePickerResult
import com.uaialternativa.imageeditor.ui.picker.PermissionHandler
import com.uaialternativa.imageeditor.ui.theme.ImageEditorTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ImageEditorTheme {
                ImageEditorApp()
            }
        }
    }
}

@Composable
fun ImageEditorApp() {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Gallery) }
    var isImagePickerLoading by remember { mutableStateOf(false) }
    var imagePickerError by remember { mutableStateOf<String?>(null) }
    var showPermissionDialog by remember { mutableStateOf(false) }
    var showPermissionDeniedDialog by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val imagePickerManager = remember { ImagePickerManager(context) }
    val permissionHandler = remember { PermissionHandler(context) }
    
    // Image picker launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            isImagePickerLoading = true
            imagePickerError = null
            
            coroutineScope.launch {
                when (val result = imagePickerManager.validateAndProcessImage(uri)) {
                    is ImagePickerResult.Success -> {
                        isImagePickerLoading = false
                        currentScreen = Screen.Editor(uri, result.fileName)
                    }
                    is ImagePickerResult.Error -> {
                        isImagePickerLoading = false
                        imagePickerError = result.message
                    }
                    is ImagePickerResult.Cancelled -> {
                        isImagePickerLoading = false
                        // User cancelled, no action needed
                    }
                }
            }
        } else {
            // User cancelled selection
            isImagePickerLoading = false
        }
    }
    
    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Permission granted, launch image picker
            imagePickerManager.launchImagePicker(imagePickerLauncher)
        } else {
            // Permission denied
            showPermissionDeniedDialog = true
        }
    }
    
    // Function to handle image picker launch with permission check
    val launchImagePicker = {
        if (permissionHandler.hasImagePickerPermission()) {
            imagePickerManager.launchImagePicker(imagePickerLauncher)
        } else {
            if (context is ComponentActivity && permissionHandler.shouldShowPermissionRationale(context)) {
                showPermissionDialog = true
            } else {
                permissionHandler.requestImagePickerPermission(permissionLauncher)
            }
        }
    }

    when (val screen = currentScreen) {
        is Screen.Gallery -> {
            GalleryScreen(
                onImageSelected = { savedImage ->
                    // TODO: Navigate to image editor screen with saved image
                    // This will be implemented in a future task
                },
                onAddImageClicked = launchImagePicker,
                isImagePickerLoading = isImagePickerLoading,
                imagePickerError = imagePickerError,
                onImagePickerErrorDismissed = { imagePickerError = null },
                modifier = Modifier.fillMaxSize()
            )
        }
        is Screen.Editor -> {
            val editorViewModel: ImageEditorViewModel = hiltViewModel()
            
            // Load the image when entering editor screen
            androidx.compose.runtime.LaunchedEffect(screen.imageUri) {
                editorViewModel.loadImage(screen.imageUri, screen.fileName)
            }
            
            ImageEditorScreen(
                onNavigateBack = { currentScreen = Screen.Gallery },
                modifier = Modifier.fillMaxSize(),
                viewModel = editorViewModel
            )
        }
    }
    
    // Permission rationale dialog
    if (showPermissionDialog) {
        PermissionRationaleDialog(
            onGrantPermission = {
                showPermissionDialog = false
                permissionHandler.requestImagePickerPermission(permissionLauncher)
            },
            onDismiss = {
                showPermissionDialog = false
            }
        )
    }
    
    // Permission denied dialog
    if (showPermissionDeniedDialog) {
        PermissionDeniedDialog(
            onDismiss = {
                showPermissionDeniedDialog = false
            }
        )
    }
}

/**
 * Dialog shown to explain why permission is needed
 */
@Composable
private fun PermissionRationaleDialog(
    onGrantPermission: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.permission_required_title),
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Text(
                text = stringResource(R.string.permission_required_message),
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            Button(onClick = onGrantPermission) {
                Text(stringResource(R.string.grant_permission))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

/**
 * Dialog shown when permission is denied
 */
@Composable
private fun PermissionDeniedDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.permission_denied_title),
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Text(
                text = stringResource(R.string.permission_denied_message),
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text(stringResource(R.string.ok))
            }
        }
    )
}

/**
 * Sealed class representing different screens in the app
 */
sealed class Screen {
    object Gallery : Screen()
    data class Editor(val imageUri: Uri, val fileName: String?) : Screen()
}

@Preview(showBackground = true)
@Composable
fun ImageEditorAppPreview() {
    ImageEditorTheme {
        ImageEditorApp()
    }
}