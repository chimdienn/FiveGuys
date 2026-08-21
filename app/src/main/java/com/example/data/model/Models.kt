package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trails")
data class Trail(
    @PrimaryKey val id: String,
    val title: String,
    val region: String,
    val stateOrCountry: String,
    val distanceKm: Double,
    val elevationGainM: Int,
    val durationHours: Double,
    val difficulty: String, // "Easy", "Moderate", "Hard", "Challenging"
    val terrain: String, // "Forest & Coastal", "Granite Peaks", "Rainforest Steps", "Alpine Ridge"
    val rating: Double,
    val reviewCount: Int,
    val description: String,
    val highlights: List<String>,
    val recommendedGear: List<String>,
    val currentTempC: Int,
    val weatherCondition: String, // "Sunny", "Partly Cloudy", "Breezy", "Light Rain"
    val fireDangerLevel: String, // "Low", "Moderate", "High", "Extreme"
    val routeWaypoints: List<TrailPoint>,
    val isSaved: Boolean = false,
    val isVisited: Boolean = false
)

data class TrailPoint(
    val name: String,
    val kmMarker: Double,
    val elevationM: Int,
    val type: String // "START", "LOOKOUT", "WATER", "SUMMIT", "CAMPSITE", "END"
)

@Entity(tableName = "hike_matches")
data class HikeBuddy(
    @PrimaryKey val id: String,
    val name: String,
    val age: Int,
    val location: String,
    val bio: String,
    val fitnessLevel: String, // "Beginner", "Moderate", "Advanced", "Endurance"
    val preferredPace: String, // "Leisurely (3 km/h)", "Moderate (4.5 km/h)", "Fast-paced (6 km/h)"
    val activityVibe: String, // "Relaxed", "Social", "Photography", "Fast-paced", "Training", "Exploration"
    val experienceYears: Int,
    val matchScore: Int, // e.g. 96%
    val matchReasons: List<String>,
    val preferredTrails: List<String>,
    val verifiedSkills: List<String>,
    val completedHikesCount: Int,
    val attendanceRate: Int, // e.g. 98%
    val matchStatus: String = "AVAILABLE" // "AVAILABLE", "INVITED", "MATCHED", "CHATTING"
)

@Entity(tableName = "trips")
data class TripPlan(
    @PrimaryKey val id: String,
    val title: String,
    val trailId: String,
    val trailName: String,
    val departureDate: String,
    val returnDate: String,
    val departureTime: String,
    val meetingPoint: String,
    val status: String, // "PLANNING", "ON_TRAIL", "COMPLETED"
    val maxParticipants: Int,
    val organizerName: String,
    val carpoolSeatsTotal: Int,
    val carpoolSeatsTaken: Int,
    val emergencyContactInfo: String,
    val weatherForecast: String,
    val safetyScore: Int = 92
)

@Entity(tableName = "trip_participants")
data class TripParticipant(
    @PrimaryKey val id: String,
    val tripId: String,
    val name: String,
    val role: String, // "Organizer", "Navigator", "First Aider", "Camp Chef", "Photographer", "Explorer"
    val avatarColor: Long,
    val carpoolRole: String, // "Driver (4 seats)", "Passenger", "Self-drive"
    val isReady: Boolean = false
)

@Entity(tableName = "shared_gear")
data class SharedGearItem(
    @PrimaryKey val id: String,
    val tripId: String,
    val name: String,
    val category: String, // "Safety", "Cooking", "Shelter", "Navigation", "Personal"
    val assignedTo: String,
    val isEssential: Boolean,
    val isPacked: Boolean = false,
    val weightKg: Double = 0.5
)

@Entity(tableName = "trip_meals")
data class TripMeal(
    @PrimaryKey val id: String,
    val tripId: String,
    val mealName: String,
    val mealType: String, // "Breakfast", "Trail Lunch", "Campfire Dinner", "Trail Snacks"
    val assignedTo: String,
    val dietaryInfo: String
)

@Entity(tableName = "chat_messages")
data class ChatMessage(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tripId: String,
    val channel: String, // "TRIP_GROUP", "CAMPSITE_LOCAL", "EMERGENCY_SOS"
    val senderName: String,
    val messageText: String,
    val timestamp: String,
    val isEmergency: Boolean = false
)

