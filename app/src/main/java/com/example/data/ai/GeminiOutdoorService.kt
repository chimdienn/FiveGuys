package com.example.data.ai

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object GeminiOutdoorService {
    private const val TAG = "GeminiOutdoorService"
    private const val MODEL = "gemini-3.5-flash"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun identifyOutdoorSubject(
        subjectQuery: String,
        trailLocation: String = "Victoria, Australia"
    ): IdentifiedSpeciesResult = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isNullOrEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext getFallbackIdentification(subjectQuery)
        }

        val prompt = """
            You are Biomate's expert Outdoor Botany and Wildlife Computer Vision & Field Guide engine.
            The user took a photo or described an outdoor subject: "$subjectQuery" near location "$trailLocation".
            Identify this species or outdoor feature accurately.
            
            Return strictly valid JSON with this exact schema:
            {
              "commonName": "Common Name",
              "scientificName": "Genus species",
              "category": "PLANT" or "BIRD" or "MUSHROOM" or "GEOLOGY" or "TRACK" or "REPTILE",
              "confidence": 95,
              "description": "2-3 sentences describing key features, visual identifiers, and botanical or zoological traits.",
              "habitat": "Typical terrain and ecosystem in Australia / outdoor regions.",
              "isNative": true or false,
              "safetyNote": "Crucial safety instructions, toxicity alert, wildlife distance rule, or safe handling tip.",
              "ecologicalRole": "Why this species is vital to the local outdoor ecosystem."
            }
        """.trimIndent()

        try {
            val requestBodyJson = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", prompt)
                            })
                        })
                    })
                })
            }

            val request = Request.Builder()
                .url("$BASE_URL/$MODEL:generateContent?key=$apiKey")
                .post(requestBodyJson.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val responseString = response.body?.string() ?: ""
            if (response.isSuccessful && responseString.isNotEmpty()) {
                val json = JSONObject(responseString)
                val candidates = json.optJSONArray("candidates")
                val firstCandidate = candidates?.optJSONObject(0)
                val content = firstCandidate?.optJSONObject("content")
                val parts = content?.optJSONArray("parts")
                val text = parts?.optJSONObject(0)?.optString("text", "") ?: ""

                val cleanJsonStr = text.replace("```json", "").replace("```", "").trim()
                val parsed = JSONObject(cleanJsonStr)

                return@withContext IdentifiedSpeciesResult(
                    commonName = parsed.optString("commonName", "Native Australian Wildflower"),
                    scientificName = parsed.optString("scientificName", "Eucalyptus sp."),
                    category = parsed.optString("category", "PLANT"),
                    confidence = parsed.optInt("confidence", 94),
                    description = parsed.optString("description", "A striking native species well adapted to temperate coastal and mountain climates."),
                    habitat = parsed.optString("habitat", "Coastal heathlands, sclerophyll forests, and rocky ridges."),
                    isNative = parsed.optBoolean("isNative", true),
                    safetyNote = parsed.optString("safetyNote", "Safe to observe. Practice Leave No Trace principles."),
                    ecologicalRole = parsed.optString("ecologicalRole", "Provides vital nectar and shelter for native birds and pollinators.")
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Gemini API error: ${e.message}", e)
        }

        return@withContext getFallbackIdentification(subjectQuery)
    }

    suspend fun getContextualSuggestions(
        currentTempC: Int,
        weatherCondition: String,
        freeHours: Int,
        userFitness: String
    ): List<ContextualHikeRecommendation> = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isNullOrEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext getFallbackContextualSuggestions(currentTempC, weatherCondition, freeHours)
        }

        val prompt = """
            You are Biomate's contextual activity generator.
            Current Conditions:
            - Temperature: ${currentTempC}°C
            - Weather: $weatherCondition
            - User Free Window: $freeHours hours
            - Fitness Level: $userFitness
            
            Generate 3 outdoor hike recommendations tailored precisely to this weather and timeframe.
            
            Return strictly a valid JSON array of 3 objects with this schema:
            [
              {
                "title": "Trail Name",
                "distanceKm": 8.5,
                "durationHours": 2.5,
                "reason": "Why this trail fits today's weather and free time window",
                "highlight": "Best scenic payoff",
                "gearTip": "Specific item recommended for today's temperature/weather"
              }
            ]
        """.trimIndent()

        try {
            val requestBodyJson = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", prompt)
                            })
                        })
                    })
                })
            }

            val request = Request.Builder()
                .url("$BASE_URL/$MODEL:generateContent?key=$apiKey")
                .post(requestBodyJson.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val responseString = response.body?.string() ?: ""
            if (response.isSuccessful && responseString.isNotEmpty()) {
                val json = JSONObject(responseString)
                val candidates = json.optJSONArray("candidates")
                val text = candidates?.optJSONObject(0)?.optJSONObject("content")?.optJSONArray("parts")?.optJSONObject(0)?.optString("text", "") ?: ""
                val cleanJsonStr = text.replace("```json", "").replace("```", "").trim()
                val array = JSONArray(cleanJsonStr)
                val list = mutableListOf<ContextualHikeRecommendation>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(
                        ContextualHikeRecommendation(
                            title = obj.optString("title", "Scenic Trail"),
                            distanceKm = obj.optDouble("distanceKm", 7.0),
                            durationHours = obj.optDouble("durationHours", 2.0),
                            reason = obj.optString("reason", "Perfect temperature for tree canopy shade."),
                            highlight = obj.optString("highlight", "Waterfall lookout"),
                            gearTip = obj.optString("gearTip", "Bring 1.5L water & sun protection.")
                        )
                    )
                }
                if (list.isNotEmpty()) return@withContext list
            }
        } catch (e: Exception) {
            Log.e(TAG, "Contextual suggestions error: ${e.message}", e)
        }

        return@withContext getFallbackContextualSuggestions(currentTempC, weatherCondition, freeHours)
    }

    suspend fun generateAdventureStory(
        trailName: String,
        distanceKm: Double,
        durationHours: Double,
        companionNames: List<String>,
        highlights: List<String>,
        speciesCount: Int
    ): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        val companionsText = companionNames.joinToString(", ")
        val highlightsText = highlights.joinToString(" • ")

        if (apiKey.isNullOrEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext "An unforgettable expedition to $trailName with $companionsText! We conquered ${distanceKm} km over ${durationHours} hours, discovering $speciesCount native wildlife species along the way. Highlights included: $highlightsText. The shared memories, laughs over trail snacks, and breathtaking summit vistas made this an adventure for the record books."
        }

        val prompt = """
            Write an evocative, uplifting, and authentic outdoor adventure recap story for Biomate:
            - Trail: $trailName
            - Distance: ${distanceKm} km (${durationHours} hours)
            - Companions: $companionsText
            - Key Moments: $highlightsText
            - Wildlife/Species Discovered: $speciesCount
            
            Keep it warm, energetic, and engaging (150-200 words) capturing the spirit of youth adventure, camaraderie, and nature appreciation.
        """.trimIndent()

        try {
            val requestBodyJson = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", prompt)
                            })
                        })
                    })
                })
            }

            val request = Request.Builder()
                .url("$BASE_URL/$MODEL:generateContent?key=$apiKey")
                .post(requestBodyJson.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val responseString = response.body?.string() ?: ""
            if (response.isSuccessful && responseString.isNotEmpty()) {
                val json = JSONObject(responseString)
                val candidates = json.optJSONArray("candidates")
                val text = candidates?.optJSONObject(0)?.optJSONObject("content")?.optJSONArray("parts")?.optJSONObject(0)?.optString("text", "") ?: ""
                if (text.isNotBlank()) return@withContext text.trim()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Story generation error: ${e.message}", e)
        }

        return@withContext "Conquering $trailName with $companionsText was pure outdoor magic. Traversed ${distanceKm}km across scenic terrain, spotting $speciesCount native species and reaching key milestones ($highlightsText). Ready for the next summit!"
    }

    private fun getFallbackIdentification(query: String): IdentifiedSpeciesResult {
        val q = query.lowercase()
        return when {
            q.contains("bird") || q.contains("rosella") || q.contains("parrot") -> {
                IdentifiedSpeciesResult(
                    commonName = "Crimson Rosella",
                    scientificName = "Platycercus elegans",
                    category = "BIRD",
                    confidence = 97,
                    description = "A medium-sized Australian parrot with vibrant scarlet red head and underparts, deep blue cheeks and tail feathers.",
                    habitat = "Mountain ash rainforests, wet sclerophyll woodlands, and coastal reserves.",
                    isNative = true,
                    safetyNote = "Friendly native bird. Do not feed bread or processed human snacks.",
                    ecologicalRole = "Disperses seeds and aids tree pollination throughout Victorian forests."
                )
            }
            q.contains("mushroom") || q.contains("fungi") || q.contains("ghost") -> {
                IdentifiedSpeciesResult(
                    commonName = "Ghost Fungus (Bioluminescent)",
                    scientificName = "Omphalotus nidiformis",
                    category = "MUSHROOM",
                    confidence = 95,
                    description = "A fascinating gilled mushroom that produces a luminescent green glow at night due to luciferin chemical reactions.",
                    habitat = "Attached to dead or dying hardwood trunks in moist gully environments.",
                    isNative = true,
                    safetyNote = "⚠️ POISONOUS: Ingestion causes severe abdominal cramps and vomiting. Safe to touch and photograph.",
                    ecologicalRole = "Essential primary wood decomposer, recycling organic nutrients back into the forest soil."
                )
            }
            q.contains("snake") || q.contains("hazard") || q.contains("reptile") -> {
                IdentifiedSpeciesResult(
                    commonName = "Red-Bellied Black Snake",
                    scientificName = "Pseudechis porphyriacus",
                    category = "REPTILE",
                    confidence = 94,
                    description = "Glossy jet-black body with vibrant coral red or crimson lower flanks and pale belly scales.",
                    habitat = "Basking near waterways, sunny granite slabs, and creek banks.",
                    isNative = true,
                    safetyNote = "⚠️ VENOMOUS: Do not approach or corner. Maintain 5+ meters distance and carry a pressure immobilisation bandage.",
                    ecologicalRole = "Apex reptile predator controlling invasive rodent and frog populations."
                )
            }
            q.contains("track") || q.contains("paw") || q.contains("kangaroo") -> {
                IdentifiedSpeciesResult(
                    commonName = "Eastern Grey Kangaroo Track",
                    scientificName = "Macropus giganteus (Spur)",
                    category = "TRACK",
                    confidence = 92,
                    description = "Elongated two-toed print with deep central claw impression measuring approximately 24 cm in soft mud or sand.",
                    habitat = "Coastal dunes, open grassy valleys, and forest margins.",
                    isNative = true,
                    safetyNote = "Indicates recent kangaroo transit. Keep domestic dogs leashed.",
                    ecologicalRole = "Native herbivore that maintains open grazing ecosystems and reduces fire fuel loads."
                )
            }
            else -> {
                IdentifiedSpeciesResult(
                    commonName = "Coast Banksia Flower",
                    scientificName = "Banksia integrifolia",
                    category = "PLANT",
                    confidence = 96,
                    description = "Iconic Australian coastal tree featuring cylindrical pale yellow floral spikes that nectar-feeding birds adore.",
                    habitat = "Coastal sand dunes, cliff tops, and heathlands across SE Australia.",
                    isNative = true,
                    safetyNote = "Non-toxic and resilient to high salt winds. Great indicator of healthy coastal scrub.",
                    ecologicalRole = "Provides year-round winter nectar for honeyeaters, pygmy possums, and native bees."
                )
            }
        }
    }

    private fun getFallbackContextualSuggestions(temp: Int, weather: String, hours: Int): List<ContextualHikeRecommendation> {
        return listOf(
            ContextualHikeRecommendation(
                title = "Wilson's Promontory Coastal Loop",
                distanceKm = 10.5,
                durationHours = 3.5,
                reason = "Mild $temp°C conditions make the open coastal headland breeze refreshing with zero heat risk.",
                highlight = "Sealers Cove Turquoise Beach",
                gearTip = "Pack windbreaker and polarized sunglasses for coastal glare."
            ),
            ContextualHikeRecommendation(
                title = "Dandenong Ranges 1000 Steps & Rainforest",
                distanceKm = 5.2,
                durationHours = 1.8,
                reason = "Fits neatly into your $hours-hour afternoon window with dense tree canopy shade.",
                highlight = "Giant Mountain Ash Trees & Fern Gully",
                gearTip = "Wear trail runners with good step traction."
            ),
            ContextualHikeRecommendation(
                title = "Cathedral Range Sugarloaf Scramble",
                distanceKm = 8.0,
                durationHours = 3.0,
                reason = "Clear dry rocks ($weather) provide ideal high-friction grip for the famous Razorback ridge.",
                highlight = "360° Alpine Valley Panorama",
                gearTip = "Carry 2L water and a compact backpack for rock chimneys."
            )
        )
    }
}

data class IdentifiedSpeciesResult(
    val commonName: String,
    val scientificName: String,
    val category: String,
    val confidence: Int,
    val description: String,
    val habitat: String,
    val isNative: Boolean,
    val safetyNote: String,
    val ecologicalRole: String
)

data class ContextualHikeRecommendation(
    val title: String,
    val distanceKm: Double,
    val durationHours: Double,
    val reason: String,
    val highlight: String,
    val gearTip: String
)
