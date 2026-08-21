package com.example.data.seed

import com.example.domain.model.ActivityType
import com.example.domain.model.Difficulty
import com.example.domain.model.GeoPoint
import com.example.domain.model.Trail
import com.example.domain.model.TrailWaypoint

/**
 * Demo trail catalogue.
 *
 * These are real, well-known public walking tracks in Victoria, described from public
 * information (Parks Victoria signage and published track notes). No commercial trail
 * database has been scraped (spec section 13).
 *
 * Coordinates are approximate start points and the routes are simplified demonstration
 * polylines — enough to draw a plausible line on a map and to exercise the progress
 * calculation, but **not survey-grade navigation data**. `README.md` says so plainly, and
 * the trail detail screen carries the same caution.
 */
object SeedTrails {

    val all: List<Trail> = listOf(
        Trail(
            id = "trail_sealers_cove",
            name = "Sealers Cove Track",
            region = "Wilsons Promontory",
            activityTypes = setOf(ActivityType.HIKING, ActivityType.CAMPING),
            description = "A classic Prom walk from Telegraph Saddle over the ridge and down " +
                "through warm temperate rainforest to a wide, sheltered beach. The final " +
                "descent crosses a boardwalk over Sealers Creek, which floods on a high tide.",
            difficulty = Difficulty.MODERATE,
            distanceKm = 19.6,
            elevationGainM = 560,
            estimatedMinutes = 390,
            start = GeoPoint(-39.0306, 146.3392),
            route = listOf(
                GeoPoint(-39.0306, 146.3392),
                GeoPoint(-39.0271, 146.3438),
                GeoPoint(-39.0223, 146.3492),
                GeoPoint(-39.0180, 146.3560),
                GeoPoint(-39.0141, 146.3641),
                GeoPoint(-39.0116, 146.3729),
                GeoPoint(-39.0098, 146.3812),
                GeoPoint(-39.0072, 146.3889)
            ),
            waypoints = listOf(
                TrailWaypoint("Telegraph Saddle car park", 0.0, 120, "START"),
                TrailWaypoint("Windy Saddle", 3.2, 305, "LOOKOUT"),
                TrailWaypoint("Sealers Creek crossing", 8.1, 15, "WATER"),
                TrailWaypoint("Sealers Cove campsite", 9.8, 5, "CAMPSITE")
            ),
            tags = listOf("Coastal", "Rainforest", "Overnight option", "Beach"),
            isShaded = true,
            rating = 4.8,
            reviewCount = 412,
            highlights = listOf("Warm temperate rainforest", "Sheltered beach", "Creek boardwalk"),
            recommendedGear = listOf("Tide-aware timing", "Water filter", "Insect repellent")
        ),
        Trail(
            id = "trail_pinnacle_grampians",
            name = "The Pinnacle via Wonderland",
            region = "Grampians / Gariwerd",
            activityTypes = setOf(ActivityType.HIKING),
            description = "A steep, rocky scramble through the Grand Canyon and Silent Street " +
                "to a railed lookout with a long view over Halls Gap and the Fyans Valley. " +
                "Exposed rock for much of the upper section.",
            difficulty = Difficulty.HARD,
            distanceKm = 9.6,
            elevationGainM = 480,
            estimatedMinutes = 270,
            start = GeoPoint(-37.1550, 142.5183),
            route = listOf(
                GeoPoint(-37.1550, 142.5183),
                GeoPoint(-37.1573, 142.5158),
                GeoPoint(-37.1596, 142.5131),
                GeoPoint(-37.1618, 142.5107),
                GeoPoint(-37.1641, 142.5089),
                GeoPoint(-37.1662, 142.5072)
            ),
            waypoints = listOf(
                TrailWaypoint("Wonderland car park", 0.0, 260, "START"),
                TrailWaypoint("Grand Canyon", 1.1, 340, "LOOKOUT"),
                TrailWaypoint("Silent Street", 2.4, 520, "LOOKOUT"),
                TrailWaypoint("The Pinnacle", 4.3, 715, "SUMMIT")
            ),
            tags = listOf("Rock scramble", "Lookout", "Exposed"),
            isExposed = true,
            rating = 4.9,
            reviewCount = 903,
            highlights = listOf("Grand Canyon", "Silent Street", "Pinnacle lookout"),
            recommendedGear = listOf("Grippy footwear", "Sun protection", "2L+ water")
        ),
        Trail(
            id = "trail_cathedral_razorback",
            name = "Cathedral Range Southern Circuit",
            region = "Cathedral Range State Park",
            activityTypes = setOf(ActivityType.HIKING, ActivityType.CAMPING),
            description = "A demanding circuit taking in the Razorback ridge and Sugarloaf Peak. " +
                "Long stretches of narrow, rocky ridgeline with genuine exposure and no " +
                "water on the tops.",
            difficulty = Difficulty.CHALLENGING,
            distanceKm = 11.4,
            elevationGainM = 720,
            estimatedMinutes = 330,
            start = GeoPoint(-37.3736, 145.7461),
            route = listOf(
                GeoPoint(-37.3736, 145.7461),
                GeoPoint(-37.3712, 145.7488),
                GeoPoint(-37.3688, 145.7513),
                GeoPoint(-37.3661, 145.7534),
                GeoPoint(-37.3639, 145.7551),
                GeoPoint(-37.3617, 145.7522),
                GeoPoint(-37.3672, 145.7473)
            ),
            waypoints = listOf(
                TrailWaypoint("Cooks Mill", 0.0, 280, "START"),
                TrailWaypoint("Razorback ridge", 3.6, 780, "LOOKOUT"),
                TrailWaypoint("Sugarloaf Peak", 6.2, 920, "SUMMIT"),
                TrailWaypoint("Neds Gully", 9.8, 300, "WATER")
            ),
            tags = listOf("Ridgeline", "Exposed", "Scrambling"),
            isExposed = true,
            rating = 4.7,
            reviewCount = 288,
            highlights = listOf("Razorback ridgeline", "Sugarloaf Peak", "Wide valley views"),
            recommendedGear = listOf("3L water", "Helmet optional", "Wind shell")
        ),
        Trail(
            id = "trail_1000_steps",
            name = "Kokoda Track Memorial Walk",
            region = "Dandenong Ranges",
            activityTypes = setOf(ActivityType.HIKING, ActivityType.TRAIL_RUNNING),
            description = "The Thousand Steps: a short, steep climb on stone steps through wet " +
                "mountain ash forest, returning on the gentler Lyrebird Track. Busy at weekends.",
            difficulty = Difficulty.MODERATE,
            distanceKm = 3.0,
            elevationGainM = 220,
            estimatedMinutes = 75,
            start = GeoPoint(-37.8850, 145.3392),
            route = listOf(
                GeoPoint(-37.8850, 145.3392),
                GeoPoint(-37.8863, 145.3411),
                GeoPoint(-37.8877, 145.3428),
                GeoPoint(-37.8891, 145.3441),
                GeoPoint(-37.8872, 145.3418)
            ),
            waypoints = listOf(
                TrailWaypoint("Ferntree Gully car park", 0.0, 130, "START"),
                TrailWaypoint("Top of the steps", 1.5, 350, "LOOKOUT"),
                TrailWaypoint("Lyrebird Track junction", 2.1, 300, "REST")
            ),
            tags = listOf("Steps", "Fern gully", "Close to Melbourne", "Training"),
            isShaded = true,
            rating = 4.4,
            reviewCount = 1560,
            highlights = listOf("Mountain ash forest", "Steep step training", "Lyrebird habitat"),
            recommendedGear = listOf("1L water", "Grippy shoes")
        ),
        Trail(
            id = "trail_you_yangs_flinders",
            name = "Flinders Peak Walk",
            region = "You Yangs Regional Park",
            activityTypes = setOf(ActivityType.HIKING, ActivityType.TRAIL_RUNNING),
            description = "A short climb on steps and granite to the highest point of the You " +
                "Yangs, with a wide view across the Werribee Plains to Port Phillip and, on a " +
                "clear day, the Melbourne skyline.",
            difficulty = Difficulty.EASY,
            distanceKm = 3.2,
            elevationGainM = 180,
            estimatedMinutes = 70,
            start = GeoPoint(-37.9527, 144.4245),
            route = listOf(
                GeoPoint(-37.9527, 144.4245),
                GeoPoint(-37.9540, 144.4262),
                GeoPoint(-37.9553, 144.4278),
                GeoPoint(-37.9561, 144.4291)
            ),
            waypoints = listOf(
                TrailWaypoint("Turntable car park", 0.0, 120, "START"),
                TrailWaypoint("Flinders Peak", 1.6, 352, "SUMMIT")
            ),
            tags = listOf("Granite", "Short", "Big view", "Beginner friendly"),
            isExposed = true,
            rating = 4.5,
            reviewCount = 640,
            highlights = listOf("Granite outcrops", "Werribee Plains view", "Quick summit"),
            recommendedGear = listOf("Sun hat", "1L water")
        ),
        Trail(
            id = "trail_great_ocean_walk_stage1",
            name = "Great Ocean Walk — Apollo Bay to Elliot Ridge",
            region = "Great Otway National Park",
            activityTypes = setOf(ActivityType.HIKING, ActivityType.CAMPING),
            description = "The opening stage of the Great Ocean Walk, leaving the coast at " +
                "Apollo Bay and climbing gradually inland through eucalypt forest to the " +
                "Elliot Ridge hike-in campsite.",
            difficulty = Difficulty.MODERATE,
            distanceKm = 10.0,
            elevationGainM = 340,
            estimatedMinutes = 210,
            start = GeoPoint(-38.7561, 143.6683),
            route = listOf(
                GeoPoint(-38.7561, 143.6683),
                GeoPoint(-38.7602, 143.6641),
                GeoPoint(-38.7648, 143.6598),
                GeoPoint(-38.7691, 143.6550),
                GeoPoint(-38.7734, 143.6497)
            ),
            waypoints = listOf(
                TrailWaypoint("Apollo Bay trailhead", 0.0, 10, "START"),
                TrailWaypoint("Marengo lookout", 2.5, 90, "LOOKOUT"),
                TrailWaypoint("Elliot Ridge campsite", 10.0, 320, "CAMPSITE")
            ),
            tags = listOf("Multi-day", "Coastal", "Forest", "Hike-in camping"),
            isShaded = true,
            rating = 4.6,
            reviewCount = 355,
            highlights = listOf("Coastal outlook", "Otway forest", "Hike-in campsite"),
            recommendedGear = listOf("Tent", "Water filter", "Stove")
        ),
        Trail(
            id = "trail_mt_donna_buang",
            name = "Mount Donna Buang Rainforest Loop",
            region = "Yarra Ranges",
            activityTypes = setOf(ActivityType.HIKING, ActivityType.PHOTOGRAPHY),
            description = "A cool, damp loop through myrtle beech and tree ferns below the Donna " +
                "Buang summit, with an optional detour to the rainforest canopy walk.",
            difficulty = Difficulty.EASY,
            distanceKm = 4.4,
            elevationGainM = 160,
            estimatedMinutes = 95,
            start = GeoPoint(-37.7017, 145.6931),
            route = listOf(
                GeoPoint(-37.7017, 145.6931),
                GeoPoint(-37.7031, 145.6952),
                GeoPoint(-37.7048, 145.6968),
                GeoPoint(-37.7035, 145.6944),
                GeoPoint(-37.7017, 145.6931)
            ),
            waypoints = listOf(
                TrailWaypoint("Summit car park", 0.0, 1245, "START"),
                TrailWaypoint("Canopy walk", 1.8, 1180, "LOOKOUT"),
                TrailWaypoint("Cement Creek", 3.1, 1090, "WATER")
            ),
            tags = listOf("Rainforest", "Cool climate", "Family friendly", "Photography"),
            isShaded = true,
            rating = 4.3,
            reviewCount = 214,
            highlights = listOf("Myrtle beech", "Tree fern gully", "Canopy walk"),
            recommendedGear = listOf("Warm layer", "Waterproof jacket")
        ),
        Trail(
            id = "trail_werribee_gorge",
            name = "Werribee Gorge Circuit",
            region = "Werribee Gorge State Park",
            activityTypes = setOf(ActivityType.HIKING),
            description = "A rugged circuit around a deep river gorge, including a rock ledge " +
                "section with a fixed cable above the water. Very exposed and hot in summer; " +
                "the river crossing is impassable after heavy rain.",
            difficulty = Difficulty.HARD,
            distanceKm = 10.0,
            elevationGainM = 400,
            estimatedMinutes = 240,
            start = GeoPoint(-37.6725, 144.3161),
            route = listOf(
                GeoPoint(-37.6725, 144.3161),
                GeoPoint(-37.6748, 144.3132),
                GeoPoint(-37.6771, 144.3105),
                GeoPoint(-37.6795, 144.3084),
                GeoPoint(-37.6772, 144.3141),
                GeoPoint(-37.6725, 144.3161)
            ),
            waypoints = listOf(
                TrailWaypoint("Meikles Point", 0.0, 180, "START"),
                TrailWaypoint("Cable ledge", 3.4, 150, "LOOKOUT"),
                TrailWaypoint("Island Junction", 5.6, 120, "WATER"),
                TrailWaypoint("Eastern lookout", 8.2, 320, "LOOKOUT")
            ),
            tags = listOf("Gorge", "Cable section", "Exposed", "River crossing"),
            isExposed = true,
            rating = 4.6,
            reviewCount = 476,
            highlights = listOf("Cable ledge traverse", "Gorge walls", "River flats"),
            recommendedGear = listOf("3L water", "Sun protection", "Sturdy boots")
        )
    )
}
