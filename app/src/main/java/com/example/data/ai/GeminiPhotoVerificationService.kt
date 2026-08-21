package com.example.data.ai

import android.util.Log
import com.example.domain.ai.PhotoVerificationService
import com.example.domain.ai.VerificationResult
import com.example.domain.model.Challenge
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * Photo verification through Firebase AI Logic.
 *
 * Routed via the Firebase AI SDK rather than a raw `generativelanguage.googleapis.com`
 * call with an embedded key, which is what the original prototype did. That matters: a
 * key compiled into an APK is extractable by anyone who downloads it. Firebase AI Logic
 * proxies the request server-side and is gated by App Check, so the client never holds a
 * privileged credential (spec section 49).
 *
 * Falls back to the caller's error handling on any failure — a verification outage must
 * not be indistinguishable from a failed verification.
 */
class GeminiPhotoVerificationService(
    private val fallback: PhotoVerificationService,
    private val modelName: String = DEFAULT_MODEL
) : PhotoVerificationService {

    override suspend fun verify(image: ByteArray, challenge: Challenge): VerificationResult =
        withContext(Dispatchers.IO) {
            val subject = challenge.photoSubject ?: challenge.title
            try {
                val model = Firebase.ai(backend = GenerativeBackend.googleAI())
                    .generativeModel(modelName)

                val prompt = """
                    You are verifying a photo submitted for an outdoor challenge.
                    The challenge requires a photo of: $subject

                    Decide whether the image plausibly shows that subject. Be reasonable
                    rather than strict — this is a friendly outdoors app, not an exam.

                    Reply with strictly valid JSON and nothing else:
                    {"passed": true or false, "confidence": 0.0 to 1.0, "explanation": "one short sentence"}
                """.trimIndent()

                val response = model.generateContent(
                    content {
                        inlineData(image, "image/jpeg")
                        text(prompt)
                    }
                )

                parse(response.text) ?: fallback.verify(image, challenge)
            } catch (e: Exception) {
                Log.w(TAG, "Firebase AI verification unavailable, using development verifier", e)
                fallback.verify(image, challenge)
            }
        }

    private fun parse(raw: String?): VerificationResult? {
        val text = raw?.trim()?.removeSurrounding("```json", "```")?.removeSurrounding("```")?.trim()
        if (text.isNullOrEmpty()) return null
        val start = text.indexOf('{')
        val end = text.lastIndexOf('}')
        if (start < 0 || end <= start) return null

        return runCatching {
            val json = JSONObject(text.substring(start, end + 1))
            VerificationResult(
                passed = json.optBoolean("passed", false),
                confidence = json.optDouble("confidence", 0.5).toFloat().coerceIn(0f, 1f),
                explanation = json.optString("explanation").ifBlank { "No explanation provided." }
            )
        }.getOrNull()
    }

    companion object {
        private const val TAG = "GeminiPhotoVerify"
        const val DEFAULT_MODEL = "gemini-2.5-flash"
    }
}
