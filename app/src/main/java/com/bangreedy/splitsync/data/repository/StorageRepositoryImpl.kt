package com.bangreedy.splitsync.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.graphics.scale
import com.bangreedy.splitsync.domain.repository.StorageRepository
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.util.UUID

class StorageRepositoryImpl(
    private val storage: FirebaseStorage,
    private val context: Context
) : StorageRepository {

    override suspend fun uploadUserPhoto(uid: String, uri: Uri): String {
        return uploadImage("user_profiles/$uid/${UUID.randomUUID()}.jpg", uri)
    }

    override suspend fun uploadGroupPhoto(groupId: String, uri: Uri): String {
        return uploadImage("group_photos/$groupId/${UUID.randomUUID()}.jpg", uri)
    }

    private suspend fun uploadImage(path: String, uri: Uri): String {
        val jpegData = compressImage(uri)
        val ref = storage.reference.child(path)
        ref.putBytes(jpegData).await()
        return ref.downloadUrl.await().toString()
    }

    private suspend fun compressImage(uri: Uri): ByteArray = withContext(Dispatchers.IO) {
        val inputStream = context.contentResolver.openInputStream(uri)
        val bitmap = BitmapFactory.decodeStream(inputStream)
        inputStream?.close()

        val outputStream = ByteArrayOutputStream()
        // Resize if too large (e.g., max 1024x1024)
        val maxDimension = 1024
        var w = bitmap.width
        var h = bitmap.height
        if (w > maxDimension || h > maxDimension) {
            val ratio = w.toFloat() / h.toFloat()
            if (ratio > 1) {
                w = maxDimension
                h = (maxDimension / ratio).toInt()
            } else {
                h = maxDimension
                w = (maxDimension * ratio).toInt()
            }
        }
        val scaled = bitmap.scale(w, h, true)

        scaled.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
        outputStream.toByteArray()
    }
}
