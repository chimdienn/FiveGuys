package com.example.data.ai

import android.util.Log
import com.example.domain.ai.SpeciesIdentification
import com.example.domain.ai.SpeciesIdentificationService
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.security.MessageDigest

/**
 * Offline species identification for builds without AI credentials.
 *
 * Returns a plausible entry from a small Victorian field-guide set, chosen deterministically
 * from the image bytes. Confidence is deliberately kept modest — a development stub that
 * claims 99% certainty would train both developers and users to trust a number that means
 * nothing.
 */
class MockSpeciesIdentificationService(
    private val artificialDelayMs: Long = 1_100L
) : SpeciesIdentificationService {

    override suspend fun identify(
        image: ByteArray?,
        textHint: String?,
        locationLabel: String?
    ): Result<SpeciesIdentification> {
        if (artificialDelayMs > 0) delay(artificialDelayMs)

        val seedBytes = image ?: (textHint ?: "biomate").toByteArray()
        val digest = MessageDigest.getInstance("SHA-256").digest(seedBytes)
        val index = (digest[0].toInt() and 0xFF) % CATALOGUE.size
        val entry = CATALOGUE[index]
        val confidence = 55 + ((digest[1].toInt() and 0xFF) % 35)

        return Result.success(entry.copy(confidence = confidence))
    }

    private companion object {
        /**
         * Every safety note here is phrased as a caution, never as an all-clear. The
         * service contract forbids telling a user something is safe to eat, touch or
         * approach (spec section 51), and the development data must not model otherwise.
         */
        val CATALOGUE = listOf(
            SpeciesIdentification(
                commonName = "Austral Grass-tree",
                scientificName = "Xanthorrhoea australis",
                category = "PLANT",
                confidence = 0,
                description = "A slow-growing plant with a dark trunk and a dense skirt of long, " +
                    "narrow leaves, often with a tall flowering spike.",
                habitat = "Dry sclerophyll forest and heathland across south-eastern Australia.",
                interestingFacts = listOf(
                    "Individual plants can be hundreds of years old.",
                    "Flowering is often triggered by fire.",
                    "The trunk is built from old leaf bases bound together with resin."
                ),
                isNative = true,
                safetyNote = "Leaf edges are sharp enough to cut skin. Do not handle the foliage."
            ),
            SpeciesIdentification(
                commonName = "Superb Fairywren",
                scientificName = "Malurus cyaneus",
                category = "BIRD",
                confidence = 0,
                description = "A very small bird with a long upright tail. Breeding males show " +
                    "bright blue on the crown and cheek.",
                habitat = "Dense low shrubs and undergrowth near open ground.",
                interestingFacts = listOf(
                    "They forage in small family groups.",
                    "Non-breeding males lose the blue plumage entirely.",
                    "They rarely fly far, preferring short hops between cover."
                ),
                isNative = true,
                safetyNote = "Observe from a distance. Do not feed native birds."
            ),
            SpeciesIdentification(
                commonName = "Fly Agaric",
                scientificName = "Amanita muscaria",
                category = "MUSHROOM",
                confidence = 0,
                description = "A striking mushroom with a red cap and white flecks, usually found " +
                    "near introduced pine and birch.",
                habitat = "Introduced conifer plantations and parkland.",
                interestingFacts = listOf(
                    "It is introduced to Australia, not native.",
                    "It forms a root partnership with certain trees.",
                    "The white flecks wash off in heavy rain."
                ),
                isNative = false,
                safetyNote = "Toxic. Do not touch, pick or eat. Fungi identification from a photo " +
                    "is unreliable and mistakes can be fatal."
            ),
            SpeciesIdentification(
                commonName = "Eastern Grey Kangaroo",
                scientificName = "Macropus giganteus",
                category = "ANIMAL",
                confidence = 0,
                description = "A large grey-brown macropod with a long, thick tail and a finely " +
                    "haired muzzle.",
                habitat = "Open grassy woodland and forest margins.",
                interestingFacts = listOf(
                    "Most active around dawn and dusk.",
                    "Groups are called mobs.",
                    "They use the tail as a fifth limb when moving slowly."
                ),
                isNative = true,
                safetyNote = "Keep well back. Large males can be dangerous, particularly if they " +
                    "feel cornered. Never approach or feed them."
            ),
            SpeciesIdentification(
                commonName = "Granite Tor",
                scientificName = "Granodiorite outcrop",
                category = "GEOLOGY",
                confidence = 0,
                description = "Rounded granite boulders exposed by weathering, often stacked into " +
                    "prominent outcrops.",
                habitat = "Ranges and uplands where granite bedrock reaches the surface.",
                interestingFacts = listOf(
                    "The rounding happens underground before the rock is exposed.",
                    "Granite cools slowly, which is why the crystals are visible.",
                    "Tors often mark the high points of a range."
                ),
                isNative = null,
                safetyNote = "Granite becomes very slippery when wet or lichen-covered. Take care " +
                    "near edges and drop-offs."
            ),
            SpeciesIdentification(
                commonName = "Common Wombat tracks",
                scientificName = "Vombatus ursinus",
                category = "TRACK",
                confidence = 0,
                description = "Broad, blunt footprints with five toes and heavy claw marks, often " +
                    "alongside a well-worn trail through vegetation.",
                habitat = "Forest and heath with diggable soil.",
                interestingFacts = listOf(
                    "Wombats maintain regular routes between burrows.",
                    "They are mainly nocturnal.",
                    "Their burrows can extend many metres underground."
                ),
                isNative = true,
                safetyNote = "Do not enter or disturb burrows. If you meet a wombat, give it room."
            )
        )
    }
}

