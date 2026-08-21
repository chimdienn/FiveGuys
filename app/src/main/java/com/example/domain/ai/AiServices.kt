package com.example.domain.ai

import com.example.domain.model.Challenge

/**
 * Verifies that a photo actually shows what a challenge asked for.
 *
 * Behind an interface so the implementation can change (spec section 47), and so that a
 * development build with no AI credentials still exercises both the pass and the fail
 * path rather than rubber-stamping everything.
 */
interface PhotoVerificationService {
    suspend fun verify(image: ByteArray, challenge: Challenge): VerificationResult
}

data class VerificationResult(
    val passed: Boolean,
    /** 0.0..1.0 */
    val confidence: Float,
    val explanation: String
)

/**
 * Identifies a plant, animal, fungus or feature from a photo.
 *
 * Every result carries [safetyNote] and [uncertaintyNote]. The service contract is that
 * an identification is a *guess*: it must never be presented, or generated, as advice
 * that something is safe to eat, drink, touch or approach (spec section 51).
 */
interface SpeciesIdentificationService {
    suspend fun identify(image: ByteArray?, textHint: String?, locationLabel: String?): Result<SpeciesIdentification>
}

data class SpeciesIdentification(
    val commonName: String,
    val scientificName: String,
    val category: String,
    /** 0..100 */
    val confidence: Int,
    val description: String,
    val habitat: String,
    val interestingFacts: List<String>,
    val isNative: Boolean?,
    val safetyNote: String
) {
    /**
     * The disclaimer shown alongside every identification, without exception.
     *
     * Not optional and not dismissible: a misidentified fungus or berry can kill someone,
     * and the model's confidence number is not a substitute for an expert.
     */
    val uncertaintyNote: String
        get() = "AI identification can be wrong. Never eat, drink, touch or approach anything " +
            "based on this result — confirm with an expert or an official field guide first."
}
