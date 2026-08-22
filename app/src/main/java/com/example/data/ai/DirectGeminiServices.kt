package com.example.data.ai

import android.util.Base64
import android.util.Log
import com.example.domain.ai.PhotoVerificationService
import com.example.domain.ai.SpeciesIdentification
import com.example.domain.ai.SpeciesIdentificationService
import com.example.domain.ai.VerificationResult
import com.example.domain.model.Challenge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

/**
 * Direct Google Gemini REST client used by the hackathon build.
 *
 * The API key is supplied from BuildConfig by AppContainer. The real key belongs in the
 * git-ignored `.env`/`local.properties`; never hard-code it in this source file.
 *
 * The camera path intentionally uses a longer timeout than OkHttp's defaults. Vision
 * requests can take more than ten seconds on a mobile connection, especially when the
 * first request has to warm the model. The old ten-second read timeout was one reason the
 * app could silently fall back to the offline catalogue even with a valid key.
 */
internal class GeminiApiClient(
    private val apiKey: String,
    private val modelName: String = DEFAULT_MODEL,
    private val http: OkHttpClient = defaultHttpClient()
) {
    suspend fun generateJson(
        prompt: String,
        image: ByteArray? = null,
        imageMimeType: String = "image/jpeg"
    ): String = withContext(Dispatchers.IO) {
        require(apiKey.isNotBlank()) { "Gemini API key is missing." }

        val parts = JSONArray()
        // Put the image first. This mirrors Google's current multimodal examples and keeps
        // the visual input adjacent to the instruction that follows it.
        if (image != null) {
            parts.put(
                JSONObject().put(
                    "inlineData",
                    JSONObject()
                        .put("mimeType", imageMimeType)
                        .put("data", Base64.encodeToString(image, Base64.NO_WRAP))
                )
            )
        }
        parts.put(JSONObject().put("text", prompt))

        val payload = JSONObject()
            .put(
                "contents",
                JSONArray().put(
                    JSONObject()
                        .put("role", "user")
                        .put("parts", parts)
                )
            )
            .put(
                "generationConfig",
                JSONObject()
                    .put("maxOutputTokens", 1200)
                    .put("responseMimeType", "application/json")
            )

        var lastError: IOException? = null
        repeat(MAX_ATTEMPTS) { attempt ->
            try {
                return@withContext execute(payload)
            } catch (error: RetryableGeminiException) {
                lastError = error
                if (attempt < MAX_ATTEMPTS - 1) delay(RETRY_DELAY_MS * (attempt + 1))
            }
        }
        throw lastError ?: IOException("Gemini request failed.")
    }

    private fun execute(payload: JSONObject): String {
        val request = Request.Builder()
            .url("$BASE_URL/models/$modelName:generateContent")
            .header("x-goog-api-key", apiKey)
            .header("Accept", "application/json")
            .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
            .build()

        http.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                val apiMessage = runCatching {
                    JSONObject(body).optJSONObject("error")?.optString("message")
                }.getOrNull().orEmpty()
                val message = apiMessage.ifBlank {
                    "Gemini request failed with HTTP ${response.code}."
                }
                if (response.code == 429 || response.code in 500..599) {
                    throw RetryableGeminiException(message)
                }
                throw IOException(message)
            }

            return extractText(body) ?: throw IOException(explainEmptyResponse(body))
        }
    }

    private fun extractText(raw: String): String? {
        val candidates = JSONObject(raw).optJSONArray("candidates") ?: return null
        for (candidateIndex in 0 until candidates.length()) {
            val parts = candidates.optJSONObject(candidateIndex)
                ?.optJSONObject("content")
                ?.optJSONArray("parts")
                ?: continue
            for (partIndex in 0 until parts.length()) {
                val text = parts.optJSONObject(partIndex)?.optString("text")
                if (!text.isNullOrBlank()) return text
            }
        }
        return null
    }

    private fun explainEmptyResponse(raw: String): String {
        return runCatching {
            val root = JSONObject(raw)
            val candidate = root.optJSONArray("candidates")?.optJSONObject(0)
            val finishReason = candidate?.optString("finishReason").orEmpty()
            val blockReason = root.optJSONObject("promptFeedback")?.optString("blockReason").orEmpty()
            when {
                blockReason.isNotBlank() -> "Gemini blocked the request: $blockReason."
                finishReason.isNotBlank() -> "Gemini returned no identification ($finishReason)."
                else -> "Gemini returned no usable response."
            }
        }.getOrDefault("Gemini returned no usable response.")
    }

    private class RetryableGeminiException(message: String) : IOException(message)

    companion object {
        // Gemini 3.6 Flash supports image input and structured JSON output.
        // Do not send a 2.5-style numeric thinkingBudget to Gemini 3.x; it expects
        // thinkingLevel instead, and an invalid thinking config causes HTTP 400.
        const val DEFAULT_MODEL = "gemini-3.6-flash"
        private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta"
        private const val MAX_ATTEMPTS = 2
        private const val RETRY_DELAY_MS = 900L
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

        private fun defaultHttpClient() = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .readTimeout(75, TimeUnit.SECONDS)
            .callTimeout(90, TimeUnit.SECONDS)
            .build()
    }
}