@Entity(tableName = "trail_moments")
data class TrailMoment(
    @PrimaryKey val id: String,
    val trailId: String,
    val type: String, // "LOOKOUT", "WATER_SOURCE", "WILDFLOWER", "HAZARD_SNAKE", "HAZARD_BLOCKED", "SUNSET_SPOT", "REST_AREA"
    val title: String,
    val description: String,
    val reportedBy: String,
    val timeAgo: String,
    val upvotes: Int,
    val isVerified: Boolean,
    val kmMarker: Double,
    val warningLevel: String = "INFO" // "INFO", "CAUTION", "DANGER"
)

@Entity(tableName = "species_scans")
data class SpeciesScan(
    @PrimaryKey val id: String,
    val commonName: String,
    val scientificName: String,
    val category: String, // "PLANT", "BIRD", "MUSHROOM", "GEOLOGY", "TRACK", "REPTILE"
    val confidence: Int,
    val description: String,
    val habitat: String,
    val isNative: Boolean,
    val safetyNote: String,
    val foundAtLocation: String,
    val timestamp: String,
    val tripId: String? = null
)

@Entity(tableName = "adventure_stories")
data class AdventureStory(
    @PrimaryKey val id: String,
    val tripId: String,
    val trailTitle: String,
    val dateFormatted: String,
    val totalDistanceKm: Double,
    val totalDurationHours: Double,
    val participantsSummary: String,
    val keyMilestones: List<String>,
    val highlightsNarrative: String,
    val speciesDiscoveredCount: Int,
    val elevationGainM: Int
)

@Entity(tableName = "community_groups")
data class CommunityGroup(
    @PrimaryKey val id: String,
    val name: String,
    val category: String,
    val memberCount: Int,
    val description: String,
    val upcomingTripTitle: String,
    val upcomingTripDate: String,
    val totalExploredKm: Int,
    val isJoined: Boolean = false
)

@Entity(tableName = "challenges")
data class AdventureChallenge(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val progress: Int,
    val target: Int,
    val unit: String,
    val isTeam: Boolean,
    val category: String,
    val badgeName: String
)

@Entity(tableName = "user_profile")
data class UserProfile(
    @PrimaryKey val id: String = "me",
    val name: String = "Alex Rivera",
    val handle: String = "@alex_outdoors",
    val bio: String = "Trail enthusiast & amateur botanist based in Melbourne. Love alpine scrambles and sunrise summits.",
    val totalHikes: Int = 42,
    val overnightTrips: Int = 6,
    val totalKmExplored: Int = 310,
    val attendanceRate: Int = 96,
    val repeatHikerCount: Int = 11,
    val groupTripsOrganized: Int = 8,
    val fitnessLevel: String = "Advanced",
    val preferredPace: String = "Moderate (4.5 km/h)",
    val preferredVibe: String = "Exploration & Photography",
    val verifiedSkills: List<String> = listOf("Wilderness First Aid", "Topographic Navigation", "Multi-day Camping", "Leave No Trace"),
    val badges: List<String> = listOf("Trail Master", "Flora Scout", "Reliable Pacer", "Summit Chaser", "Gear Guru")
)

@Entity(tableName = "user_accounts")
data class UserAccount(
    @PrimaryKey val id: String,
    val email: String,
    val password: String = "trail2026",
    val name: String,
    val handle: String,
    val bio: String,
    val avatarInitials: String,
    val avatarColorHex: Long = 0xFFCD744C,
    val fitnessLevel: String = "Intermediate",
    val preferredPace: String = "Moderate (4.5 km/h)",
    val preferredVibe: String = "Social & Exploration",
    val location: String = "Melbourne, Victoria",
    val totalHikes: Int = 12,
    val overnightTrips: Int = 2,
    val totalKmExplored: Int = 145,
    val attendanceRate: Int = 98,
    val repeatHikerCount: Int = 5,
    val groupTripsOrganized: Int = 3,
    val verifiedSkills: List<String> = listOf("Navigation", "Leave No Trace"),
    val badges: List<String> = listOf("Trail Explorer", "Early Riser"),
    val isLoggedIn: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
