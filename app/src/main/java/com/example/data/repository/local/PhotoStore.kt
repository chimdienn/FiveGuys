package com.example.data.repository.local

/**
 * Where captured photos are persisted.
 *
 * Abstracted so that a build with Firebase uploads to Cloud Storage under a per-user path
 * while a local build writes to app-private files, without either the camera screen or
 * the challenge repository knowing which is in use (spec section 62).
 */
interface PhotoStore {
    /**
     * @param folder logical bucket such as `challenge-submissions` or `trail-moments`.
     * @return a URL or file URI that can be rendered, or a failure if the write failed.
     */
    suspend fun save(uid: String, folder: String, bytes: ByteArray): Result<String>
}