/** Verifies BioCoin challenge photos using the team's Google AI Studio Gemini key. */
class DirectGeminiPhotoVerificationService(
    apiKey: String,
    private val fallback: PhotoVerificationService,
    modelName: String = GeminiApiClient.DEFAULT_MODEL
) : PhotoVerificationService {
    private val client = GeminiApiClient(apiKey = apiKey, modelName = modelName)

    override suspend fun verify(image: ByteArray, challenge: Challenge): VerificationResult {
        val subject = challenge.photoSubject ?: challenge.title
        val prompt = """
            You are verifying a photo submitted for an outdoor challenge in Biomate.
            The challenge requires a photo of: $subject

            Examine the actual image, not only the wording of the challenge. Decide whether
            the main visible subject plausibly satisfies the requirement. Be friendly rather
            than overly strict, but reject clearly unrelated or unusable photos.

            Return exactly one JSON object:
            {"passed": true, "confidence": 0.0, "explanation": "one short sentence"}

            `confidence` must be a number from 0.0 to 1.0.
        """.trimIndent()

        return try {
            parseVerification(client.generateJson(prompt = prompt, image = image))
                ?: fallback.verify(image, challenge)
        } catch (error: Exception) {
            // Challenge submissions already have a deterministic development fallback so a
            // temporary API outage does not strand an immutable challenge submission.
            Log.w(TAG_VERIFY, "Direct Gemini verification unavailable; using fallback", error)
            fallback.verify(image, challenge)
        }
    }

    private fun parseVerification(raw: String): VerificationResult? {
        val json = extractJsonObject(raw) ?: return null
        return runCatching {
            val rawConfidence = json.optDouble("confidence", 0.5)
            VerificationResult(
                passed = json.optBoolean("passed", false),
                confidence = (if (rawConfidence > 1.0) rawConfidence / 100.0 else rawConfidence)
                    .toFloat()
                    .coerceIn(0f, 1f),
                explanation = json.optString("explanation")
                    .ifBlank { "No explanation provided." }
            )
        }.getOrNull()
    }
}

/**
 * Uses Gemini Vision for Biomate's Explore camera mode.
 *
 * When a real API key is configured, this service no longer silently swaps in a random
 * offline catalogue entry after a network/authentication failure. The UI will show the
 * real Gemini error instead, which makes configuration problems visible and prevents a
 * failed cloud request from looking like a confident AI identification.
 */
