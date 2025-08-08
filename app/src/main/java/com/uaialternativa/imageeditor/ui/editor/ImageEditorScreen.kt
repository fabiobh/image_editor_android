package com.uaialternativa.imageeditor.ui.editor

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetState
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntSize
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.uaialternativa.imageeditor.R
import com.uaialternativa.imageeditor.domain.model.EditingTool
import com.uaialternativa.imageeditor.ui.editor.crop.CropOverlay

/**
 * Main image editor screen with toolbar and tool-specific controls
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageEditorScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ImageEditorViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scaffoldState = rememberBottomSheetScaffoldState()
    var showUnsavedChangesDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // Handle back navigation with unsaved changes check
    BackHandler {
        if (viewModel.hasUnsavedChanges()) {
            showUnsavedChangesDialog = true
        } else {
            onNavigateBack()
        }
    }

    // Show error messages in snackbar
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.handleAction(ImageEditorAction.ClearError)
        }
    }

    // Show save success message and navigate back
    LaunchedEffect(uiState.saveSuccess) {
        if (uiState.saveSuccess) {
            snackbarHostState.showSnackbar(context.getString(R.string.image_saved_successfully))
            viewModel.handleAction(ImageEditorAction.ClearSaveSuccess)
            onNavigateBack()
        }
    }

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        topBar = {
            ImageEditorTopBar(
                onNavigateBack = {
                    if (viewModel.hasUnsavedChanges()) {
                        showUnsavedChangesDialog = true
                    } else {
                        onNavigateBack()
                    }
                },
                canUndo = uiState.canUndo,
                canRedo = uiState.canRedo,
                onUndo = { viewModel.handleAction(ImageEditorAction.Undo) },
                onRedo = { viewModel.handleAction(ImageEditorAction.Redo) }
            )
        },
        sheetContent = {
            ToolControlPanel(
                selectedTool = uiState.selectedTool,
                uiState = uiState,
                onAction = viewModel::handleAction,
                modifier = Modifier.fillMaxWidth()
            )
        },
        sheetPeekHeight = if (uiState.selectedTool != EditingTool.None) 200.dp else 0.dp,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Main image display area
            ImageDisplayArea(
                bitmap = uiState.editedImage,
                isLoading = uiState.isLoading,
                isProcessing = uiState.isProcessing,
                selectedTool = uiState.selectedTool,
                cropBounds = uiState.cropBounds,
                onCropBoundsChanged = { bounds ->
                    viewModel.handleAction(ImageEditorAction.SetCropBounds(bounds))
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(16.dp)
            )

            // Toolbar with editing tools
            EditorToolbar(
                selectedTool = uiState.selectedTool,
                onToolSelected = { tool ->
                    viewModel.handleAction(ImageEditorAction.SelectTool(tool))
                },
                onSave = { viewModel.handleAction(ImageEditorAction.SaveImage) },
                isSaving = uiState.isSaving,
                hasImage = uiState.editedImage != null,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
    }

    // Unsaved changes dialog
    if (showUnsavedChangesDialog) {
        UnsavedChangesDialog(
            onSaveAndExit = {
                showUnsavedChangesDialog = false
                viewModel.handleAction(ImageEditorAction.SaveImage)
            },
            onDiscardAndExit = {
                showUnsavedChangesDialog = false
                onNavigateBack()
            },
            onCancel = {
                showUnsavedChangesDialog = false
            }
        )
    }
}

/**
 * Top app bar with navigation and undo/redo controls
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ImageEditorTopBar(
    onNavigateBack: () -> Unit,
    canUndo: Boolean,
    canRedo: Boolean,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    TopAppBar(
        title = {
            Text(
                text = stringResource(R.string.image_editor_title),
                style = MaterialTheme.typography.headlineSmall
            )
        },
        navigationIcon = {
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier.semantics {
                    contentDescription = context.getString(R.string.navigate_back)
                }
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = stringResource(R.string.navigate_back)
                )
            }
        },
        actions = {
            IconButton(
                onClick = onUndo,
                enabled = canUndo,
                modifier = Modifier.semantics {
                    contentDescription = context.getString(R.string.undo_action)
                }
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowLeft,
                    contentDescription = stringResource(R.string.undo_action),
                    tint = if (canUndo) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    }
                )
            }
            
            IconButton(
                onClick = onRedo,
                enabled = canRedo,
                modifier = Modifier.semantics {
                    contentDescription = context.getString(R.string.redo_action)
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = stringResource(R.string.redo_action),
                    tint = if (canRedo) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    }
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface
        ),
        modifier = modifier
    )
}/**
 * Main image display area with loading and processing states
 */
