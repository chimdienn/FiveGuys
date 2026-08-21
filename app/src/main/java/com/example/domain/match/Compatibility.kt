package com.example.domain.match

import com.example.domain.model.UserProfile
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Deterministic weighted compatibility scoring for HikeMatch.
 *
 * There is no machine learning here and there should not be (spec section 18). The score
 * is a weighted sum of six independent components, each normalised to 0.0..1.0, so the
 * result is explainable: every percentage point can be traced to a component, which is
 * what makes the "why you match" reasons truthful rather than decorative.
 *
 * The function is pure — same inputs, same output, no clock, no IO — which is what makes
 * it testable and what makes two devices agree on a score.
 */
object Compatibility {

    /** Component weights. These must sum to 100. */
    const val WEIGHT_ACTIVITY = 25
    const val WEIGHT_EXPERIENCE = 20
    const val WEIGHT_FITNESS = 15
    const val WEIGHT_PACE = 15
    const val WEIGHT_SOCIAL = 15
    const val WEIGHT_LOCATION = 10

    const val TOTAL_WEIGHT = WEIGHT_ACTIVITY + WEIGHT_EXPERIENCE + WEIGHT_FITNESS +
        WEIGHT_PACE + WEIGHT_SOCIAL + WEIGHT_LOCATION

    /** Number of steps between the extreme values of each ordered scale. */
    private const val EXPERIENCE_SPAN = 4.0 // NEW..EXPERT
    private const val FITNESS_SPAN = 3.0 // BEGINNER..VERY_FIT
    private const val PACE_SPAN = 3.0 // RELAXED..TRAINING

    fun calculate(userA: UserProfile, userB: UserProfile): CompatibilityResult {
        val activity = jaccard(userA.interests, userB.interests)
        val experience = ordinalCloseness(
            userA.experienceLevel.ordinal, userB.experienceLevel.ordinal, EXPERIENCE_SPAN
        )
        val fitness = ordinalCloseness(
            userA.fitnessLevel.ordinal, userB.fitnessLevel.ordinal, FITNESS_SPAN
        )
        val pace = ordinalCloseness(
            userA.preferredPace.ordinal, userB.preferredPace.ordinal, PACE_SPAN
        )
        val social = jaccard(userA.socialStyles, userB.socialStyles)
        val location = locationCloseness(userA.homeArea, userB.homeArea)

        val components = listOf(
            Component(Facet.ACTIVITY, activity, WEIGHT_ACTIVITY),
            Component(Facet.EXPERIENCE, experience, WEIGHT_EXPERIENCE),
            Component(Facet.FITNESS, fitness, WEIGHT_FITNESS),
            Component(Facet.PACE, pace, WEIGHT_PACE),
            Component(Facet.SOCIAL, social, WEIGHT_SOCIAL),
            Component(Facet.LOCATION, location, WEIGHT_LOCATION)
        )

        val score = components.sumOf { it.normalised * it.weight }.roundToInt().coerceIn(0, 100)

        return CompatibilityResult(
            score = score,
            reasons = buildReasons(userA, userB, components),
            components = components
        )
    }

    /**
     * Overlap of two unordered sets, 0.0..1.0.
     *
     * Two users who have each selected nothing are treated as neutral (0.5) rather than
     * incompatible (0.0) — an unanswered question is not evidence of a mismatch.
     */
    private fun <T> jaccard(a: Set<T>, b: Set<T>): Double {
        if (a.isEmpty() && b.isEmpty()) return 0.5
        if (a.isEmpty() || b.isEmpty()) return 0.0
        val union = (a + b).size
        return if (union == 0) 0.0 else a.intersect(b).size.toDouble() / union
    }

    /** Closeness of two positions on an ordered scale, 1.0 when identical. */
    private fun ordinalCloseness(a: Int, b: Int, span: Double): Double =
        (1.0 - abs(a - b) / span).coerceIn(0.0, 1.0)

    /**
     * Approximate locality comparison against free text such as "Melbourne, Victoria".
     *
     * Never uses coordinates — Biomate does not hold precise home locations to compare
     * (spec section 64). Unknown on either side is neutral rather than penalising.
     */
    private fun locationCloseness(a: String?, b: String?): Double {
        val left = a?.trim()?.lowercase().orEmpty()
        val right = b?.trim()?.lowercase().orEmpty()
        if (left.isEmpty() || right.isEmpty()) return 0.5
        if (left == right) return 1.0
        val leftTokens = left.split(',', ' ').map { it.trim() }.filter { it.length > 2 }.toSet()
        val rightTokens = right.split(',', ' ').map { it.trim() }.filter { it.length > 2 }.toSet()
        return if (leftTokens.intersect(rightTokens).isNotEmpty()) 0.6 else 0.0
    }

    private fun buildReasons(
        a: UserProfile,
        b: UserProfile,
        components: List<Component>
    ): List<String> {
        val reasons = mutableListOf<String>()

        val sharedActivities = a.interests.intersect(b.interests)
        if (sharedActivities.isNotEmpty()) {
            reasons += "You both enjoy " + sharedActivities.joinToString(" and ") { it.label.lowercase() }
        }

        val paceComponent = components.first { it.facet == Facet.PACE }
        if (paceComponent.normalised >= 0.99) {
            reasons += "You both prefer a ${a.preferredPace.label.lowercase()} pace"
        } else if (paceComponent.normalised >= 0.66) {
            reasons += "Similar preferred pace"
        }

        val sharedStyles = a.socialStyles.intersect(b.socialStyles)
        if (sharedStyles.isNotEmpty()) {
            reasons += "Both prefer " + sharedStyles.joinToString(" and ") { it.label.lowercase() } + " adventures"
        }

        val experienceComponent = components.first { it.facet == Facet.EXPERIENCE }
        if (experienceComponent.normalised >= 0.99) {
            reasons += "Same experience level (${a.experienceLevel.label.lowercase()})"
        } else if (experienceComponent.normalised >= 0.74) {
            reasons += "Comparable outdoor experience"
        }

        val fitnessComponent = components.first { it.facet == Facet.FITNESS }
        if (fitnessComponent.normalised >= 0.99) {
            reasons += "Matching fitness level"
        }

        val locationComponent = components.first { it.facet == Facet.LOCATION }
        if (locationComponent.normalised >= 0.99 && !b.homeArea.isNullOrBlank()) {
            reasons += "Both based around ${b.homeArea}"
        } else if (locationComponent.normalised >= 0.6 && !b.homeArea.isNullOrBlank()) {
            reasons += "Nearby region"
        }

        val sharedSkills = a.skills.intersect(b.skills)
        if (reasons.size < 3 && sharedSkills.isNotEmpty()) {
            reasons += "Shared skills: " + sharedSkills.joinToString(", ") { it.label }
        }

        if (reasons.isEmpty()) {
            reasons += "Different styles — could be a good chance to try something new"
        }

        return reasons.take(4)
    }

    enum class Facet { ACTIVITY, EXPERIENCE, FITNESS, PACE, SOCIAL, LOCATION }

    /** One weighted contribution to the final score. */
    data class Component(
        val facet: Facet,
        /** 0.0..1.0 */
        val normalised: Double,
        val weight: Int
    ) {
        /** Points this component contributed out of 100. */
        val points: Int get() = (normalised * weight).roundToInt()
    }
}

data class CompatibilityResult(
    val score: Int,
    val reasons: List<String>,
    val components: List<Compatibility.Component> = emptyList()
)
