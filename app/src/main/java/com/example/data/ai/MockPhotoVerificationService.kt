package com.example.data.ai

import com.example.domain.ai.PhotoVerificationService
import com.example.domain.ai.VerificationResult
import com.example.domain.model.Challenge
import kotlinx.coroutines.delay
import java.security.MessageDigest

/**
 * Deterministic photo verification for builds without AI credentials.
 *
 * Deliberately *not* a rubber stamp (spec section 48). The verdict is derived from a hash
 * of the image bytes and the challenge id, so:
 *
 *  - the same photo submitted against the same challenge always gives the same verdict,
 *    which makes the pass and fail paths reproducible in tests and demos; and
 *  - roughly one submission in four fails, so the failure UI is actually exercised
 *    during development rather than discovered in production.
 *
 * An empty or absurdly small image always fails — that is a real signal, not a coin toss.
 */
class MockPhotoVerificationService(
    private val artificialDelayMs: Long = 900L
) : PhotoVerificationService {

    override suspend fun verify(image: ByteArray, challenge: Challenge): VerificationResult {
        if (artificialDelayMs > 0) delay(artificialDelayMs)

        if (image.size < MIN_PLAUSIBLE_IMAGE_BYTES) {
            return VerificationResult(
                passed = false,
                confidence = 0.95f,
                explanation = "That image was too small to check. Try taking the photo again."
            )
        }

        val digest = MessageDigest.getInstance("SHA-256")
            .digest(image + challenge.id.toByteArray())
        // Unsigned value of the first byte, 0..255.
        val bucket = digest[0].toInt() and 0xFF
        val passed = bucket % 4 != 0
        val confidence = 0.55f + (bucket % 40) / 100f

        val subject = challenge.photoSubject ?: challenge.title
        return VerificationResult(
            passed = passed,
            confidence = confidence.coerceIn(0f, 1f),
            explanation = if (passed) {
                "Development verifier: this looks consistent with $subject."
            } else {
                "Development verifier: could not confirm $subject in this photo."
            }
        )
    }

    private companion object {
        /** Below this, the bytes cannot be a real camera frame. */
        const val MIN_PLAUSIBLE_IMAGE_BYTES = 1024
    }
}