/**
 * Species identification through Firebase AI Logic.
 *
 * As with photo verification, the request is proxied by Firebase rather than made with a
 * key embedded in the APK. The prompt explicitly forbids safety clearances: the model is
 * instructed to warn rather than reassure, because an incorrect "edible" from an image
 * classifier is the one failure mode in this app that could actually hurt someone.
 */
class GeminiSpeciesIdentificationService(
    private val fallback: SpeciesIdentificationService,
    private val modelName: String = DEFAULT_MODEL
) : SpeciesIdentificationService {

    override suspend fun identify(
        image: ByteArray?,
        textHint: String?,
        locationLabel: String?
    ): Result<SpeciesIdentification> = withContext(Dispatchers.IO) {
        try {
            val model = Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(modelName)

            val prompt = """
                Identify the natural subject shown, for a hiking field-guide app.
                ${textHint?.let { "The user described it as: \"$it\"." } ?: ""}
                ${locationLabel?.let { "Photographed near: $it." } ?: ""}

                Rules you must follow:
                - Never state that anything is safe to eat, drink, touch or approach.
                - If the subject could be confused with something dangerous, say so plainly.
                - If you are unsure, say you are unsure and lower the confidence.

                Reply with strictly valid JSON and nothing else:
                {
                  "commonName": "",
                  "scientificName": "",
                  "category": "PLANT|BIRD|MUSHROOM|ANIMAL|REPTILE|GEOLOGY|TRACK|OTHER",
                  "confidence": 0,
                  "description": "two or three sentences",
                  "habitat": "one sentence",
                  "interestingFacts": ["", "", ""],
                  "isNative": true,
                  "safetyNote": "a caution, never a clearance"
                }
            """.trimIndent()

            val response = model.generateContent(
                content {
                    if (image != null) inlineData(image, "image/jpeg")
                    text(prompt)
                }
            )

            parse(response.text)?.let { Result.success(it) }
                ?: fallback.identify(image, textHint, locationLabel)
        } catch (e: Exception) {
            Log.w(TAG, "Firebase AI identification unavailable, using offline field guide", e)
            fallback.identify(image, textHint, locationLabel)
        }
    }

    private fun parse(raw: String?): SpeciesIdentification? {
        val text = raw?.trim() ?: return null
        val start = text.indexOf('{')
        val end = text.lastIndexOf('}')
        if (start < 0 || end <= start) return null

        return runCatching {
            val json = JSONObject(text.substring(start, end + 1))
            val facts = json.optJSONArray("interestingFacts")
            SpeciesIdentification(
                commonName = json.optString("commonName").ifBlank { "Unidentified" },
                scientificName = json.optString("scientificName"),
                category = json.optString("category").ifBlank { "OTHER" },
                confidence = json.optInt("confidence", 50).coerceIn(0, 100),
                description = json.optString("description"),
                habitat = json.optString("habitat"),
                interestingFacts = buildList {
                    for (i in 0 until (facts?.length() ?: 0)) {
                        facts?.optString(i)?.takeIf { it.isNotBlank() }?.let { add(it) }
                    }
                },
                isNative = if (json.has("isNative")) json.optBoolean("isNative") else null,
                safetyNote = json.optString("safetyNote")
                    .ifBlank { "Treat any wild plant, fungus or animal as potentially harmful." }
            )
        }.getOrNull()
    }

    companion object {
        private const val TAG = "GeminiSpeciesId"
        const val DEFAULT_MODEL = "gemini-2.5-flash"
    }
}
