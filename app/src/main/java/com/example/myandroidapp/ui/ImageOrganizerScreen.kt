package com.example.myandroidapp.ui

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.myandroidapp.data.FolderItem
import com.example.myandroidapp.data.MediaItem
import com.example.myandroidapp.ui.theme.ColorCardBackground
import com.example.myandroidapp.ui.theme.ColorDark0
import com.example.myandroidapp.ui.theme.ColorDark1
import com.example.myandroidapp.ui.theme.ColorOrange1
import com.example.myandroidapp.ui.theme.ColorOrange2
import com.example.myandroidapp.ui.theme.ColorOrange3
import com.example.myandroidapp.ui.theme.ColorRed1
import com.example.myandroidapp.ui.theme.ColorRed2
import com.example.myandroidapp.ui.theme.ColorRed3
import com.example.myandroidapp.ui.theme.ColorTextPrimary
import com.example.myandroidapp.ui.theme.ColorTextSecondary
import com.example.myandroidapp.ui.theme.ColorYellow1
import com.example.myandroidapp.ui.theme.ColorYellow2
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

private val folderColors = listOf(
    ColorRed3,
    ColorOrange1,
    ColorOrange2,
    ColorOrange3,
    ColorRed2,
    ColorRed1
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageOrganizerScreen(
    viewModel: MainViewModel,
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // System consent launcher for MediaStore trash & delete requests
    val consentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        viewModel.onConsentResult(result.resultCode == Activity.RESULT_OK)
    }

    LaunchedEffect(uiState.pendingConsentRequest) {
        uiState.pendingConsentRequest?.let { pending ->
            val request = IntentSenderRequest.Builder(pending.intentSender).build()
            consentLauncher.launch(request)
        }
    }

    LaunchedEffect(uiState.userMessage) {
        uiState.userMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearUserMessage()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        ColorDark0,
                        ColorDark1,
                        ColorDark0
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // FlicPic Branding & Folder Header Bar
            FlicPicAppHeader(
                appTitle = uiState.appTitle,
                activeFolder = uiState.selectedFolder,
                currentIndex = uiState.currentIndex,
                totalImages = uiState.images.size,
                trashedCount = uiState.trashedItems.size,
                onOpenTrashClick = { viewModel.openTrashSheet() }
            )

            // Main Viewer Area with Prominent Arrows & Smooth Animations
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                if (!uiState.hasPermission) {
                    PermissionCard(onRequestPermission = onRequestPermission)
                } else if (uiState.isLoading && uiState.images.isEmpty()) {
                    CircularProgressIndicator(color = ColorYellow2)
                } else if (!uiState.hasImages) {
                    EmptyFolderPlaceholder(folderName = uiState.selectedFolder?.name)
                } else {
                    val currentItem = uiState.currentImage
                    if (currentItem != null) {
                        AnimatedImageViewerWithArrows(
                            mediaItem = currentItem,
                            currentIndex = uiState.currentIndex,
                            hasPrevious = uiState.hasPrevious,
                            hasNext = uiState.hasNext,
                            onPreviousClick = { viewModel.previousImage() },
                            onNextClick = { viewModel.nextImage() },
                            onSwipeUp = { viewModel.trashCurrentImage() }
                        )
                    }
                }
            }

            // Clean Folder Selection Row (No watermark or color overlay on thumbnails)
            FolderSelectionRow(
                folders = uiState.folders,
                selectedFolder = uiState.selectedFolder,
                onFolderSelected = { viewModel.selectFolder(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
            )

            // Bottom Action Bar (Undo, Trash View, Permanent Delete)
            TrashAndActionBar(
                canUndo = uiState.canUndo,
                trashedCount = uiState.trashedItems.size,
                pendingSyncCount = uiState.pendingSystemSyncCount,
                onUndoClick = { viewModel.undoLastTrash() },
                onOpenTrashClick = { viewModel.openTrashSheet() },
                onEmptyTrashClick = { viewModel.requestPermanentDeleteConfirmation() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 4.dp)
            )
        }

        // Snackbar Host for status notifications
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 80.dp)
        )

        // Trash & Restore Bottom Sheet
        if (uiState.showTrashSheet) {
            TrashAndRestoreBottomSheet(
                trashedItems = uiState.trashedItems,
                onRestoreItem = { viewModel.restoreItem(it) },
                onSyncSystemTrash = { viewModel.syncPendingTrashToSystem() },
                onEmptyTrash = { viewModel.requestPermanentDeleteConfirmation() },
                onDismiss = { viewModel.closeTrashSheet() }
            )
        }

        // Permanent Deletion Confirmation Dialog
        if (uiState.showDeleteConfirmDialog) {
            PermanentDeleteDialog(
                itemCount = uiState.trashedItems.size,
                onConfirm = { viewModel.confirmPermanentDelete() },
                onDismiss = { viewModel.dismissDeleteConfirmation() }
            )
        }
    }
}

