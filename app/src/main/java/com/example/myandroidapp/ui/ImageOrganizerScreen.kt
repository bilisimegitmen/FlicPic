package com.example.myandroidapp.ui

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
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
import com.example.myandroidapp.ui.theme.ColorBody
import com.example.myandroidapp.ui.theme.ColorCanvas
import com.example.myandroidapp.ui.theme.ColorCanvasSoft
import com.example.myandroidapp.ui.theme.ColorHairline
import com.example.myandroidapp.ui.theme.ColorHairlineSoft
import com.example.myandroidapp.ui.theme.ColorHairlineStrong
import com.example.myandroidapp.ui.theme.ColorInk
import com.example.myandroidapp.ui.theme.ColorMuted
import com.example.myandroidapp.ui.theme.ColorMutedSoft
import com.example.myandroidapp.ui.theme.ColorOnPrimary
import com.example.myandroidapp.ui.theme.ColorPrimary
import com.example.myandroidapp.ui.theme.ColorSemanticError
import com.example.myandroidapp.ui.theme.ColorSurfaceCard
import com.example.myandroidapp.ui.theme.ColorSurfaceStrong
import com.example.myandroidapp.ui.theme.ColorTimelineDone
import com.example.myandroidapp.ui.theme.ColorTimelineEdit
import com.example.myandroidapp.ui.theme.ColorTimelineGrep
import com.example.myandroidapp.ui.theme.ColorTimelineRead
import com.example.myandroidapp.ui.theme.ColorTimelineThinking
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