@Composable
private fun ImageDisplayArea(
    bitmap: Bitmap?,
    isLoading: Boolean,
    isProcessing: Boolean,
    selectedTool: EditingTool,
    cropBounds: android.graphics.Rect?,
    onCropBoundsChanged: (android.graphics.Rect) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            when {
                isLoading -> {
                    LoadingImageState()
                }
                bitmap != null -> {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(bitmap)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Image being edited",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Fit
                    )
                    
                    // Crop overlay when crop tool is selected
                    if (selectedTool == EditingTool.Crop) {
                        CropOverlay(
                            imageSize = IntSize(bitmap.width, bitmap.height),
                            initialCropBounds = cropBounds,
                            onCropBoundsChanged = onCropBoundsChanged,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    
                    // Processing overlay
                    if (isProcessing) {
                        ProcessingOverlay()
                    }
                }
                else -> {
                    EmptyImageState()
                }
            }
        }
    }
}

/**
 * Loading state for image display area
 */
@Composable
private fun LoadingImageState() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        CircularProgressIndicator(
            modifier = Modifier
                .size(48.dp)
                .semantics {
                    contentDescription = "Loading image"
                }
        )
        Text(
            text = stringResource(R.string.loading_image),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Processing overlay shown during image operations
 */
@Composable
private fun ProcessingOverlay() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Color.Black.copy(alpha = 0.6f),
                RoundedCornerShape(12.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(48.dp)
                    .semantics {
                        contentDescription = "Processing image"
                    }
            )
            Text(
                text = stringResource(R.string.processing_image),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White
            )
        }
    }
}

/**
 * Empty state when no image is loaded
 */
@Composable
private fun EmptyImageState() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.padding(32.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Build,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
        Text(
            text = stringResource(R.string.no_image_loaded),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Text(
            text = stringResource(R.string.select_image_to_edit),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Toolbar with editing tools and save button
 */
@Composable
private fun EditorToolbar(
    selectedTool: EditingTool,
    onToolSelected: (EditingTool) -> Unit,
    onSave: () -> Unit,
    isSaving: Boolean,
    hasImage: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Crop tool
            ToolButton(
                icon = Icons.Default.Edit,
                label = "Crop",
                isSelected = selectedTool == EditingTool.Crop,
                enabled = hasImage,
                onClick = { 
                    onToolSelected(
                        if (selectedTool == EditingTool.Crop) EditingTool.None 
                        else EditingTool.Crop
                    )
                }
            )
            
            // Resize tool
            ToolButton(
                icon = Icons.Default.Build,
                label = "Resize",
                isSelected = selectedTool == EditingTool.Resize,
                enabled = hasImage,
                onClick = { 
                    onToolSelected(
                        if (selectedTool == EditingTool.Resize) EditingTool.None 
                        else EditingTool.Resize
                    )
                }
            )
            
            // Filter tool
            ToolButton(
                icon = Icons.Default.Settings,
                label = "Filter",
                isSelected = selectedTool == EditingTool.Filter,
                enabled = hasImage,
                onClick = { 
                    onToolSelected(
                        if (selectedTool == EditingTool.Filter) EditingTool.None 
                        else EditingTool.Filter
                    )
                }
            )
            
            // Save button
            SaveButton(
                onClick = onSave,
                isSaving = isSaving,
                enabled = hasImage
            )
        }
    }
}

/**
 * Individual tool button in the toolbar
 */
@Composable
private fun ToolButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isSelected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
    ) {
        FilledTonalButton(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier
                .size(56.dp)
                .semantics {
                    contentDescription = "$label tool"
                },
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected && enabled) {
                    MaterialTheme.colorScheme.primary
                } else if (enabled) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                }
            )
        }
        
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (enabled) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            },
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

/**
 * Save button with loading state
 */
@Composable
private fun SaveButton(
    onClick: () -> Unit,
    isSaving: Boolean,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
    ) {
        Button(
            onClick = onClick,
            enabled = enabled && !isSaving,
            modifier = Modifier
                .size(56.dp)
                .semantics {
                    contentDescription = "Save image"
                },
            shape = RoundedCornerShape(12.dp)
        ) {
            if (isSaving) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Done,
                    contentDescription = null
                )
            }
        }
        
        Text(
            text = if (isSaving) stringResource(R.string.saving_image) else stringResource(R.string.save_image),
            style = MaterialTheme.typography.labelSmall,
            color = if (enabled) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            }
        )
    }
}/**
 *
 Bottom sheet panel with tool-specific controls
 */
