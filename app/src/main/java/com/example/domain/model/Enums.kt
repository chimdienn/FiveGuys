package com.example.domain.model

/**
 * Domain vocabulary for Biomate.
 *
 * These enums replace the free-form `String` columns used by the original prototype
 * (`fitnessLevel: String`, `difficulty: String`, ...). Modelling them as closed sets
 * means the compiler — not a runtime string comparison — checks that every branch is
 * handled, and it keeps the compatibility algorithm honest: ordinal distance is only
 * meaningful because the ordering below is deliberate.
 */

/** Ordered from least to most demanding. Ordinal distance is used by the matcher. */
enum class FitnessLevel(val label: String) {
    BEGINNER("Beginner"),
    MODERATE("Moderate"),
    FIT("Fit"),
    VERY_FIT("Very fit");

    companion object {
        fun fromLabelOrNull(value: String?): FitnessLevel? =
            entries.firstOrNull { it.label.equals(value?.trim(), ignoreCase = true) || it.name == value }
    }
}

/** Ordered from least to most experienced. Ordinal distance is used by the matcher. */
enum class ExperienceLevel(val label: String) {
    NEW("New"),
    BEGINNER("Beginner"),
    INTERMEDIATE("Intermediate"),
    EXPERIENCED("Experienced"),
    EXPERT("Expert");

    companion object {
        fun fromLabelOrNull(value: String?): ExperienceLevel? =
            entries.firstOrNull { it.label.equals(value?.trim(), ignoreCase = true) || it.name == value }
    }
}

/** Ordered from slowest to fastest. Ordinal distance is used by the matcher. */
enum class PreferredPace(val label: String, val approxKmh: Double) {
    RELAXED("Relaxed", 3.0),
    MODERATE("Moderate", 4.5),
    FAST("Fast", 6.0),
    TRAINING("Training", 7.5);

    companion object {
        fun fromLabelOrNull(value: String?): PreferredPace? =
            entries.firstOrNull { it.label.equals(value?.trim(), ignoreCase = true) || it.name == value }
    }
}

/** Unordered — compared as a set overlap, never by ordinal. */
enum class SocialStyle(val label: String) {
    RELAXED("Relaxed"),
    SOCIAL("Social"),
    PHOTOGRAPHY("Photography"),
    FAST_PACED("Fast-paced"),
    TRAINING("Training"),
    EXPLORATION("Exploration");

    companion object {
        fun fromLabelOrNull(value: String?): SocialStyle? =
            entries.firstOrNull { it.label.equals(value?.trim(), ignoreCase = true) || it.name == value }
    }
}

/**
 * Activities a user can be interested in and a trail can be suited to.
 *
 * The first four are the MVP set; the rest exist so that adding them later is a data
 * change rather than a schema change (spec section 1).
 */
enum class ActivityType(val label: String, val inMvp: Boolean) {
    HIKING("Hiking", true),
    CAMPING("Camping", true),
    TRAIL_RUNNING("Trail running", true),
    CYCLING("Cycling", true),
    SKIING("Skiing", false),
    CLIMBING("Climbing", false),
    KAYAKING("Kayaking", false),
    MOUNTAIN_BIKING("Mountain biking", false),
    WALKING("Walking", false),
    PHOTOGRAPHY("Photography", false);

    companion object {
        val mvpActivities: List<ActivityType> get() = entries.filter { it.inMvp }

        fun fromLabelOrNull(value: String?): ActivityType? =
            entries.firstOrNull { it.label.equals(value?.trim(), ignoreCase = true) || it.name == value }
    }
}

enum class Skill(val label: String) {
    NAVIGATION("Navigation"),
    FIRST_AID("First Aid"),
    CAMPING("Camping"),
    OUTDOOR_COOKING("Outdoor Cooking"),
    TRAIL_RUNNING("Trail Running"),
    CLIMBING("Climbing");

    companion object {
        fun fromLabelOrNull(value: String?): Skill? =
            entries.firstOrNull { it.label.equals(value?.trim(), ignoreCase = true) || it.name == value }
    }
}

/** Ordered from easiest to hardest — used by the weather ranking rules. */
enum class Difficulty(val label: String) {
    EASY("Easy"),
    MODERATE("Moderate"),
    HARD("Hard"),
    CHALLENGING("Challenging");

    companion object {
        fun fromLabelOrNull(value: String?): Difficulty =
            entries.firstOrNull { it.label.equals(value?.trim(), ignoreCase = true) || it.name == value }
                ?: MODERATE
    }
}