// Signature AI-timeline pastel palette from design.md
private val timelineFolderColors = listOf(
    ColorTimelineThinking,
    ColorTimelineGrep,
    ColorTimelineRead,
    ColorTimelineEdit,
    ColorTimelineDone
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
            .background(ColorCanvas)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Editorial Header Bar
            EditorialAppHeader(
                appTitle = uiState.appTitle,
                activeFolder = uiState.selectedFolder,
                currentIndex = uiState.currentIndex,
                totalImages = uiState.images.size,
                trashedCount = uiState.trashedItems.size,
                onOpenTrashClick = { viewModel.openTrashSheet() }
            )

            // Main Viewer Area (Hairline-only depth, no drop shadows)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                if (!uiState.hasPermission) {
                    EditorialPermissionCard(onRequestPermission = onRequestPermission)
                } else if (uiState.isLoading && uiState.images.isEmpty()) {
                    CircularProgressIndicator(color = ColorPrimary, strokeWidth = 2.dp)
                } else if (!uiState.hasImages) {
                    EditorialEmptyFolderPlaceholder(folderName = uiState.selectedFolder?.name)
                } else {
                    val currentItem = uiState.currentImage
                    if (currentItem != null) {
                        EditorialImageViewer(
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

            // Folder Selection Row
            EditorialFolderSelectionRow(
                folders = uiState.folders,
                selectedFolder = uiState.selectedFolder,
                onFolderSelected = { viewModel.selectFolder(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
            )

            // Bottom Action Bar (Hairline card, 8dp buttons, no drop shadows)
            EditorialActionBar(
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
            EditorialTrashBottomSheet(
                trashedItems = uiState.trashedItems,
                onRestoreItem = { viewModel.restoreItem(it) },
                onSyncSystemTrash = { viewModel.syncPendingTrashToSystem() },
                onEmptyTrash = { viewModel.requestPermanentDeleteConfirmation() },
                onDismiss = { viewModel.closeTrashSheet() }
            )
        }

        // Permanent Deletion Confirmation Dialog
        if (uiState.showDeleteConfirmDialog) {
            EditorialPermanentDeleteDialog(
                itemCount = uiState.trashedItems.size,
                onConfirm = { viewModel.confirmPermanentDelete() },
                onDismiss = { viewModel.dismissDeleteConfirmation() }
            )
        }
    }
}

/**
 * Editorial Header Bar:
 * - Display weight 400 (never bold), negative letter-spacing (-0.5px).
 * - Warm near-black Ink for title, subtle separator, Muted for captions.
 */
@Composable
private fun EditorialAppHeader(
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
            .padding(top = 4.dp, bottom = 6.dp),
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
                    color = ColorInk,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Normal, // design.md: Display stays at weight 400
                    letterSpacing = (-0.5).sp
                )
                Text(
                    text = " / ",
                    color = ColorHairlineStrong,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Normal
                )
                Text(
                    text = activeFolder?.name ?: "Galeri",
                    color = ColorBody,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = "Yukarı Kaydır: Çöpe At · Oklar: Gezin",
                color = ColorMuted,
                fontSize = 12.sp,
                letterSpacing = 0.sp
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            if (totalImages > 0) {
                Surface(
                    shape = RoundedCornerShape(9999.dp), // rounded.pill
                    color = ColorSurfaceStrong,
                    border = BorderStroke(1.dp, ColorHairline),
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Text(
                        text = "${currentIndex + 1} / $totalImages",
                        color = ColorInk,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            BadgedBox(
                badge = {
                    if (trashedCount > 0) {
                        Badge(
                            containerColor = ColorPrimary,
                            contentColor = ColorOnPrimary
                        ) {
                            Text(
                                text = "$trashedCount",
                                fontWeight = FontWeight.Medium,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            ) {
                IconButton(
                    onClick = onOpenTrashClick,
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = ColorSurfaceCard,
                        contentColor = ColorInk
                    ),
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.dp, ColorHairline, RoundedCornerShape(8.dp))
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Çöp Kutusu",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

/**
 * Editorial Image Viewer:
 * - Hairline-only depth (1px ColorHairline on ColorSurfaceCard), no drop shadows.
 * - Rounded 12dp ({rounded.lg}).
 * - Minimalist circle navigation arrows.
 */
@Composable
private fun EditorialImageViewer(
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
                .clip(RoundedCornerShape(12.dp)) // rounded.lg
                .background(ColorSurfaceCard)
                .border(1.dp, ColorHairline, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            SwipeableImageContent(
                mediaItem = mediaItem,
                itemId = itemId,
                onSwipeLeft = onNextClick,
                onSwipeRight = onPreviousClick,
                onSwipeUp = onSwipeUp
            )

            // Prominent Left Arrow Button (Hairline card, no drop shadow)
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 12.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = ColorSurfaceCard.copy(alpha = if (hasPrevious) 0.94f else 0.4f),
                    border = BorderStroke(1.dp, if (hasPrevious) ColorHairlineStrong else ColorHairlineSoft),
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .clickable(enabled = hasPrevious, onClick = onPreviousClick)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(
                            imageVector = Icons.Default.ChevronLeft,
                            contentDescription = "Önceki Fotoğraf",
                            tint = if (hasPrevious) ColorInk else ColorMutedSoft,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }

            // Prominent Right Arrow Button (Hairline card, no drop shadow)
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 12.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = ColorSurfaceCard.copy(alpha = if (hasNext) 0.94f else 0.4f),
                    border = BorderStroke(1.dp, if (hasNext) ColorHairlineStrong else ColorHairlineSoft),
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .clickable(enabled = hasNext, onClick = onNextClick)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Sonraki Fotoğraf",
                            tint = if (hasNext) ColorInk else ColorMutedSoft,
                            modifier = Modifier.size(28.dp)
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
                    val scale = 1f - (upwardRatio * 0.12f)
                    scaleX = scale
                    scaleY = scale
                    alpha = 1f - (upwardRatio * 0.35f)
                }
        )

        // Editorial Upward Swipe Indicator (Pastel peach & semantic error border, no drop shadow)
        if (upwardRatio > 0.05f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(ColorSemanticError.copy(alpha = upwardRatio * 0.12f)),
                contentAlignment = Alignment.TopCenter
            ) {
                Surface(
                    shape = RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp),
                    color = ColorSurfaceCard,
                    border = BorderStroke(1.dp, ColorSemanticError)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Çöpe At",
                            tint = ColorSemanticError,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (upwardRatio >= 0.9f) "Bırakın, çöpe taşınsın" else "Çöpe taşımak için yukarı kaydırın",
                            color = ColorSemanticError,
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}

/**
 * Editorial Folder Selection Row:
 * Uses the 5 AI-timeline pastel colors for ring accents on folders.
 */
@Composable
private fun EditorialFolderSelectionRow(
    folders: List<FolderItem>,
    selectedFolder: FolderItem?,
    onFolderSelected: (FolderItem) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 2.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            itemsIndexed(folders) { index, folder ->
                val isSelected = folder.bucketId == selectedFolder?.bucketId
                val accentColor = timelineFolderColors[index % timelineFolderColors.size]

                EditorialFolderCircleItem(
                    folder = folder,
                    isSelected = isSelected,
                    accentColor = accentColor,
                    onClick = { onFolderSelected(folder) }
                )
            }
        }
    }
}

/**
 * Editorial Folder Circular Thumbnail (Hairline borders, no drop shadows).
 */
@Composable
private fun EditorialFolderCircleItem(
    folder: FolderItem,
    isSelected: Boolean,
    accentColor: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .width(66.dp)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(ColorSurfaceCard)
                .border(
                    width = if (isSelected) 2.dp else 1.dp,
                    color = if (isSelected) ColorPrimary else accentColor,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (folder.coverUri != null) {
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
                    tint = ColorMuted,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Folder count badge (Pill, hairline border)
            Surface(
                shape = RoundedCornerShape(9999.dp),
                color = ColorSurfaceCard,
                border = BorderStroke(1.dp, ColorHairline),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 2.dp)
            ) {
                Text(
                    text = "${folder.count}",
                    color = ColorInk,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = folder.name,
            color = if (isSelected) ColorInk else ColorMuted,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Editorial Action & Trash Bar:
 * - Surface card with 1px hairline border ({colors.hairline}), NO DROP SHADOWS.
 * - Compact 8dp radius for CTA buttons ({rounded.md}).
 * - Cursor Orange used scarcely on primary callouts.
 */
@Composable
private fun EditorialActionBar(
    canUndo: Boolean,
    trashedCount: Int,
    pendingSyncCount: Int,
    onUndoClick: () -> Unit,
    onOpenTrashClick: () -> Unit,
    onEmptyTrashClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp), // rounded.lg
        color = ColorSurfaceCard,
        border = BorderStroke(1.dp, ColorHairline),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Undo Button (Secondary button: white/soft surface, ink text, hairline border, 8dp radius)
            Button(
                onClick = onUndoClick,
                enabled = canUndo,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ColorCanvasSoft,
                    contentColor = ColorInk,
                    disabledContainerColor = ColorCanvasSoft.copy(alpha = 0.5f),
                    disabledContentColor = ColorMutedSoft
                ),
                shape = RoundedCornerShape(8.dp), // rounded.md
                border = BorderStroke(1.dp, ColorHairlineStrong),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Undo,
                    contentDescription = "Geri Al",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Geri Al",
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp
                )
            }

            // View Trash Items Button (Secondary style, subtle orange highlight if pending sync)
            Button(
                onClick = onOpenTrashClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (pendingSyncCount > 0) ColorPrimary.copy(alpha = 0.12f) else ColorCanvasSoft,
                    contentColor = if (pendingSyncCount > 0) ColorPrimary else ColorInk
                ),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, if (pendingSyncCount > 0) ColorPrimary else ColorHairlineStrong),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Çöp Kutusu",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (pendingSyncCount > 0) "Çöp ($trashedCount*)" else "Çöp ($trashedCount)",
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp
                )
            }

            // Permanent Delete Button (Semantic error button)
            Button(
                onClick = onEmptyTrashClick,
                enabled = trashedCount > 0,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ColorSemanticError,
                    contentColor = ColorOnPrimary,
                    disabledContainerColor = ColorHairlineSoft,
                    disabledContentColor = ColorMutedSoft
                ),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteForever,
                    contentDescription = "Kalıcı Sil",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Kalıcı Sil",
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp
                )
            }
        }
    }
}

/**
 * Editorial Trash & Restore Bottom Sheet:
 * - Clean cream background, 1px hairline dividers.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditorialTrashBottomSheet(
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
        containerColor = ColorCanvas,
        contentColor = ColorInk
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
                        tint = ColorInk,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Çöp Kutusu (${trashedItems.size})",
                        color = ColorInk,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = (-0.3).sp
                    )
                }

                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Kapat",
                        tint = ColorBody
                    )
                }
            }

            if (unsyncedCount > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = ColorTimelineThinking.copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, ColorTimelineThinking),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "$unsyncedCount fotoğraf galeri çöpüne işlenmeyi bekliyor.",
                            color = ColorInk,
                            fontSize = 12.sp,
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = onSyncSystemTrash,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ColorPrimary,
                                contentColor = ColorOnPrimary
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Sync,
                                    contentDescription = null,
                                    tint = ColorOnPrimary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Sistem Çöpüne İşle",
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 11.sp
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
                        color = ColorMuted,
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
                        EditorialTrashedItemCard(
                            mediaItem = item,
                            onRestore = { onRestoreItem(item) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = onEmptyTrash,
                    colors = ButtonDefaults.buttonColors(containerColor = ColorSemanticError),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(imageVector = Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Çöpü Kalıcı Olarak Temizle",
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun EditorialTrashedItemCard(
    mediaItem: MediaItem,
    onRestore: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = ColorSurfaceCard,
        border = BorderStroke(1.dp, ColorHairline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(6.dp))
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
                color = ColorInk,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(6.dp))

            Button(
                onClick = onRestore,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ColorCanvasSoft,
                    contentColor = ColorInk
                ),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, ColorHairlineStrong),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Restore,
                    contentDescription = "Geri Yükle",
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Geri Yükle",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

/**
 * Editorial Permanent Delete Confirmation Dialog
 */
@Composable
private fun EditorialPermanentDeleteDialog(
    itemCount: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ColorSurfaceCard,
        titleContentColor = ColorInk,
        textContentColor = ColorBody,
        shape = RoundedCornerShape(12.dp),
        title = {
            Text(
                text = "Fotoğrafları Kalıcı Olarak Sil",
                fontWeight = FontWeight.Medium,
                fontSize = 18.sp
            )
        },
        text = {
            Text(
                text = "$itemCount adet fotoğraf cihazınızdan kalıcı olarak silinecektir. Bu işlem geri alınamaz. Onaylıyor musunuz?",
                fontSize = 14.sp,
                lineHeight = 20.sp
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = ColorSemanticError),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Kalıcı Olarak Sil", color = ColorOnPrimary, fontWeight = FontWeight.Medium)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, shape = RoundedCornerShape(8.dp)) {
                Text("İptal", color = ColorBody)
            }
        }
    )
}

/**
 * Editorial Permission Request Card
 */
@Composable
private fun EditorialPermissionCard(onRequestPermission: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = ColorSurfaceCard,
        border = BorderStroke(1.dp, ColorHairline),
        modifier = Modifier.padding(24.dp)
    ) {
        Column(
            modifier = Modifier.padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.PhotoLibrary,
                contentDescription = null,
                tint = ColorPrimary, // Cursor Orange
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "Galeri Erişimi Gerekli",
                color = ColorInk,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Cihazınızdaki fotoğrafları albüm albüm görüntülemek ve düzenlemek için depolama izni vermelisiniz.",
                color = ColorBody,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = onRequestPermission,
                colors = ButtonDefaults.buttonColors(containerColor = ColorPrimary),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp)
            ) {
                Text("İzni Etkinleştir", color = ColorOnPrimary, fontWeight = FontWeight.Medium)
            }
        }
    }
}

/**
 * Editorial Empty Folder Placeholder
 */
@Composable
private fun EditorialEmptyFolderPlaceholder(folderName: String?) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.padding(32.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Folder,
            contentDescription = null,
            tint = ColorMutedSoft,
            modifier = Modifier.size(44.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "${folderName ?: "Klasör"} boş",
            color = ColorInk,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Bu albümde gösterilecek fotoğraf yok ya da tümü çöpe taşındı.",
            color = ColorMuted,
            fontSize = 13.sp,
            textAlign = TextAlign.Center
        )
    }
}
