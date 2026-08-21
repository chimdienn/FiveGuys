package com.example.data.seed

import com.example.data.auth.PasswordHasher
import com.example.data.local.BiomateDaoV2
import com.example.data.local.LocalCredentialEntity
import com.example.data.mapper.toEntity
import com.example.domain.model.Connection
import com.example.domain.model.ConnectionStatus
import com.example.domain.model.Conversation
import com.example.domain.model.ConversationType
import com.example.domain.model.GearItem
import com.example.domain.model.Message
import com.example.domain.model.MomentCategory
import com.example.domain.model.MomentVisibility
import com.example.domain.model.TrailMoment
import com.example.domain.model.Trip
import com.example.domain.model.TripMember
import com.example.domain.model.TripMemberStatus
import com.example.domain.model.TripRole
import com.example.domain.model.TripStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Populates a fresh local install with demo people, relationships and content.
 *
 * Everything inserted here uses `INSERT OR IGNORE` on a stable id, so seeding is
 * idempotent: running it on every launch neither duplicates rows nor overwrites anything
 * a real user has since changed.
 *
 * Only ever called for the local backend. A Firebase-backed build seeds trails only —
 * demo credentials must never reach a real authentication provider (spec section 74).
 */
class DevSeeder(
    private val dao: BiomateDaoV2,
    private val nowMillis: () -> Long = System::currentTimeMillis
) {

    suspend fun seed() = withContext(Dispatchers.IO) {
        seedProfilesAndCredentials()
        seedConnections()
        seedTripsAndConversations()
        seedMoments()
    }

    private suspend fun seedProfilesAndCredentials() {
        dao.insertProfilesIfAbsent(SeedUsers.all.map { it.profile.toEntity() })

        if (dao.credentialCount() > 0) return
        val credentials = SeedUsers.all.mapNotNull { seed ->
            val password = seed.password ?: return@mapNotNull null
            val salt = PasswordHasher.newSalt()
            LocalCredentialEntity(
                email = SeedUsers.emailFor(seed.profile.uid),
                uid = seed.profile.uid,
                passwordHash = PasswordHasher.hash(password, salt),
                salt = salt,
                iterations = PasswordHasher.DEFAULT_ITERATIONS,
                createdAt = nowMillis()
            )
        }
        dao.insertCredentialsIfAbsent(credentials)
    }

    /** A mix of accepted connections and still-pending requests, so both UIs have content. */
    private suspend fun seedConnections() {
        val now = nowMillis()
        val accepted = listOf(
            "demo_alex" to "demo_marcus",
            "demo_alex" to "demo_priya",
            "demo_alex" to "demo_yuki",
            "demo_sarah" to "demo_tom",
            "demo_sarah" to "demo_amara",
            "demo_hannah" to "demo_jonas",
            "demo_liam" to "demo_priya"
        )
        val pending = listOf(
            "demo_daniel" to "demo_alex",
            "demo_ellie" to "demo_sarah",
            "demo_raj" to "demo_alex"
        )

        val rows = accepted.map { (a, b) ->
            Connection(
                id = Connection.connectionIdFor(a, b),
                requesterId = a,
                addresseeId = b,
                status = ConnectionStatus.ACCEPTED,
                createdAt = now - 7 * DAY_MS,
                respondedAt = now - 6 * DAY_MS
            ).toEntity()
        } + pending.map { (a, b) ->
            Connection(
                id = Connection.connectionIdFor(a, b),
                requesterId = a,
                addresseeId = b,
                status = ConnectionStatus.PENDING,
                createdAt = now - DAY_MS
            ).toEntity()
        }
        dao.insertConnectionsIfAbsent(rows)
    }

    private suspend fun seedTripsAndConversations() {
        val now = nowMillis()

        val upcoming = Trip(
            id = "trip_demo_prom",
            creatorId = "demo_alex",
            trailId = "trail_sealers_cove",
            trailName = "Sealers Cove Track",
            title = "Wilsons Prom Weekend",
            startsAt = atHour(now + 6 * DAY_MS, hour = 7),
            meetingPoint = "Telegraph Saddle car park, 07:00",
            participantLimit = 6,
            carpoolNotes = "Alex — 3 seats, leaving UniMelb 05:30.\nMarcus — self-driving.",
            foodNotes = "Yuki is bringing dinner. Everyone else: own lunch and snacks.",
            generalNotes = "Tide-dependent creek crossing — we need to be past Sealers Creek by 14:00.",
            emergencyNotes = "Parks Victoria 13 19 63. Emergency 000. Nearest hospital: Foster.",
            status = TripStatus.PLANNING,
            createdAt = now - 3 * DAY_MS,
            updatedAt = now - 3 * DAY_MS
        )

        val past = Trip(
            id = "trip_demo_youyangs",
            creatorId = "demo_sarah",
            trailId = "trail_you_yangs_flinders",
            trailName = "Flinders Peak Walk",
            title = "You Yangs Sunrise",
            startsAt = atHour(now - 21 * DAY_MS, hour = 6),
            meetingPoint = "Turntable car park, 05:45",
            carpoolNotes = "Sarah drove.",
            foodNotes = "Coffee after at Little River.",
            emergencyNotes = "Emergency 000.",
            status = TripStatus.COMPLETED,
            createdAt = now - 30 * DAY_MS,
            updatedAt = now - 21 * DAY_MS,
            completedAt = now - 21 * DAY_MS
        )

        dao.insertTripsIfAbsent(listOf(upcoming.toEntity(), past.toEntity()))

        val members = listOf(
            member(upcoming.id, "demo_alex", "Alex Rivera", TripRole.ORGANISER, TripMemberStatus.JOINED, now),
            member(upcoming.id, "demo_marcus", "Marcus Webb", TripRole.PARTICIPANT, TripMemberStatus.JOINED, now),
            member(upcoming.id, "demo_yuki", "Yuki Tanaka", TripRole.PARTICIPANT, TripMemberStatus.JOINED, now),
            member(upcoming.id, "demo_priya", "Priya Nair", TripRole.PARTICIPANT, TripMemberStatus.INVITED, null),
            member(past.id, "demo_sarah", "Sarah Chen", TripRole.ORGANISER, TripMemberStatus.JOINED, now, attended = true),
            member(past.id, "demo_tom", "Tom Whitfield", TripRole.PARTICIPANT, TripMemberStatus.JOINED, now, attended = true)
        )
        dao.insertMembersIfAbsent(members)

        dao.insertGearIfAbsent(
            listOf(
                GearItem("gear_demo_1", upcoming.id, "First Aid Kit", "Safety", assignedToUid = "demo_alex", assignedToName = "Alex Rivera", isPacked = true, isEssential = true),
                GearItem("gear_demo_2", upcoming.id, "Stove", "Cooking", assignedToUid = "demo_yuki", assignedToName = "Yuki Tanaka"),
                GearItem("gear_demo_3", upcoming.id, "Water Filter", "Safety", isEssential = true),
                GearItem("gear_demo_4", upcoming.id, "Tent", "Shelter", quantity = 2, assignedToUid = "demo_marcus", assignedToName = "Marcus Webb"),
                GearItem("gear_demo_5", upcoming.id, "Satellite communicator", "Safety", assignedToUid = "demo_alex", assignedToName = "Alex Rivera", isPacked = true)
            ).map { it.toEntity() }
        )

        val tripConversationId = Conversation.tripIdFor(upcoming.id)
        val messages = listOf(
            Message("msg_demo_1", tripConversationId, "demo_alex", "Alex Rivera", "Forecast looks good for Saturday. Still a chance of showers Sunday morning.", now - 2 * DAY_MS),
            Message("msg_demo_2", tripConversationId, "demo_yuki", "Yuki Tanaka", "I've got dinner covered. Any dietary things I should know about?", now - 2 * DAY_MS + 3_600_000),
            Message("msg_demo_3", tripConversationId, "demo_marcus", "Marcus Webb", "Nothing from me. I'll bring the second tent.", now - DAY_MS),
            Message("msg_demo_4", tripConversationId, "demo_alex", "Alex Rivera", "Reminder: we need to clear Sealers Creek before the tide turns at 14:00.", now - 6 * 3_600_000)
        )

        dao.insertConversationsIfAbsent(
            listOf(
                Conversation(
                    id = tripConversationId,
                    type = ConversationType.TRIP,
                    memberIds = listOf("demo_alex", "demo_marcus", "demo_yuki", "demo_priya"),
                    title = upcoming.title,
                    tripId = upcoming.id,
                    lastMessagePreview = messages.last().text.take(120),
                    lastMessageAt = messages.last().sentAt,
                    lastMessageSenderId = messages.last().senderId
                ).toEntity(),
                Conversation(
                    id = Conversation.directIdFor("demo_alex", "demo_priya"),
                    type = ConversationType.DIRECT,
                    memberIds = listOf("demo_alex", "demo_priya"),
                    title = "Priya Nair",
                    lastMessagePreview = "Are you going to the Prom this weekend?",
                    lastMessageAt = now - 4 * 3_600_000,
                    lastMessageSenderId = "demo_priya"
                ).toEntity()
            )
        )

        dao.insertMessagesIfAbsent(
            (messages + Message(
                "msg_demo_5",
                Conversation.directIdFor("demo_alex", "demo_priya"),
                "demo_priya",
                "Priya Nair",
                "Are you going to the Prom this weekend?",
                now - 4 * 3_600_000
            )).map { it.toEntity() }
        )
    }

    /**
     * Community reports along the seeded trails.
     *
     * Ages are spread from hours to weeks so that the staleness treatment on the map is
     * visible immediately — a fresh hazard and a three-week-old one must not look alike.
     */
    private suspend fun seedMoments() {
        val now = nowMillis()
        val moments = listOf(
            TrailMoment(
                id = "moment_demo_1", creatorId = "demo_marcus", creatorName = "Marcus Webb",
                trailId = "trail_sealers_cove", tripId = null,
                latitude = -39.0141, longitude = 146.3641,
                category = MomentCategory.HAZARD,
                description = "Large tree down across the track about 200m past Windy Saddle. Easy to step over but watch your footing.",
                visibility = MomentVisibility.PUBLIC, createdAt = now - 3 * 3_600_000, upvotes = 4
            ),
            TrailMoment(
                id = "moment_demo_2", creatorId = "demo_yuki", creatorName = "Yuki Tanaka",
                trailId = "trail_sealers_cove", tripId = null,
                latitude = -39.0116, longitude = 146.3729,
                category = MomentCategory.WATER,
                description = "Creek flowing well. Still filter it.",
                visibility = MomentVisibility.PUBLIC, createdAt = now - 2 * DAY_MS, upvotes = 7
            ),
            TrailMoment(
                id = "moment_demo_3", creatorId = "demo_priya", creatorName = "Priya Nair",
                trailId = "trail_pinnacle_grampians", tripId = null,
                latitude = -37.1641, longitude = 142.5089,
                category = MomentCategory.VIEWPOINT,
                description = "Best light is about 40 minutes before sunset from the ledge just below the lookout.",
                visibility = MomentVisibility.PUBLIC, createdAt = now - 12 * DAY_MS, upvotes = 23
            ),
            TrailMoment(
                id = "moment_demo_4", creatorId = "demo_liam", creatorName = "Liam Brennan",
                trailId = "trail_pinnacle_grampians", tripId = null,
                latitude = -37.1596, longitude = 142.5131,
                category = MomentCategory.TRAIL_CONDITION,
                description = "Silent Street is very slippery after the rain. Take the steps slowly.",
                visibility = MomentVisibility.PUBLIC, createdAt = now - 5 * DAY_MS, upvotes = 11
            ),
            TrailMoment(
                id = "moment_demo_5", creatorId = "demo_ellie", creatorName = "Ellie Sanderson",
                trailId = "trail_1000_steps", tripId = null,
                latitude = -37.8877, longitude = 145.3428,
                category = MomentCategory.WILDLIFE,
                description = "Lyrebird calling near the top of the steps around 7am. Stayed for ten minutes.",
                visibility = MomentVisibility.PUBLIC, createdAt = now - 26 * DAY_MS, upvotes = 31
            ),
            TrailMoment(
                id = "moment_demo_6", creatorId = "demo_hannah", creatorName = "Hannah Okafor",
                trailId = "trail_werribee_gorge", tripId = null,
                latitude = -37.6771, longitude = 144.3105,
                category = MomentCategory.HAZARD,
                description = "Cable section is fine but the ledge is narrow and there is a real drop. Not one for nervous walkers.",
                visibility = MomentVisibility.PUBLIC, createdAt = now - 9 * DAY_MS, upvotes = 18
            ),
            TrailMoment(
                id = "moment_demo_7", creatorId = "demo_jonas", creatorName = "Jonas Meyer",
                trailId = "trail_cathedral_razorback", tripId = null,
                latitude = -37.3688, longitude = 145.7513,
                category = MomentCategory.NOTE,
                description = "No water anywhere on the ridge. Carry everything you need from Cooks Mill.",
                visibility = MomentVisibility.PUBLIC, createdAt = now - 40 * DAY_MS, upvotes = 15
            )
        )
        dao.insertMomentsIfAbsent(moments.map { it.toEntity() })
    }

    /**
     * Snaps a timestamp to a given hour, local time.
     *
     * Seeded trips are offsets from "now", so without this a demo trip departs at whatever
     * time the app happened to be first launched — and a hike leaving at 02:35 makes the
     * seed data look broken.
     */
    private fun atHour(epochMillis: Long, hour: Int): Long =
        java.util.Calendar.getInstance().apply {
            timeInMillis = epochMillis
            set(java.util.Calendar.HOUR_OF_DAY, hour)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis

    private fun member(
        tripId: String,
        uid: String,
        name: String,
        role: TripRole,
        status: TripMemberStatus,
        joinedAt: Long?,
        attended: Boolean = false
    ) = TripMember(tripId, uid, name, role, status, joinedAt, attended).toEntity()

    private companion object {
        const val DAY_MS = 24 * 60 * 60 * 1000L
    }
}
