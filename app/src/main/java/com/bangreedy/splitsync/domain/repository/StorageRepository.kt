package com.bangreedy.splitsync.domain.repository

import android.net.Uri

interface StorageRepository {
    suspend fun uploadUserPhoto(uid: String, uri: Uri): String
    suspend fun uploadGroupPhoto(groupId: String, uri: Uri): String
}