class DirectGeminiSpeciesIdentificationService(
    apiKey: String,
    modelName: String = GeminiApiClient.DEFAULT_MODEL
) : SpeciesIdentificationService {
    private val client = GeminiApiClient(apiKey = apiKey, modelName = modelName)

    override suspend fun identify(
        image: ByteArray?,
        textHint: String?,
        locationLabel: String?
    ): Result<SpeciesIdentification> {
        if (image == null || image.isEmpty()) {
            return Result.failure(IllegalArgumentException("No camera image was available to identify."))
        }

        val prompt = """
            Analyse the attached camera photo for Biomate, an outdoor field-guide app.
            Identify the MAIN VISIBLE SUBJECT in the image. It may be a plant, flower, tree,
            bird, animal, insect, mushroom, track, rock/geological feature, trail sign,
            outdoor gear, hazard, or another outdoor object. Do not force a nature species
            identification when the image clearly shows something else.
            ${textHint?.let { "The user described it as: \"$it\"." } ?: ""}
            ${locationLabel?.let { "Approximate photo location: $it." } ?: ""}

            Rules:
            - Base the answer on what is actually visible in the photo.
            - If species-level identification is uncertain, use a broader common name and
              lower the confidence instead of guessing.
            - Never state that anything is safe to eat, drink, touch, pick or approach.
            - If it could be confused with something dangerous, say so plainly.
            - Use Australian/Victorian context only when it is supported by the image or
              supplied location, not as a reason to invent a local species.
            - `confidence` MUST be an integer from 0 to 100.
            - For a non-living object, `scientificName` should be an empty string and
              `isNative` should be null.

            Return exactly one JSON object:
            {
              "commonName": "",
              "scientificName": "",
              "category": "PLANT|BIRD|MUSHROOM|ANIMAL|REPTILE|GEOLOGY|TRACK|OBJECT|SIGN|OTHER",
              "confidence": 0,
              "description": "two or three concise sentences describing visible evidence",
              "habitat": "one concise sentence, or empty for ordinary objects",
              "interestingFacts": ["", "", ""],
              "isNative": null,
              "safetyNote": "a relevant caution; never a safety clearance"
            }
        """.trimIndent()

        return try {
            val parsed = parseIdentification(
                client.generateJson(prompt = prompt, image = image)
            ) ?: return Result.failure(IOException("Gemini returned JSON that Biomate could not read."))
            Result.success(parsed)
        } catch (error: Exception) {
            Log.e(TAG_IDENTIFY, "Live Gemini identification failed", error)
            Result.failure(
                IOException(
                    "Gemini identification failed: ${error.message ?: "unknown API error"}",
                    error
                )
            )
        }
    }

    private fun parseIdentification(raw: String): SpeciesIdentification? {
        val json = extractJsonObject(raw) ?: return null
        val facts = json.optJSONArray("interestingFacts")
        return runCatching {
            val rawConfidence = json.optDouble("confidence", 50.0)
            val confidence = if (rawConfidence in 0.0..1.0) {
                (rawConfidence * 100.0).roundToInt()
            } else {
                rawConfidence.roundToInt()
            }.coerceIn(0, 100)

            SpeciesIdentification(
                commonName = json.optString("commonName").ifBlank { "Unidentified subject" },
                scientificName = json.optString("scientificName"),
                category = json.optString("category").ifBlank { "OTHER" },
                confidence = confidence,
                description = json.optString("description")
                    .ifBlank { "Gemini could not provide a detailed description." },
                habitat = json.optString("habitat"),
                interestingFacts = buildList {
                    for (i in 0 until (facts?.length() ?: 0)) {
                        facts?.optString(i)?.takeIf { it.isNotBlank() }?.let(::add)
                    }
                },
                isNative = when {
                    !json.has("isNative") || json.isNull("isNative") -> null
                    else -> json.optBoolean("isNative")
                },
                safetyNote = json.optString("safetyNote")
                    .ifBlank { "AI identification can be wrong. Verify important findings independently." }
            )
        }.getOrNull()
    }
}

private fun extractJsonObject(raw: String?): JSONObject? {
    val text = raw?.trim() ?: return null
    val start = text.indexOf('{')
    val end = text.lastIndexOf('}')
    if (start < 0 || end <= start) return null
    return runCatching { JSONObject(text.substring(start, end + 1)) }.getOrNull()
}

private const val TAG_VERIFY = "GeminiDirectVerify"
private const val TAG_IDENTIFY = "GeminiDirectIdentify"
