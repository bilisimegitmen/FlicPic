package com.example.myandroidapp.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.myandroidapp.data.FolderItem
import com.example.myandroidapp.data.MediaItem
import com.example.myandroidapp.data.MediaRepository
import com.example.myandroidapp.data.OperationResult
import com.example.myandroidapp.data.PendingConsentRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class OrganizerUiState(
    val appTitle: String = "FlicPic",
    val isLoading: Boolean = false,
    val hasPermission: Boolean = false,
    val folders: List<FolderItem> = emptyList(),
    val selectedFolder: FolderItem? = null,
    val images: List<MediaItem> = emptyList(),
    val currentIndex: Int = 0,
    val trashedItems: List<MediaItem> = emptyList(),
    val showTrashSheet: Boolean = false,
    val showDeleteConfirmDialog: Boolean = false,
    val pendingConsentRequest: PendingConsentRequest? = null,
    val userMessage: String? = null
) {
    val currentImage: MediaItem?
        get() = images.getOrNull(currentIndex)

    val hasImages: Boolean
        get() = images.isNotEmpty()

    val canUndo: Boolean
        get() = trashedItems.isNotEmpty()

    val hasPrevious: Boolean
        get() = currentIndex > 0

    val hasNext: Boolean
        get() = images.isNotEmpty() && currentIndex < images.size - 1

    val pendingSystemSyncCount: Int
        get() = trashedItems.count { !it.isSystemSynced }
}

/**
 * ViewModel managing "FlicPic" state, folder selections, arrow navigation,
 * seamless fluid upward-swipe trashing (without permission loops), batch system sync, and restoration.
 */
