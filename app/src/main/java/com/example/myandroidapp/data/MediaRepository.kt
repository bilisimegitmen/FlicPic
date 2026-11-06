package com.example.myandroidapp.data

import android.app.RecoverableSecurityException
import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repository responsible for querying media collections and executing file/trash operations
 * via Android's MediaStore API with batch processing support.
 */
class MediaRepository(private val context: Context) {

    private val contentResolver: ContentResolver = context.contentResolver

    /**
     * Queries all image albums / folders currently indexed on the device.
     */
    suspend fun getFolders(): List<FolderItem> = withContext(Dispatchers.IO) {
        val folderMap = linkedMapOf<String, FolderItem>()
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.BUCKET_ID,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Images.Media.DATE_ADDED
        )
        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

        try {
            contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                sortOrder
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val bucketIdCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_ID)
                val bucketNameCol = cursor.getColumnIndex(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val bucketId = cursor.getString(bucketIdCol) ?: "unknown"
                    val bucketName = if (bucketNameCol != -1) {
                        cursor.getString(bucketNameCol) ?: "Galeri"
                    } else {
                        "Galeri"
                    }
                    val uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)

                    val existing = folderMap[bucketId]
                    if (existing == null) {
                        folderMap[bucketId] = FolderItem(
                            bucketId = bucketId,
                            name = bucketName,
                            coverUri = uri,
                            count = 1
                        )
                    } else {
                        existing.count++
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        folderMap.values.sortedByDescending { it.count }
    }

    /**
     * Retrieves all non-trashed media items belonging to a given folder bucket.
     */
    suspend fun getImagesForFolder(bucketId: String): List<MediaItem> = withContext(Dispatchers.IO) {
        val items = mutableListOf<MediaItem>()
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.BUCKET_ID,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.DATE_ADDED
        )
        val selection = "${MediaStore.Images.Media.BUCKET_ID} = ?"
        val selectionArgs = arrayOf(bucketId)
        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

        try {
            contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                val bucketIdCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_ID)
                val bucketNameCol = cursor.getColumnIndex(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
                val dateAddedCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val name = cursor.getString(nameCol) ?: "Resim_$id"
                    val bId = cursor.getString(bucketIdCol) ?: bucketId
                    val bName = if (bucketNameCol != -1) cursor.getString(bucketNameCol) ?: "" else ""
                    val size = cursor.getLong(sizeCol)
                    val dateAdded = cursor.getLong(dateAddedCol)
                    val uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)

                    items.add(
                        MediaItem(
                            id = id,
                            uri = uri,
                            name = name,
                            bucketId = bId,
                            bucketName = bName,
                            size = size,
                            dateAdded = dateAdded,
                            isTrashed = false,
                            isSystemSynced = false
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        items
    }

    /**
     * Queries MediaStore for images marked as trashed in System Phone Gallery.
     */
    suspend fun getSystemTrashedImages(): List<MediaItem> = withContext(Dispatchers.IO) {
        val items = mutableListOf<MediaItem>()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return@withContext items

        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.BUCKET_ID,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.DATE_ADDED
        )

        try {
            val queryArgs = Bundle().apply {
                putInt("android:query-arg-match-trashed", 1)
                putString(ContentResolver.QUERY_ARG_SQL_SELECTION, "${MediaStore.MediaColumns.IS_TRASHED} = 1")
                putStringArray(ContentResolver.QUERY_ARG_SORT_COLUMNS, arrayOf(MediaStore.Images.Media.DATE_ADDED))
                putInt(ContentResolver.QUERY_ARG_SORT_DIRECTION, ContentResolver.QUERY_SORT_DIRECTION_DESCENDING)
            }

            contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                queryArgs,
                null
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                val bucketIdCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_ID)
                val bucketNameCol = cursor.getColumnIndex(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
                val dateAddedCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val name = cursor.getString(nameCol) ?: "Resim_$id"
                    val bId = cursor.getString(bucketIdCol) ?: ""
                    val bName = if (bucketNameCol != -1) cursor.getString(bucketNameCol) ?: "" else ""
                    val size = cursor.getLong(sizeCol)
                    val dateAdded = cursor.getLong(dateAddedCol)
                    val uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)

                    items.add(
                        MediaItem(
                            id = id,
                            uri = uri,
                            name = name,
                            bucketId = bId,
                            bucketName = bName,
                            size = size,
                            dateAdded = dateAdded,
                            isTrashed = true,
                            isSystemSynced = true
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        items
    }

    /**
     * Requests moving a batch of items to/from System Phone Gallery Trash Bin via MediaStore.createTrashRequest (API 30+).
     */
    suspend fun requestTrashUnTrash(items: List<MediaItem>, trash: Boolean): OperationResult = withContext(Dispatchers.IO) {
        if (items.isEmpty()) return@withContext OperationResult.Success
        val uris = items.map { it.uri }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val pendingIntent = MediaStore.createTrashRequest(contentResolver, uris, trash)
                OperationResult.RequiresConsent(pendingIntent.intentSender)
            } catch (e: Exception) {
                OperationResult.Error(e.localizedMessage ?: "Çöp işlem isteği oluşturulamadı.")
            }
        } else {
            // Android 10 and lower fallback
            OperationResult.Success
        }
    }

    /**
     * Permanently deletes a batch of items from storage via MediaStore.createDeleteRequest (API 30+).
     */
    suspend fun permanentDelete(items: List<MediaItem>): OperationResult = withContext(Dispatchers.IO) {
        if (items.isEmpty()) return@withContext OperationResult.Success
        val uris = items.map { it.uri }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val pendingIntent = MediaStore.createDeleteRequest(contentResolver, uris)
                OperationResult.RequiresConsent(pendingIntent.intentSender)
            } catch (e: Exception) {
                OperationResult.Error(e.localizedMessage ?: "Kalıcı silme isteği oluşturulamadı.")
            }
        } else {
            try {
                for (item in items) {
                    contentResolver.delete(item.uri, null, null)
                }
                OperationResult.Success
            } catch (securityException: SecurityException) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                    securityException is RecoverableSecurityException
                ) {
                    OperationResult.RequiresConsent(securityException.userAction.actionIntent.intentSender)
                } else {
                    OperationResult.Error(securityException.localizedMessage ?: "Silme izni reddedildi.")
                }
            } catch (e: Exception) {
                OperationResult.Error(e.localizedMessage ?: "Bilinmeyen silme hatası.")
            }
        }
    }
}
