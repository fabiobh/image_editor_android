package com.uaialternativa.imageeditor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.uaialternativa.imageeditor.ui.gallery.GalleryScreen
import com.uaialternativa.imageeditor.ui.theme.ImageEditorTheme
import dagger.hilt.android.AndroidEntryPoint

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
    GalleryScreen(
        onImageSelected = { savedImage ->
            // TODO: Navigate to image editor screen
            // This will be implemented in a future task
        },
        onAddImageClicked = {
            // TODO: Open image picker
            // This will be implemented in a future task
        },
        modifier = Modifier.fillMaxSize()
    )
}

@Preview(showBackground = true)
@Composable
fun ImageEditorAppPreview() {
    ImageEditorTheme {
        ImageEditorApp()
    }
}