class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: MediaRepository = MediaRepository(application)

    private val _uiState = MutableStateFlow(OrganizerUiState())
    val uiState: StateFlow<OrganizerUiState> = _uiState.asStateFlow()

    fun onPermissionResult(granted: Boolean) {
        _uiState.update { it.copy(hasPermission = granted) }
        if (granted) {
            loadFoldersAndTrash()
        }
    }

    fun loadFoldersAndTrash() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val folders = repository.getFolders()
            val systemTrashed = repository.getSystemTrashedImages()
            val initialFolder = folders.firstOrNull()

            _uiState.update {
                it.copy(
                    folders = folders,
                    selectedFolder = initialFolder,
                    trashedItems = systemTrashed,
                    isLoading = false
                )
            }

            if (initialFolder != null) {
                selectFolder(initialFolder)
            }
        }
    }

    /**
     * Loads images for the tapped folder, strictly starting from the first image (index 0).
     */
    fun selectFolder(folder: FolderItem) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, selectedFolder = folder) }
            val rawImages = repository.getImagesForFolder(folder.bucketId)
            val trashedIds = _uiState.value.trashedItems.map { it.id }.toSet()
            val filteredImages = rawImages.filterNot { it.id in trashedIds }

            _uiState.update {
                it.copy(
                    images = filteredImages,
                    currentIndex = 0, // Strictly start from index 0
                    isLoading = false
                )
            }
        }
    }

    /**
     * Navigate to next image (Right Arrow / Left Swipe).
     */
    fun nextImage() {
        _uiState.update { state ->
            if (state.currentIndex < state.images.size - 1) {
                state.copy(currentIndex = state.currentIndex + 1)
            } else {
                state
            }
        }
    }

    /**
     * Navigate to previous image (Left Arrow / Right Swipe).
     */
    fun previousImage() {
        _uiState.update { state ->
            if (state.currentIndex > 0) {
                state.copy(currentIndex = state.currentIndex - 1)
            } else {
                state
            }
        }
    }

    /**
     * Upward Swipe Action (Fluid & Pop-up Free):
     * Instantly soft-trashes current image into app state and advances to the next image
     * without interrupting the user with system permission prompts on every swipe.
     */
    fun trashCurrentImage() {
        val currentState = _uiState.value
        val imageToTrash = currentState.currentImage ?: return

        val updatedImages = currentState.images.filterNot { it.id == imageToTrash.id }
        val trashedItem = imageToTrash.copy(isTrashed = true, isSystemSynced = false)
        val updatedTrash = currentState.trashedItems.filterNot { it.id == imageToTrash.id } + trashedItem

        val nextIndex = if (updatedImages.isEmpty()) {
            0
        } else {
            currentState.currentIndex.coerceAtMost(updatedImages.size - 1)
        }

        _uiState.update {
            it.copy(
                images = updatedImages,
                trashedItems = updatedTrash,
                currentIndex = nextIndex,
                userMessage = "\"${imageToTrash.name}\" çöpe taşındı."
            )
        }
    }

    /**
     * Syncs all pending soft-trashed items to the System Phone Gallery Trash Bin in a SINGLE batch operation.
     */
    fun syncPendingTrashToSystem() {
        val unsyncedItems = _uiState.value.trashedItems.filter { !it.isSystemSynced }
        if (unsyncedItems.isEmpty()) return

        viewModelScope.launch {
            when (val result = repository.requestTrashUnTrash(unsyncedItems, trash = true)) {
                is OperationResult.RequiresConsent -> {
                    _uiState.update {
                        it.copy(
                            pendingConsentRequest = PendingConsentRequest.Trash(
                                intentSender = result.intentSender,
                                items = unsyncedItems,
                                isTrashing = true
                            )
                        )
                    }
                }
                is OperationResult.Success -> {
                    markItemsAsSystemSynced(unsyncedItems)
                }
                is OperationResult.Error -> {
                    _uiState.update { it.copy(userMessage = result.message) }
                }
            }
        }
    }

    private fun markItemsAsSystemSynced(syncedItems: List<MediaItem>) {
        val syncedIds = syncedItems.map { it.id }.toSet()
        _uiState.update { state ->
            val updatedTrash = state.trashedItems.map { item ->
                if (item.id in syncedIds) item.copy(isSystemSynced = true) else item
            }
            state.copy(
                trashedItems = updatedTrash,
                userMessage = "${syncedItems.size} fotoğraf galeri çöp kutusuna işlendi."
            )
        }
    }

    /**
     * Restore a trashed item back to its original folder.
     */
    fun restoreItem(item: MediaItem) {
        if (item.isSystemSynced) {
            viewModelScope.launch {
                when (val result = repository.requestTrashUnTrash(listOf(item), trash = false)) {
                    is OperationResult.RequiresConsent -> {
                        _uiState.update {
                            it.copy(
                                pendingConsentRequest = PendingConsentRequest.Trash(
                                    intentSender = result.intentSender,
                                    items = listOf(item),
                                    isTrashing = false
                                )
                            )
                        }
                    }
                    is OperationResult.Success -> {
                        completeRestoreItem(item)
                    }
                    is OperationResult.Error -> {
                        _uiState.update { it.copy(userMessage = result.message) }
                    }
                }
            }
        } else {
            completeRestoreItem(item)
        }
    }

    private fun completeRestoreItem(item: MediaItem) {
        val currentState = _uiState.value
        val updatedTrash = currentState.trashedItems.filterNot { it.id == item.id }
        val restoredItem = item.copy(isTrashed = false, isSystemSynced = false)

        val updatedImages = if (currentState.selectedFolder?.bucketId == restoredItem.bucketId) {
            val list = currentState.images.toMutableList()
            list.add(currentState.currentIndex.coerceAtMost(list.size), restoredItem)
            list
        } else {
            currentState.images
        }

        _uiState.update {
            it.copy(
                trashedItems = updatedTrash,
                images = updatedImages,
                userMessage = "\"${item.name}\" klasörüne geri yüklendi."
            )
        }
    }

    /**
     * Quick Undo for last trashed item.
     */
    fun undoLastTrash() {
        val lastTrashed = _uiState.value.trashedItems.lastOrNull() ?: return
        restoreItem(lastTrashed)
    }

    fun openTrashSheet() {
        _uiState.update { it.copy(showTrashSheet = true) }
        syncPendingTrashToSystem()
    }

    fun closeTrashSheet() {
        _uiState.update { it.copy(showTrashSheet = false) }
    }

    fun requestPermanentDeleteConfirmation() {
        if (_uiState.value.trashedItems.isEmpty()) {
            _uiState.update { it.copy(userMessage = "Çöp kutusu boş.") }
            return
        }
        _uiState.update { it.copy(showDeleteConfirmDialog = true) }
    }

    fun dismissDeleteConfirmation() {
        _uiState.update { it.copy(showDeleteConfirmDialog = false) }
    }

    fun confirmPermanentDelete() {
        val trashed = _uiState.value.trashedItems
        if (trashed.isEmpty()) {
            dismissDeleteConfirmation()
            return
        }

        dismissDeleteConfirmation()

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            when (val result = repository.permanentDelete(trashed)) {
                is OperationResult.Success -> {
                    _uiState.update {
                        it.copy(
                            trashedItems = emptyList(),
                            isLoading = false,
                            userMessage = "${trashed.size} fotoğraf cihazdan kalıcı olarak silindi."
                        )
                    }
                }
                is OperationResult.RequiresConsent -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            pendingConsentRequest = PendingConsentRequest.Delete(
                                intentSender = result.intentSender,
                                items = trashed
                            )
                        )
                    }
                }
                is OperationResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            userMessage = "Silme işlemi başarısız: ${result.message}"
                        )
                    }
                }
            }
        }
    }

    fun onConsentResult(granted: Boolean) {
        val pending = _uiState.value.pendingConsentRequest
        _uiState.update { it.copy(pendingConsentRequest = null) }

        if (!granted) {
            _uiState.update { it.copy(userMessage = "İşlem izni reddedildi.") }
            return
        }

        when (pending) {
            is PendingConsentRequest.Trash -> {
                if (pending.isTrashing) {
                    markItemsAsSystemSynced(pending.items)
                } else {
                    pending.items.forEach { completeRestoreItem(it) }
                }
            }
            is PendingConsentRequest.Delete -> {
                _uiState.update {
                    it.copy(
                        trashedItems = emptyList(),
                        userMessage = "Seçilen fotoğraflar cihazdan kalıcı olarak silindi."
                    )
                }
            }
            null -> {}
        }
    }

    fun clearUserMessage() {
        _uiState.update { it.copy(userMessage = null) }
    }
}
