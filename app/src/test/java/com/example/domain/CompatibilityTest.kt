package com.example.domain

import com.example.domain.match.Compatibility
import com.example.domain.model.ActivityType
import com.example.domain.model.ExperienceLevel
import com.example.domain.model.FitnessLevel
import com.example.domain.model.PreferredPace
import com.example.domain.model.SocialStyle
import com.example.domain.model.UserProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CompatibilityTest {

    private fun profile(
        uid: String,
        interests: Set<ActivityType> = setOf(ActivityType.HIKING),
        fitness: FitnessLevel = FitnessLevel.MODERATE,
        experience: ExperienceLevel = ExperienceLevel.INTERMEDIATE,
        pace: PreferredPace = PreferredPace.MODERATE,
        styles: Set<SocialStyle> = setOf(SocialStyle.SOCIAL),
        area: String? = "Melbourne, Victoria"
    ) = UserProfile(
        uid = uid,
        displayName = uid,
        interests = interests,
        fitnessLevel = fitness,
        experienceLevel = experience,
        preferredPace = pace,
        socialStyles = styles,
        homeArea = area
    )

    @Test
    fun `weights sum to one hundred`() {
        assertEquals(100, Compatibility.TOTAL_WEIGHT)
    }

    @Test
    fun `identical profiles score one hundred`() {
        val a = profile("a")
        val b = profile("b")
        assertEquals(100, Compatibility.calculate(a, b).score)
    }

    @Test
    fun `completely opposed profiles score low`() {
        val a = profile(
            "a",
            interests = setOf(ActivityType.HIKING),
            fitness = FitnessLevel.BEGINNER,
            experience = ExperienceLevel.NEW,
            pace = PreferredPace.RELAXED,
            styles = setOf(SocialStyle.RELAXED),
            area = "Melbourne, Victoria"
        )
        val b = profile(
            "b",
            interests = setOf(ActivityType.CYCLING),
            fitness = FitnessLevel.VERY_FIT,
            experience = ExperienceLevel.EXPERT,
            pace = PreferredPace.TRAINING,
            styles = setOf(SocialStyle.TRAINING),
            area = "Cairns, Queensland"
        )
        assertTrue("Expected a low score, got ${Compatibility.calculate(a, b).score}",
            Compatibility.calculate(a, b).score < 20)
    }

    @Test
    fun `scoring is symmetric`() {
        val a = profile("a", interests = setOf(ActivityType.HIKING, ActivityType.CAMPING))
        val b = profile("b", interests = setOf(ActivityType.CAMPING), pace = PreferredPace.FAST)
        assertEquals(Compatibility.calculate(a, b).score, Compatibility.calculate(b, a).score)
    }

    @Test
    fun `shared activities raise the score`() {
        val base = profile("a", interests = setOf(ActivityType.HIKING, ActivityType.CAMPING))
        val overlapping = profile("b", interests = setOf(ActivityType.HIKING, ActivityType.CAMPING))
        val disjoint = profile("c", interests = setOf(ActivityType.CYCLING))

        assertTrue(
            Compatibility.calculate(base, overlapping).score >
                Compatibility.calculate(base, disjoint).score
        )
    }

    @Test
    fun `activity overlap contributes its full weight when identical`() {
        val a = profile("a", interests = setOf(ActivityType.HIKING, ActivityType.CAMPING))
        val b = profile("b", interests = setOf(ActivityType.HIKING, ActivityType.CAMPING))
        val activity = Compatibility.calculate(a, b).components
            .first { it.facet == Compatibility.Facet.ACTIVITY }
        assertEquals(Compatibility.WEIGHT_ACTIVITY, activity.points)
    }

    @Test
    fun `pace difference reduces the score`() {
        val a = profile("a", pace = PreferredPace.RELAXED)
        val same = profile("b", pace = PreferredPace.RELAXED)
        val opposite = profile("c", pace = PreferredPace.TRAINING)

        assertTrue(
            Compatibility.calculate(a, same).score > Compatibility.calculate(a, opposite).score
        )
    }

    @Test
    fun `adjacent pace scores higher than distant pace`() {
        val a = profile("a", pace = PreferredPace.RELAXED)
        val adjacent = Compatibility.calculate(a, profile("b", pace = PreferredPace.MODERATE)).score
        val distant = Compatibility.calculate(a, profile("c", pace = PreferredPace.TRAINING)).score
        assertTrue("adjacent=$adjacent distant=$distant", adjacent > distant)
    }

    @Test
    fun `social style overlap raises the score`() {
        val a = profile("a", styles = setOf(SocialStyle.PHOTOGRAPHY, SocialStyle.RELAXED))
        val shared = profile("b", styles = setOf(SocialStyle.PHOTOGRAPHY, SocialStyle.RELAXED))
        val different = profile("c", styles = setOf(SocialStyle.TRAINING))

        assertTrue(
            Compatibility.calculate(a, shared).score > Compatibility.calculate(a, different).score
        )
    }

    @Test
    fun `experience gap reduces the score`() {
        val a = profile("a", experience = ExperienceLevel.NEW)
        val close = Compatibility.calculate(a, profile("b", experience = ExperienceLevel.BEGINNER)).score
        val far = Compatibility.calculate(a, profile("c", experience = ExperienceLevel.EXPERT)).score
        assertTrue("close=$close far=$far", close > far)
    }

    @Test
    fun `same region scores higher than a different one`() {
        val a = profile("a", area = "Melbourne, Victoria")
        val same = Compatibility.calculate(a, profile("b", area = "Melbourne, Victoria")).score
        val elsewhere = Compatibility.calculate(a, profile("c", area = "Perth, Western Australia")).score
        assertTrue(same > elsewhere)
    }

    @Test
    fun `a shared state scores between an exact match and a different state`() {
        val a = profile("a", area = "Melbourne, Victoria")
        val exact = Compatibility.calculate(a, profile("b", area = "Melbourne, Victoria")).score
        val sameState = Compatibility.calculate(a, profile("c", area = "Geelong, Victoria")).score
        val other = Compatibility.calculate(a, profile("d", area = "Perth, Western Australia")).score
        assertTrue("exact=$exact sameState=$sameState other=$other",
            exact > sameState && sameState > other)
    }

    @Test
    fun `unknown location is neutral rather than penalising`() {
        val a = profile("a", area = "Melbourne, Victoria")
        val unknown = Compatibility.calculate(a, profile("b", area = null)).score
        val mismatched = Compatibility.calculate(a, profile("c", area = "Perth, Western Australia")).score
        assertTrue("unknown=$unknown mismatched=$mismatched", unknown > mismatched)
    }

    @Test
    fun `score never leaves the zero to one hundred range`() {
        val profiles = listOf(
            profile("a", interests = emptySet(), styles = emptySet(), area = null),
            profile("b", interests = ActivityType.entries.toSet(), styles = SocialStyle.entries.toSet()),
            profile("c", fitness = FitnessLevel.VERY_FIT, experience = ExperienceLevel.EXPERT)
        )
        for (x in profiles) {
            for (y in profiles) {
                val score = Compatibility.calculate(x, y).score
                assertTrue("score=$score", score in 0..100)
            }
        }
    }

    @Test
    fun `reasons explain a strong match`() {
        val a = profile("a", interests = setOf(ActivityType.HIKING, ActivityType.CAMPING))
        val b = profile("b", interests = setOf(ActivityType.HIKING, ActivityType.CAMPING))
        val reasons = Compatibility.calculate(a, b).reasons
        assertTrue(reasons.isNotEmpty())
        assertTrue(reasons.any { it.contains("hiking", ignoreCase = true) })
    }

    @Test
    fun `reasons are never empty even for a poor match`() {
        val a = profile("a", interests = setOf(ActivityType.HIKING), styles = setOf(SocialStyle.RELAXED), area = "Melbourne, Victoria")
        val b = profile("b", interests = setOf(ActivityType.CYCLING), styles = setOf(SocialStyle.TRAINING), area = "Darwin, Northern Territory")
        assertTrue(Compatibility.calculate(a, b).reasons.isNotEmpty())
    }

    @Test
    fun `component points sum to the reported score`() {
        val a = profile("a", interests = setOf(ActivityType.HIKING, ActivityType.CYCLING), pace = PreferredPace.FAST)
        val b = profile("b", interests = setOf(ActivityType.HIKING), pace = PreferredPace.MODERATE)
        val result = Compatibility.calculate(a, b)
        // Each component rounds independently, so allow one point of drift per component.
        val summed = result.components.sumOf { it.points }
        assertTrue(
            "score=${result.score} summed=$summed",
            kotlin.math.abs(result.score - summed) <= result.components.size
        )
    }

    @Test
    fun `two users with no preferences set still produce a usable score`() {
        val blank = profile("a", interests = emptySet(), styles = emptySet(), area = null)
        val other = profile("b", interests = emptySet(), styles = emptySet(), area = null)
        val result = Compatibility.calculate(blank, other)
        assertTrue(result.score in 0..100)
        assertNotEquals(0, result.score)
    }
}
