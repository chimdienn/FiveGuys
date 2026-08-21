package com.example.data.storage

import android.content.Context
import com.example.data.repository.local.PhotoStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

/**
 * Stores photos in app-private storage.
 *
 * Files land under `filesDir/photos/{folder}/{uid}/`, mirroring the path layout used by
 * the Firebase Storage rules so that the two backends stay conceptually aligned. App
 * private storage means other apps cannot read a user's photos.
 */
class LocalPhotoStore(private val context: Context) : PhotoStore {

    override suspend fun save(uid: String, folder: String, bytes: ByteArray): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                val dir = File(context.filesDir, "photos/$folder/$uid").apply { mkdirs() }
                val file = File(dir, "${UUID.randomUUID()}.jpg")
                file.writeBytes(bytes)
                file.toURI().toString()
            }
        }
}