@Composable
private fun ToolControlPanel(
    selectedTool: EditingTool,
    uiState: ImageEditorUiState,
    onAction: (ImageEditorAction) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp)
    ) {
        when (selectedTool) {
            EditingTool.Crop -> {
                CropControlPanel(
                    cropBounds = uiState.cropBounds,
                    onApplyCrop = { onAction(ImageEditorAction.ApplyCrop) },
                    onCancel = { onAction(ImageEditorAction.SelectTool(EditingTool.None)) }
                )
            }
            EditingTool.Resize -> {
                ResizeControlPanel(
                    currentWidth = uiState.editedImage?.width,
                    currentHeight = uiState.editedImage?.height,
                    resizeWidth = uiState.resizeWidth,
                    resizeHeight = uiState.resizeHeight,
                    onDimensionsChanged = { width, height ->
                        onAction(ImageEditorAction.SetResizeDimensions(width, height))
                    },
                    onApplyResize = { onAction(ImageEditorAction.ApplyResize) },
                    onCancel = { onAction(ImageEditorAction.SelectTool(EditingTool.None)) }
                )
            }
            EditingTool.Filter -> {
                FilterControlPanel(
                    selectedFilter = uiState.selectedFilter,
                    filterIntensity = uiState.filterIntensity,
                    onFilterSelected = { filter ->
                        onAction(ImageEditorAction.SelectFilter(filter))
                    },
                    onIntensityChanged = { intensity ->
                        onAction(ImageEditorAction.SetFilterIntensity(intensity))
                    },
                    onApplyFilter = { onAction(ImageEditorAction.ApplyFilter) },
                    onCancel = { onAction(ImageEditorAction.SelectTool(EditingTool.None)) }
                )
            }
            EditingTool.None -> {
                // Empty panel when no tool is selected
                Spacer(modifier = Modifier.height(1.dp))
            }
        }
    }
}

/**
 * Control panel for crop tool
 */
@Composable
private fun CropControlPanel(
    cropBounds: android.graphics.Rect?,
    onApplyCrop: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Crop Tool",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Text(
            text = stringResource(R.string.crop_tool_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        if (cropBounds != null) {
            Text(
                text = "Crop area: ${cropBounds.width()} × ${cropBounds.height()}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Cancel")
            }
            
            Button(
                onClick = onApplyCrop,
                enabled = cropBounds != null,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Apply")
            }
        }
    }
}

/**
 * Control panel for resize tool
 */
@Composable
private fun ResizeControlPanel(
    currentWidth: Int?,
    currentHeight: Int?,
    resizeWidth: Int?,
    resizeHeight: Int?,
    onDimensionsChanged: (Int, Int) -> Unit,
    onApplyResize: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Resize Tool",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        if (currentWidth != null && currentHeight != null) {
            Text(
                text = "Current size: $currentWidth × $currentHeight",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Text(
            text = "Enter new dimensions for the image. The resize tool will be implemented in the next task.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Cancel")
            }
            
            Button(
                onClick = onApplyResize,
                enabled = resizeWidth != null && resizeHeight != null,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Apply")
            }
        }
    }
}

/**
 * Control panel for filter tool
 */
@Composable
private fun FilterControlPanel(
    selectedFilter: com.uaialternativa.imageeditor.domain.model.FilterType?,
    filterIntensity: Float,
    onFilterSelected: (com.uaialternativa.imageeditor.domain.model.FilterType) -> Unit,
    onIntensityChanged: (Float) -> Unit,
    onApplyFilter: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Filter Tool",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Text(
            text = "Select a filter to apply to your image. The filter selection and intensity controls will be implemented in the next task.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        if (selectedFilter != null) {
            Text(
                text = "Selected filter: ${selectedFilter.name}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
            
            Text(
                text = "Intensity: ${(filterIntensity * 100).toInt()}%",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Cancel")
            }
            
            Button(
                onClick = onApplyFilter,
                enabled = selectedFilter != null,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Apply")
            }
        }
    }
}

/**
 * Dialog shown when user tries to navigate back with unsaved changes
 */
@Composable
private fun UnsavedChangesDialog(
    onSaveAndExit: () -> Unit,
    onDiscardAndExit: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = {
            Text(
                text = stringResource(R.string.unsaved_changes_title),
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Text(
                text = stringResource(R.string.unsaved_changes_message),
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            Button(onClick = onSaveAndExit) {
                Text(stringResource(R.string.save_and_exit))
            }
        },
        dismissButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(onClick = onDiscardAndExit) {
                    Text(stringResource(R.string.discard_changes))
                }
                TextButton(onClick = onCancel) {
                    Text(stringResource(R.string.cancel))
                }
            }
        },
        modifier = modifier
    )
}