package com.example.data.seed

import com.example.domain.model.ActivityType
import com.example.domain.model.ExperienceLevel
import com.example.domain.model.FitnessLevel
import com.example.domain.model.PreferredPace
import com.example.domain.model.Skill
import com.example.domain.model.SocialStyle
import com.example.domain.model.UserProfile

/**
 * Development accounts and demo profiles.
 *
 * These exist so that a fresh install has people to match with, connect to and plan trips
 * alongside — an empty HikeMatch deck is impossible to evaluate. The set deliberately
 * spans fitness levels, experience levels, paces and social styles so the compatibility
 * algorithm produces a spread of scores rather than a wall of 90s.
 *
 * The two accounts with [password] set are sign-in-able development logins. They are
 * created **only** in the local backend and only when the credential table is empty; the
 * Firebase path never seeds authentication, so these passwords cannot reach a real project
 * (spec section 74).
 */
object SeedUsers {

    const val DEV_PASSWORD = "BiomateDemo123!"

    data class SeedProfile(
        val profile: UserProfile,
        /** Non-null only for the two documented development logins. */
        val password: String? = null
    )

    val all: List<SeedProfile> = listOf(
        SeedProfile(
            profile(
                uid = "demo_alex",
                name = "Alex Rivera",
                bio = "Trail enthusiast and amateur botanist. Sunrise summits and long ridge days.",
                birthYear = 1998,
                area = "Melbourne, Victoria",
                fitness = FitnessLevel.FIT,
                experience = ExperienceLevel.EXPERIENCED,
                pace = PreferredPace.MODERATE,
                styles = setOf(SocialStyle.EXPLORATION, SocialStyle.PHOTOGRAPHY),
                interests = setOf(ActivityType.HIKING, ActivityType.CAMPING),
                skills = setOf(Skill.NAVIGATION, Skill.FIRST_AID, Skill.CAMPING),
                colour = 0xFF1B4938
            ),
            password = DEV_PASSWORD
        ),
        SeedProfile(
            profile(
                uid = "demo_sarah",
                name = "Sarah Chen",
                bio = "Weekend walker working up to overnight trips. Always brings too much food.",
                birthYear = 2000,
                area = "Melbourne, Victoria",
                fitness = FitnessLevel.MODERATE,
                experience = ExperienceLevel.INTERMEDIATE,
                pace = PreferredPace.MODERATE,
                styles = setOf(SocialStyle.SOCIAL, SocialStyle.EXPLORATION),
                interests = setOf(ActivityType.HIKING, ActivityType.CAMPING),
                skills = setOf(Skill.OUTDOOR_COOKING, Skill.CAMPING),
                colour = 0xFFCD744C
            ),
            password = DEV_PASSWORD
        ),
        SeedProfile(
            profile(
                uid = "demo_marcus",
                name = "Marcus Webb",
                bio = "Trail runner. If it has a ridgeline I have probably run it badly.",
                birthYear = 1996,
                area = "Geelong, Victoria",
                fitness = FitnessLevel.VERY_FIT,
                experience = ExperienceLevel.EXPERT,
                pace = PreferredPace.TRAINING,
                styles = setOf(SocialStyle.TRAINING, SocialStyle.FAST_PACED),
                interests = setOf(ActivityType.TRAIL_RUNNING, ActivityType.HIKING),
                skills = setOf(Skill.TRAIL_RUNNING, Skill.NAVIGATION),
                colour = 0xFF2563EB
            )
        ),
        SeedProfile(
            profile(
                uid = "demo_priya",
                name = "Priya Nair",
                bio = "Photographer first, walker second. I will stop for the light.",
                birthYear = 1995,
                area = "Melbourne, Victoria",
                fitness = FitnessLevel.MODERATE,
                experience = ExperienceLevel.INTERMEDIATE,
                pace = PreferredPace.RELAXED,
                styles = setOf(SocialStyle.PHOTOGRAPHY, SocialStyle.RELAXED),
                interests = setOf(ActivityType.HIKING, ActivityType.PHOTOGRAPHY),
                skills = setOf(Skill.NAVIGATION),
                colour = 0xFF7C3AED
            )
        ),
        SeedProfile(
            profile(
                uid = "demo_tom",
                name = "Tom Whitfield",
                bio = "Started walking this year. Slowly getting less terrible at hills.",
                birthYear = 2003,
                area = "Ballarat, Victoria",
                fitness = FitnessLevel.BEGINNER,
                experience = ExperienceLevel.NEW,
                pace = PreferredPace.RELAXED,
                styles = setOf(SocialStyle.SOCIAL, SocialStyle.RELAXED),
                interests = setOf(ActivityType.HIKING),
                skills = emptySet(),
                colour = 0xFFD97706
            )
        ),
        SeedProfile(
            profile(
                uid = "demo_hannah",
                name = "Hannah Okafor",
                bio = "Wilderness first aid instructor. Happiest three days from a road.",
                birthYear = 1992,
                area = "Bright, Victoria",
                fitness = FitnessLevel.VERY_FIT,
                experience = ExperienceLevel.EXPERT,
                pace = PreferredPace.MODERATE,
                styles = setOf(SocialStyle.EXPLORATION),
                interests = setOf(ActivityType.HIKING, ActivityType.CAMPING, ActivityType.CLIMBING),
                skills = setOf(Skill.FIRST_AID, Skill.NAVIGATION, Skill.CAMPING, Skill.CLIMBING),
                colour = 0xFF0D9488
            )
        ),
        SeedProfile(
            profile(
                uid = "demo_daniel",
                name = "Daniel Kaur",
                bio = "Cyclist who discovered walking during a bike repair. No regrets.",
                birthYear = 1999,
                area = "Melbourne, Victoria",
                fitness = FitnessLevel.FIT,
                experience = ExperienceLevel.BEGINNER,
                pace = PreferredPace.FAST,
                styles = setOf(SocialStyle.FAST_PACED, SocialStyle.SOCIAL),
                interests = setOf(ActivityType.CYCLING, ActivityType.HIKING),
                skills = setOf(Skill.NAVIGATION),
                colour = 0xFFDC2626
            )
        ),
        SeedProfile(
            profile(
                uid = "demo_yuki",
                name = "Yuki Tanaka",
                bio = "Camp cook. Bring a spare mug and I will fill it.",
                birthYear = 1997,
                area = "Bendigo, Victoria",
                fitness = FitnessLevel.MODERATE,
                experience = ExperienceLevel.EXPERIENCED,
                pace = PreferredPace.RELAXED,
                styles = setOf(SocialStyle.SOCIAL, SocialStyle.RELAXED),
                interests = setOf(ActivityType.CAMPING, ActivityType.HIKING),
                skills = setOf(Skill.OUTDOOR_COOKING, Skill.CAMPING, Skill.FIRST_AID),
                colour = 0xFF0891B2
            )
        ),
        SeedProfile(
            profile(
                uid = "demo_liam",
                name = "Liam Brennan",
                bio = "Grampians regular. Will happily talk about rock for an hour.",
                birthYear = 1994,
                area = "Halls Gap, Victoria",
                fitness = FitnessLevel.FIT,
                experience = ExperienceLevel.EXPERIENCED,
                pace = PreferredPace.MODERATE,
                styles = setOf(SocialStyle.EXPLORATION, SocialStyle.PHOTOGRAPHY),
                interests = setOf(ActivityType.HIKING, ActivityType.CLIMBING),
                skills = setOf(Skill.CLIMBING, Skill.NAVIGATION),
                colour = 0xFF65A30D
            )
        ),
        SeedProfile(
            profile(
                uid = "demo_amara",
                name = "Amara Nwosu",
                bio = "Two kids, one dog, still getting out most weekends.",
                birthYear = 1989,
                area = "Geelong, Victoria",
                fitness = FitnessLevel.MODERATE,
                experience = ExperienceLevel.INTERMEDIATE,
                pace = PreferredPace.RELAXED,
                styles = setOf(SocialStyle.RELAXED, SocialStyle.SOCIAL),
                interests = setOf(ActivityType.HIKING, ActivityType.WALKING),
                skills = setOf(Skill.FIRST_AID),
                colour = 0xFFBE185D
            )
        ),
        SeedProfile(
            profile(
                uid = "demo_jonas",
                name = "Jonas Meyer",
                bio = "Long-distance walker. Currently section-hiking the AAWT.",
                birthYear = 1991,
                area = "Melbourne, Victoria",
                fitness = FitnessLevel.VERY_FIT,
                experience = ExperienceLevel.EXPERT,
                pace = PreferredPace.FAST,
                styles = setOf(SocialStyle.EXPLORATION, SocialStyle.TRAINING),
                interests = setOf(ActivityType.HIKING, ActivityType.CAMPING, ActivityType.TRAIL_RUNNING),
                skills = setOf(Skill.NAVIGATION, Skill.CAMPING, Skill.FIRST_AID),
                colour = 0xFF4338CA
            )
        ),
        SeedProfile(
            profile(
                uid = "demo_ellie",
                name = "Ellie Sanderson",
                bio = "Birdwatcher. I walk slowly and I am not sorry about it.",
                birthYear = 2001,
                area = "Warrnambool, Victoria",
                fitness = FitnessLevel.BEGINNER,
                experience = ExperienceLevel.BEGINNER,
                pace = PreferredPace.RELAXED,
                styles = setOf(SocialStyle.PHOTOGRAPHY, SocialStyle.RELAXED),
                interests = setOf(ActivityType.WALKING, ActivityType.PHOTOGRAPHY, ActivityType.HIKING),
                skills = emptySet(),
                colour = 0xFFEA580C
            )
        ),
        SeedProfile(
            profile(
                uid = "demo_raj",
                name = "Raj Patel",
                bio = "Weeknight trail runner, weekend camper. Melbourne based.",
                birthYear = 1993,
                area = "Melbourne, Victoria",
                fitness = FitnessLevel.FIT,
                experience = ExperienceLevel.INTERMEDIATE,
                pace = PreferredPace.FAST,
                styles = setOf(SocialStyle.TRAINING, SocialStyle.SOCIAL),
                interests = setOf(ActivityType.TRAIL_RUNNING, ActivityType.CAMPING, ActivityType.HIKING),
                skills = setOf(Skill.TRAIL_RUNNING, Skill.OUTDOOR_COOKING),
                colour = 0xFF9333EA
            )
        )
    )

    /** The two accounts documented in the README as sign-in-able. */
    val developmentLogins: List<Pair<String, SeedProfile>>
        get() = all.filter { it.password != null }.map { emailFor(it.profile.uid) to it }

    fun emailFor(uid: String): String = uid.removePrefix("demo_") + "@biomate.dev"

    private fun profile(
        uid: String,
        name: String,
        bio: String,
        birthYear: Int,
        area: String,
        fitness: FitnessLevel,
        experience: ExperienceLevel,
        pace: PreferredPace,
        styles: Set<SocialStyle>,
        interests: Set<ActivityType>,
        skills: Set<Skill>,
        colour: Long
    ) = UserProfile(
        uid = uid,
        displayName = name,
        bio = bio,
        birthYear = birthYear,
        homeArea = area,
        fitnessLevel = fitness,
        experienceLevel = experience,
        preferredPace = pace,
        socialStyles = styles,
        interests = interests,
        skills = skills,
        avatarColorHex = colour,
        onboardingComplete = true
    )
}