/**
 * FlicPic Top Bar & App Branding:
 * Displays "FlicPic" on the top left, with the current folder/album name dynamically next to it.
 */
@Composable
private fun FlicPicAppHeader(
    appTitle: String,
    activeFolder: FolderItem?,
    currentIndex: Int,
    totalImages: Int,
    trashedCount: Int,
    onOpenTrashClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                Text(
                    text = appTitle,
                    color = ColorYellow2,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = " • ",
                    color = ColorOrange2,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = activeFolder?.name ?: "Galeri",
                    color = ColorTextPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = "Yukarı Kaydır: Çöpe At | Oklar / Sola-Sağa: Gezin",
                color = ColorTextSecondary,
                fontSize = 11.sp
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            if (totalImages > 0) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = ColorRed1,
                    shadowElevation = 4.dp,
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Text(
                        text = "${currentIndex + 1} / $totalImages",
                        color = ColorTextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            BadgedBox(
                badge = {
                    if (trashedCount > 0) {
                        Badge(containerColor = ColorYellow2, contentColor = ColorDark0) {
                            Text(text = "$trashedCount", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            ) {
                IconButton(
                    onClick = onOpenTrashClick,
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = ColorDark1,
                        contentColor = ColorOrange3
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Çöp Kutusu"
                    )
                }
            }
        }
    }
}

/**
 * Animated Image Viewer with Prominent Navigation Arrows overlaying the screen.
 */
@Composable
private fun AnimatedImageViewerWithArrows(
    mediaItem: MediaItem,
    currentIndex: Int,
    hasPrevious: Boolean,
    hasNext: Boolean,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
    onSwipeUp: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedContent(
        targetState = Pair(mediaItem.id, currentIndex),
        transitionSpec = {
            if (targetState.second > initialState.second) {
                (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
                    slideOutHorizontally { width -> -width } + fadeOut()
                )
            } else {
                (slideInHorizontally { width -> -width } + fadeIn()).togetherWith(
                    slideOutHorizontally { width -> width } + fadeOut()
                )
            }
        },
        label = "ImageTransition"
    ) { (itemId, _) ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(24.dp))
                .background(ColorCardBackground)
                .border(
                    width = 2.dp,
                    brush = Brush.verticalGradient(
                        listOf(
                            ColorRed2.copy(alpha = 0.7f),
                            ColorOrange2.copy(alpha = 0.5f)
                        )
                    ),
                    shape = RoundedCornerShape(24.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            SwipeableImageContent(
                mediaItem = mediaItem,
                itemId = itemId,
                onSwipeLeft = onNextClick,
                onSwipeRight = onPreviousClick,
                onSwipeUp = onSwipeUp
            )

            // Prominent Left Arrow Button
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 12.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = ColorDark0.copy(alpha = if (hasPrevious) 0.85f else 0.35f),
                    shadowElevation = if (hasPrevious) 8.dp else 0.dp,
                    border = if (hasPrevious) BorderStroke(2.dp, ColorYellow2) else null,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .clickable(enabled = hasPrevious, onClick = onPreviousClick)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(
                            imageVector = Icons.Default.ChevronLeft,
                            contentDescription = "Önceki Fotoğraf",
                            tint = if (hasPrevious) ColorYellow2 else ColorTextSecondary.copy(alpha = 0.4f),
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
            }

            // Prominent Right Arrow Button
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 12.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = ColorDark0.copy(alpha = if (hasNext) 0.85f else 0.35f),
                    shadowElevation = if (hasNext) 8.dp else 0.dp,
                    border = if (hasNext) BorderStroke(2.dp, ColorYellow2) else null,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .clickable(enabled = hasNext, onClick = onNextClick)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Sonraki Fotoğraf",
                            tint = if (hasNext) ColorYellow2 else ColorTextSecondary.copy(alpha = 0.4f),
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Gesture Surface for upward-only swiping to trash.
 */
@Composable
private fun SwipeableImageContent(
    mediaItem: MediaItem,
    itemId: Long,
    onSwipeLeft: () -> Unit,
    onSwipeRight: () -> Unit,
    onSwipeUp: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current
    val thresholdPx = with(density) { 90.dp.toPx() }

    val offsetX = remember(itemId) { Animatable(0f) }
    val offsetY = remember(itemId) { Animatable(0f) }

    val upwardRatio = (-offsetY.value / thresholdPx).coerceIn(0f, 1f)

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(itemId) {
                var dragDirectionDetermined = false
                var isHorizontalDrag = false
                var accumulatedX = 0f
                var accumulatedY = 0f

                detectDragGestures(
                    onDragStart = {
                        dragDirectionDetermined = false
                        isHorizontalDrag = false
                        accumulatedX = 0f
                        accumulatedY = 0f
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        accumulatedX += dragAmount.x
                        accumulatedY += dragAmount.y

                        if (!dragDirectionDetermined) {
                            if (abs(accumulatedX) > abs(accumulatedY) && abs(accumulatedX) > 15f) {
                                dragDirectionDetermined = true
                                isHorizontalDrag = true
                            } else if (abs(accumulatedY) > abs(accumulatedX) && abs(accumulatedY) > 15f) {
                                dragDirectionDetermined = true
                                isHorizontalDrag = false
                            }
                        }

                        if (dragDirectionDetermined) {
                            coroutineScope.launch {
                                if (isHorizontalDrag) {
                                    offsetX.snapTo(offsetX.value + dragAmount.x)
                                } else {
                                    val newY = offsetY.value + dragAmount.y
                                    if (newY <= 0f) {
                                        offsetY.snapTo(newY)
                                    } else {
                                        offsetY.snapTo(0f)
                                    }
                                }
                            }
                        }
                    },
                    onDragEnd = {
                        coroutineScope.launch {
                            if (isHorizontalDrag) {
                                if (offsetX.value > thresholdPx) {
                                    onSwipeRight()
                                } else if (offsetX.value < -thresholdPx) {
                                    onSwipeLeft()
                                }
                                offsetX.animateTo(0f, spring(stiffness = Spring.StiffnessMediumLow))
                            } else {
                                if (offsetY.value < -thresholdPx) {
                                    offsetY.animateTo(-1500f, tween(180))
                                    onSwipeUp()
                                    offsetY.snapTo(0f)
                                } else {
                                    offsetY.animateTo(0f, spring(stiffness = Spring.StiffnessMediumLow))
                                }
                            }
                        }
                    },
                    onDragCancel = {
                        coroutineScope.launch {
                            offsetX.animateTo(0f)
                            offsetY.animateTo(0f)
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(mediaItem.uri)
                .crossfade(true)
                .build(),
            contentDescription = mediaItem.name,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .offset {
                    IntOffset(
                        x = offsetX.value.roundToInt(),
                        y = offsetY.value.roundToInt()
                    )
                }
                .graphicsLayer {
                    val scale = 1f - (upwardRatio * 0.15f)
                    scaleX = scale
                    scaleY = scale
                    alpha = 1f - (upwardRatio * 0.4f)
                }
        )

        if (upwardRatio > 0.05f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(ColorRed3.copy(alpha = upwardRatio * 0.4f)),
                contentAlignment = Alignment.TopCenter
            ) {
                Surface(
                    shape = RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp),
                    color = ColorRed3,
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Çöpe At",
                            tint = ColorTextPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (upwardRatio >= 0.9f) "Bırak ve Çöpe Taşınsın!" else "Çöpe Taşımak İçin Yukarı Kaydır",
                            color = ColorTextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

/**
 * Circular Folder Selection Row.
 * Thumbnails are rendered CLEANLY WITHOUT ANY ORANGE/RED WATERMARK OR MASK OVERLAY.
 */
@Composable
private fun FolderSelectionRow(
    folders: List<FolderItem>,
    selectedFolder: FolderItem?,
    onFolderSelected: (FolderItem) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            itemsIndexed(folders) { index, folder ->
                val isSelected = folder.bucketId == selectedFolder?.bucketId
                val circleColor = folderColors[index % folderColors.size]

                FolderCircleItem(
                    folder = folder,
                    isSelected = isSelected,
                    circleColor = circleColor,
                    onClick = { onFolderSelected(folder) }
                )
            }
        }
    }
}

/**
 * Clean Folder Circular Thumbnail (NO OVERLAY / WATERMARK).
 */
@Composable
private fun FolderCircleItem(
    folder: FolderItem,
    isSelected: Boolean,
    circleColor: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .width(68.dp)
    ) {
        Box(
            modifier = Modifier
                .size(58.dp)
                .shadow(elevation = if (isSelected) 8.dp else 3.dp, shape = CircleShape)
                .clip(CircleShape)
                .background(ColorDark0)
                .border(
                    width = if (isSelected) 3.5.dp else 1.5.dp,
                    color = if (isSelected) ColorYellow2 else circleColor.copy(alpha = 0.6f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (folder.coverUri != null) {
                // CLEAN THUMBNAIL WITHOUT ANY COLOR MASK OVERLAY
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(folder.coverUri)
                        .crossfade(true)
                        .build(),
                    contentDescription = folder.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Folder,
                    contentDescription = null,
                    tint = ColorYellow2,
                    modifier = Modifier.size(26.dp)
                )
            }

            // Folder count badge
            Surface(
                shape = CircleShape,
                color = ColorDark0.copy(alpha = 0.85f),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 2.dp)
            ) {
                Text(
                    text = "${folder.count}",
                    color = ColorYellow2,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = folder.name,
            color = if (isSelected) ColorYellow2 else ColorTextSecondary,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Action & Trash Bar
 */
@Composable
private fun TrashAndActionBar(
    canUndo: Boolean,
    trashedCount: Int,
    pendingSyncCount: Int,
    onUndoClick: () -> Unit,
    onOpenTrashClick: () -> Unit,
    onEmptyTrashClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = ColorYellow2,
        shadowElevation = 8.dp,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Undo Action Button ("Geri Al")
            Button(
                onClick = onUndoClick,
                enabled = canUndo,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ColorDark0,
                    contentColor = ColorYellow2,
                    disabledContainerColor = ColorDark0.copy(alpha = 0.4f),
                    disabledContentColor = ColorDark1
                ),
                shape = RoundedCornerShape(14.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Undo,
                    contentDescription = "Geri Al",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Geri Al",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }

            // View Trash Items Button
            Button(
                onClick = onOpenTrashClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ColorOrange1,
                    contentColor = ColorTextPrimary
                ),
                shape = RoundedCornerShape(14.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Çöp Kutusu",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (pendingSyncCount > 0) "Çöp ($trashedCount*)" else "Çöp ($trashedCount)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }

            // Permanent Delete Button ("Kalıcı Sil")
            Button(
                onClick = onEmptyTrashClick,
                enabled = trashedCount > 0,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ColorRed3,
                    contentColor = ColorTextPrimary,
                    disabledContainerColor = ColorRed1.copy(alpha = 0.35f),
                    disabledContentColor = ColorTextSecondary.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(14.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteForever,
                    contentDescription = "Kalıcı Sil",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Kalıcı Sil",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }
    }
}

/**
 * Trash & Restore Bottom Sheet with Batch Sync options
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TrashAndRestoreBottomSheet(
    trashedItems: List<MediaItem>,
    onRestoreItem: (MediaItem) -> Unit,
    onSyncSystemTrash: () -> Unit,
    onEmptyTrash: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val unsyncedCount = trashedItems.count { !it.isSystemSynced }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = ColorDark1,
        contentColor = ColorTextPrimary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.75f)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        tint = ColorYellow2,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Çöp Kutusu (${trashedItems.size})",
                        color = ColorYellow2,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Kapat",
                        tint = ColorTextSecondary
                    )
                }
            }

            if (unsyncedCount > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = ColorOrange2.copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, ColorOrange2),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "$unsyncedCount fotoğraf galeri çöpüne işlenmeyi bekliyor.",
                            color = ColorYellow2,
                            fontSize = 12.sp,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = onSyncSystemTrash) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Sync,
                                    contentDescription = null,
                                    tint = ColorYellow2,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Sistem Çöpüne İşle",
                                    color = ColorYellow2,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (trashedItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Çöp kutusunda fotoğraf yok.",
                        color = ColorTextSecondary,
                        fontSize = 14.sp
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(trashedItems, key = { it.id }) { item ->
                        TrashedItemCard(
                            mediaItem = item,
                            onRestore = { onRestoreItem(item) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = onEmptyTrash,
                    colors = ButtonDefaults.buttonColors(containerColor = ColorRed3),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.DeleteForever, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Çöpü Kalıcı Olarak Temizle",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun TrashedItemCard(
    mediaItem: MediaItem,
    onRestore: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = ColorDark0,
        shadowElevation = 4.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(10.dp))
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(mediaItem.uri)
                        .crossfade(true)
                        .build(),
                    contentDescription = mediaItem.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = mediaItem.name,
                color = ColorTextPrimary,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(6.dp))

            Button(
                onClick = onRestore,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ColorOrange2,
                    contentColor = ColorTextPrimary
                ),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Restore,
                    contentDescription = "Geri Yükle",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Geri Yükle",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * Permanent Delete Confirmation Dialog
 */
@Composable
private fun PermanentDeleteDialog(
    itemCount: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ColorDark1,
        titleContentColor = ColorYellow2,
        textContentColor = ColorTextPrimary,
        title = {
            Text(
                text = "Fotoğrafları Kalıcı Olarak Sil",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = "$itemCount adet fotoğraf cihazınızdan kalıcı olarak silinecektir. Bu işlem geri alınamaz. Onaylıyor musunuz?",
                fontSize = 14.sp
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = ColorRed3)
            ) {
                Text("Kalıcı Olarak Sil", color = ColorTextPrimary, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("İptal", color = ColorTextSecondary)
            }
        }
    )
}

/**
 * Permission Request Card
 */
@Composable
private fun PermissionCard(onRequestPermission: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = ColorDark1,
        shadowElevation = 6.dp,
        modifier = Modifier.padding(24.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.PhotoLibrary,
                contentDescription = null,
                tint = ColorYellow2,
                modifier = Modifier.size(54.dp)
            )
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "Galeri Erişimi Gerekli",
                color = ColorTextPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Cihazınızdaki fotoğrafları albüm albüm görüntülemek ve düzenlemek için depolama izni vermelisiniz.",
                color = ColorTextSecondary,
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(18.dp))
            Button(
                onClick = onRequestPermission,
                colors = ButtonDefaults.buttonColors(containerColor = ColorOrange2),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("İzni Etkinleştir", color = ColorTextPrimary, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/**
 * Empty Folder Placeholder
 */
@Composable
private fun EmptyFolderPlaceholder(folderName: String?) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.padding(32.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Folder,
            contentDescription = null,
            tint = ColorOrange3,
            modifier = Modifier.size(50.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "${folderName ?: "Klasör"} boş",
            color = ColorTextPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Bu albümde gösterilecek fotoğraf yok ya da tümü çöpe taşındı.",
            color = ColorTextSecondary,
            fontSize = 12.sp,
            textAlign = TextAlign.Center
        )
    }
}
