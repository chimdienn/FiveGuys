package com.example.data.auth

import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Password hashing for the local (no-Firebase) authentication backend.
 *
 * When Firebase Authentication is configured, Biomate never sees a password at all and
 * this class is unused. It exists so that a developer running the app without a Firebase
 * project still gets real credential handling instead of the prototype's plaintext
 * `password` column (spec section 8).
 *
 * PBKDF2-HMAC-SHA256 with a per-user 128-bit random salt. Verification is constant time.
 */
object PasswordHasher {

    private const val ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val KEY_LENGTH_BITS = 256
    private const val SALT_BYTES = 16

    /** Cost factor. High enough to be meaningful, low enough not to stall a debug login. */
    const val DEFAULT_ITERATIONS = 120_000

    private val random = SecureRandom()

    fun newSalt(): String {
        val bytes = ByteArray(SALT_BYTES)
        random.nextBytes(bytes)
        return bytes.toHex()
    }

    fun hash(password: String, saltHex: String, iterations: Int = DEFAULT_ITERATIONS): String {
        val spec = PBEKeySpec(password.toCharArray(), saltHex.fromHex(), iterations, KEY_LENGTH_BITS)
        return try {
            SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).encoded.toHex()
        } finally {
            spec.clearPassword()
        }
    }

    /**
     * Compares a candidate password against a stored hash.
     *
     * Uses a constant-time comparison so the duration of a failed login does not leak how
     * much of the hash matched.
     */
    fun verify(password: String, saltHex: String, expectedHash: String, iterations: Int): Boolean {
        val actual = hash(password, saltHex, iterations)
        return constantTimeEquals(actual, expectedHash)
    }

    private fun constantTimeEquals(a: String, b: String): Boolean {
        if (a.length != b.length) return false
        var diff = 0
        for (i in a.indices) diff = diff or (a[i].code xor b[i].code)
        return diff == 0
    }

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }

    private fun String.fromHex(): ByteArray =
        chunked(2).map { it.toInt(16).toByte() }.toByteArray()
}
