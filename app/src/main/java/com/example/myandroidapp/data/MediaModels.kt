package com.example.myandroidapp.data

import android.content.IntentSender
import android.net.Uri

data class MediaItem(
    val id: Long,
    val uri: Uri,
    val name: String,
    val bucketId: String,
    val bucketName: String,
    val size: Long,
    val dateAdded: Long,
    val isTrashed: Boolean = false,
    val isSystemSynced: Boolean = false
)

data class FolderItem(
    val bucketId: String,
    val name: String,
    val coverUri: Uri?,
    var count: Int
)

sealed interface PendingConsentRequest {
    val intentSender: IntentSender

    data class Trash(
        override val intentSender: IntentSender,
        val items: List<MediaItem>,
        val isTrashing: Boolean
    ) : PendingConsentRequest

    data class Delete(
        override val intentSender: IntentSender,
        val items: List<MediaItem>
    ) : PendingConsentRequest
}

sealed interface OperationResult {
    data object Success : OperationResult
    data class RequiresConsent(val intentSender: IntentSender) : OperationResult
    data class Error(val message: String) : OperationResult
}
