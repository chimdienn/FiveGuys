package com.example.data.local

import com.example.data.model.AdventureChallenge
import com.example.data.model.AdventureStory
import com.example.data.model.ChatMessage
import com.example.data.model.CommunityGroup
import com.example.data.model.HikeBuddy
import com.example.data.model.SharedGearItem
import com.example.data.model.SpeciesScan
import com.example.data.model.Trail
import com.example.data.model.TrailMoment
import com.example.data.model.TrailPoint
import com.example.data.model.TripMeal
import com.example.data.model.TripParticipant
import com.example.data.model.TripPlan
import com.example.data.model.UserAccount
import com.example.data.model.UserProfile

suspend fun populateInitialData(dao: BiomateDao) {
    // 1. Trails
    val trails = listOf(
        Trail(
            id = "trail_wilsons_prom",
            title = "Wilson's Promontory - Sealers Cove & Oberon Loop",
            region = "Gippsland, Victoria",
            stateOrCountry = "Australia",
            distanceKm = 19.2,
            elevationGainM = 540,
            durationHours = 6.5,
            difficulty = "Moderate",
            terrain = "Coastal Heath, Boardwalks & Granite Rocks",
            rating = 4.9,
            reviewCount = 384,
            description = "Iconic loop featuring crystal clear tidal beaches, lush temperate rainforest ferns, panoramic coastal overlooks, and pristine native eucalyptus woodlands.",
            highlights = listOf("Sealers Cove Tidal Flat", "Mount Oberon 360° Lookout", "Refuge Cove Anchorage", "Native Bird Sanctuary"),
            recommendedGear = listOf("Water filter", "Gaiters", "Trekking poles", "Tidal chart guide", "Headlamp", "Electrolytes"),
            currentTempC = 22,
            weatherCondition = "Sunny",
            fireDangerLevel = "Low",
            routeWaypoints = listOf(
                TrailPoint("Telegraph Saddle Trailhead", 0.0, 220, "START"),
                TrailPoint("Windy Saddle Rest", 3.2, 340, "LOOKOUT"),
                TrailPoint("Rainforest Creek Crossing", 6.8, 80, "WATER"),
                TrailPoint("Sealers Cove Beach Camp", 10.4, 5, "CAMPSITE"),
                TrailPoint("Refuge Cove North Overlook", 14.8, 190, "LOOKOUT"),
                TrailPoint("Oberon Bay Saddle", 17.5, 110, "LOOKOUT"),
                TrailPoint("Telegraph Saddle Return", 19.2, 220, "END")
            ),
            isSaved = true,
            isVisited = false
        ),
        Trail(
            id = "trail_grampians_pinnacle",
            title = "Grampians - The Pinnacle & Wonderland Loop",
            region = "Gariwerd, Western Victoria",
            stateOrCountry = "Australia",
            distanceKm = 18.4,
            elevationGainM = 612,
            durationHours = 6.2,
            difficulty = "Challenging",
            terrain = "Granite Escarpment, Rock Scrambles & Gorges",
            rating = 4.8,
            reviewCount = 520,
            description = "A rugged wonderland hike through dramatic sandstone cliffs, the famous Grand Canyon canyon chasms, and the breathtaking sheer drop cliff at The Pinnacle overlooking Lake Bellfield.",
            highlights = listOf("Grand Canyon Gorge", "Silent Street Chasm", "The Pinnacle Cliff Edge", "Lake Bellfield Vista"),
            recommendedGear = listOf("Sturdy hiking boots with grip", "2.5L Water minimum", "First aid kit", "Sun protection", "Gloves for rock scramble"),
            currentTempC = 24,
            weatherCondition = "Partly Cloudy",
            fireDangerLevel = "Moderate",
            routeWaypoints = listOf(
                TrailPoint("Halls Gap Caravan Hub", 0.0, 230, "START"),
                TrailPoint("Venus Baths Pools", 1.8, 260, "WATER"),
                TrailPoint("Grand Canyon Ascent", 4.2, 450, "LOOKOUT"),
                TrailPoint("Silent Street Crevice", 7.6, 680, "LOOKOUT"),
                TrailPoint("The Pinnacle Summit Edge", 10.1, 780, "SUMMIT"),
                TrailPoint("Sundial Peak Lookout", 13.5, 620, "LOOKOUT"),
                TrailPoint("Wonderland Return Trailhead", 18.4, 230, "END")
            ),
            isSaved = true,
            isVisited = true
        ),
        Trail(
            id = "trail_cathedral_range",
            title = "Cathedral Range - Razorback & Wells Cave Scramble",
            region = "Yarra Valley & High Country",
            stateOrCountry = "Australia",
            distanceKm = 11.5,
            elevationGainM = 780,
            durationHours = 5.0,
            difficulty = "Hard",
            terrain = "Grade 4/5 Exposed Knife-Edge Ridge & Boulder Scrambles",
            rating = 4.9,
            reviewCount = 290,
            description = "Melbourne's most thrilling ridge scramble. Squeeze through the narrow Wells Cave before tackling the exposed knife-edge Razorback spine with jaw-dropping 360-degree vistas.",
            highlights = listOf("Wells Cave Chimney Climb", "Sugarloaf Peak 912m", "The Razorback Spine", "Jawbone Creek Camp"),
            recommendedGear = listOf("Grippy approach shoes", "Compact daypack (for tight cave)", "3L Water", "Navigation app offline", "Emergency whistle"),
            currentTempC = 21,
            weatherCondition = "Sunny",
            fireDangerLevel = "Low",
            routeWaypoints = listOf(
                TrailPoint("Cooks Mill Campground", 0.0, 310, "START"),
                TrailPoint("Wells Cave Base", 2.4, 520, "POINT"),
                TrailPoint("Sugarloaf Peak", 4.5, 912, "SUMMIT"),
                TrailPoint("The Razorback Knife-Edge", 6.8, 860, "LOOKOUT"),
                TrailPoint("North Jawbone Peak", 8.9, 790, "LOOKOUT"),
                TrailPoint("Jawbone Track Descent", 11.5, 310, "END")
            ),
            isSaved = false,
            isVisited = false
        ),
        Trail(
            id = "trail_dandenong_1000_steps",
            title = "Dandenong Ranges - 1000 Steps & Kokoda Memorial Walk",
            region = "Ferny Creek, Melbourne",
            stateOrCountry = "Australia",
            distanceKm = 5.2,
            elevationGainM = 310,
            durationHours = 2.0,
            difficulty = "Moderate",
            terrain = "Steep Stone & Wooden Steps, Mountain Ash Canopy",
            rating = 4.6,
            reviewCount = 740,
            description = "A famous fitness pilgrimage through lush temperate rainforest towering Mountain Ash trees and cascading tree ferns. Perfect for afternoon training and conditioning.",
            highlights = listOf("Memorial Plaques", "Giant Tree Fern Forest", "One Tree Hill Picnic Reserve", "Lyrebird Song Spot"),
            recommendedGear = listOf("Trail runners", "Water bottle 1L", "Towel", "Rain jacket"),
            currentTempC = 19,
            weatherCondition = "Breezy",
            fireDangerLevel = "Low",
            routeWaypoints = listOf(
                TrailPoint("Ferntree Gully Picnic Ground", 0.0, 120, "START"),
                TrailPoint("Step 250 - Fern Gully", 0.8, 190, "LOOKOUT"),
                TrailPoint("Step 700 - Kokoda Crest", 1.8, 340, "POINT"),
                TrailPoint("One Tree Hill Summit", 2.6, 430, "SUMMIT"),
                TrailPoint("Lyrebird Track Loop Back", 5.2, 120, "END")
            ),
            isSaved = false,
            isVisited = true
        )
    )
    dao.insertTrails(trails)

    // 2. Hike Buddies (HikeMatch)
    val buddies = listOf(
        HikeBuddy(
            id = "buddy_sarah_c",
            name = "Sarah Chen",
            age = 23,
            location = "Carlton, Melbourne (3.2 km away)",
            bio = "UniMelb design student & landscape photography lover. Always stopping for birds, fungi, and golden hour summits!",
            fitnessLevel = "Advanced",
            preferredPace = "Moderate (4.5 km/h)",
            activityVibe = "Photography & Social",
            experienceYears = 4,
            matchScore = 96,
            matchReasons = listOf("Same pace preference (4.5 km/h)", "Both saved Wilson's Prom & Cathedral Range", "Shares photography interest", "98% attendance reliability"),
            preferredTrails = listOf("Wilson's Promontory", "Grampians Pinnacle", "Cradle Mountain"),
            verifiedSkills = listOf("Navigation", "Wilderness First Aid", "DSLR Nature Photography"),
            completedHikesCount = 44,
            attendanceRate = 98,
            matchStatus = "MATCHED"
        ),
        HikeBuddy(
            id = "buddy_marcus_l",
            name = "Marcus Liam",
            age = 25,
            location = "Fitzroy, Melbourne (4.8 km away)",
            bio = "Software dev on weekdays, scrambling rocks on weekends. Looking for buddies to carpool to multi-day alpine hikes.",
            fitnessLevel = "Endurance",
            preferredPace = "Fast-paced (6 km/h)",
            activityVibe = "Exploration & Training",
            experienceYears = 5,
            matchScore = 91,
            matchReasons = listOf("Carpool driver with 4 seats", "Both love rugged scrambles", "Certified first aider", "Active in UniMelb Hiking"),
            preferredTrails = listOf("Cathedral Range", "Mount Bogong", "Mount Feathertop"),
            verifiedSkills = listOf("Lead Climber", "Carpool Driver", "Winter Alpine"),
            completedHikesCount = 52,
            attendanceRate = 95,
            matchStatus = "CHATTING"
        ),
        HikeBuddy(
            id = "buddy_elena_r",
            name = "Elena Rostova",
            age = 22,
            location = "South Yarra, Melbourne (5.1 km away)",
            bio = "Outdoor biology major! I identify every wildflower and moss along the trail. Looking for relaxed group walks.",
            fitnessLevel = "Moderate",
            preferredPace = "Leisurely (3.5 km/h)",
            activityVibe = "Relaxed & Nature Exploration",
            experienceYears = 3,
            matchScore = 88,
            matchReasons = listOf("Complementary flora knowledge", "Likes relaxed pacing", "Both free this Sunday"),
            preferredTrails = listOf("Dandenong Ranges", "Great Ocean Walk", "Wilsons Prom"),
            verifiedSkills = listOf("Botany / Flora ID", "Camp Cooking", "Leave No Trace"),
            completedHikesCount = 29,
            attendanceRate = 100,
            matchStatus = "AVAILABLE"
        ),
        HikeBuddy(
            id = "buddy_jordan_k",
            name = "Jordan Kim",
            age = 24,
            location = "Brunswick, Melbourne (6.0 km away)",
            bio = "Trail runner and weekend camper. Love making campfire pour-over coffee and chasing early sunrise viewpoints.",
            fitnessLevel = "Advanced",
            preferredPace = "Moderate (4.5 km/h)",
            activityVibe = "Social & Sunrise Adventures",
            experienceYears = 4,
            matchScore = 85,
            matchReasons = listOf("Similar weekend schedule", "Shared camp cooking equipment", "High social compatibility"),
            preferredTrails = listOf("Grampians", "Lorne Waterfalls", "Cathedral Range"),
            verifiedSkills = listOf("Campfire Master", "Ultralight Packing"),
            completedHikesCount = 38,
            attendanceRate = 92,
            matchStatus = "AVAILABLE"
        ),
        HikeBuddy(
            id = "buddy_liam_g",
            name = "Liam Gallagher",
            age = 28,
            location = "Northcote, Melbourne (4.1 km away)",
            bio = "Mountain ultramarathoner and ridge scrambler. Training for 50k trail runs and fast alpine traverses. Up for early dawn starts!",
            fitnessLevel = "Endurance",
            preferredPace = "Fast-paced (6 km/h)",
            activityVibe = "Fast & Training",
            experienceYears = 6,
            matchScore = 93,
            matchReasons = listOf("Matches endurance pace", "Carries emergency inReach GPS", "Experienced in alpine weather navigation"),
            preferredTrails = listOf("Cathedral Range - Razorback", "Mount Bogong", "Mount Feathertop"),
            verifiedSkills = listOf("Alpine Navigation", "Wilderness First Responder", "Ultra Trail Running"),
            completedHikesCount = 67,
            attendanceRate = 100,
            matchStatus = "AVAILABLE"
        ),
        HikeBuddy(
            id = "buddy_chloe_v",
            name = "Chloe Vance",
            age = 21,
            location = "Hawthorn, Melbourne (5.8 km away)",
            bio = "Novice hiker with a golden retriever (Barnaby!). Looking for gentle day hikes, scenic river tracks, and friendly weekend walks.",
            fitnessLevel = "Beginner",
            preferredPace = "Leisurely (3 km/h)",
            activityVibe = "Relaxed & Wildflowers",
            experienceYears = 1,
            matchScore = 82,
            matchReasons = listOf("Dog friendly trails only", "Prefers day hikes under 10km", "High enthusiasm for scenic breaks"),
            preferredTrails = listOf("Dandenong Ranges 1000 Steps", "Warrandyte River Walk", "Yarra Bend Trail"),
            verifiedSkills = listOf("Canine First Aid", "Leave No Trace", "Nature Sketching"),
            completedHikesCount = 14,
            attendanceRate = 95,
            matchStatus = "AVAILABLE"
        ),
        HikeBuddy(
            id = "buddy_tariq_m",
            name = "Tariq Al-Mansoor",
            age = 29,
            location = "Footscray, Melbourne (6.5 km away)",
            bio = "Passionate backpacker and astrophotographer. Love sleeping under the stars, testing lightweight gear, and brewing campfire mint tea.",
            fitnessLevel = "Advanced",
            preferredPace = "Moderate (4.5 km/h)",
            activityVibe = "Social & Campfire",
            experienceYears = 5,
            matchScore = 94,
            matchReasons = listOf("Shares multi-day backpacking interest", "Brings 4-season tent and water filter", "High reliability rating"),
            preferredTrails = listOf("Wilson's Promontory", "Grampians Peaks Trail", "Great Ocean Walk"),
            verifiedSkills = listOf("Astrophotography", "Bushcraft & Shelters", "Wilderness First Aid", "Leave No Trace"),
            completedHikesCount = 49,
            attendanceRate = 98,
            matchStatus = "AVAILABLE"
        ),
        HikeBuddy(
            id = "buddy_zoe_k",
            name = "Zoe Katsaros",
            age = 26,
            location = "St Kilda, Melbourne (4.9 km away)",
            bio = "Coastal trail explorer and coffee enthusiast. Seeking companions for weekend coastal scrambles and waterfall circuits.",
            fitnessLevel = "Moderate",
            preferredPace = "Moderate (4.5 km/h)",
            activityVibe = "Photography & Nature",
            experienceYears = 3,
            matchScore = 89,
            matchReasons = listOf("Similar moderate pace", "Loves coastal & rainforest terrain", "Shares photography stops"),
            preferredTrails = listOf("Wilson's Promontory", "Lorne Waterfalls Circuit", "Mornington Coastal Walk"),
            verifiedSkills = listOf("Landscape Photography", "Trail Navigation", "Water Purification"),
            completedHikesCount = 31,
            attendanceRate = 96,
            matchStatus = "AVAILABLE"
        )
    )
    dao.insertHikeBuddies(buddies)

    // 3. Collaborative Trip Plans
    val sampleTrip = TripPlan(
        id = "trip_wilsons_prom_weekend",
        title = "Wilson's Prom Weekend Adventure",
        trailId = "trail_wilsons_prom",
        trailName = "Wilson's Promontory - Sealers Cove Loop",
        departureDate = "Sat, 29 Aug",
        returnDate = "Sun, 30 Aug",
        departureTime = "06:30 AM",
        meetingPoint = "Southern Cross Station / Oakleigh Carpool Hub",
        status = "PLANNING",
        maxParticipants = 5,
        organizerName = "Alex Rivera",
        carpoolSeatsTotal = 5,
        carpoolSeatsTaken = 4,
        emergencyContactInfo = "Parks Victoria Ranger (03) 8427 2000 / Group ICE: +61 412 345 678",
        weatherForecast = "21°C - 24°C, Low wind (12 km/h), 0% precipitation, UV Index 5",
        safetyScore = 94
    )
    dao.insertTrip(sampleTrip)

    // Trip Participants
    val participants = listOf(
        TripParticipant("p1", sampleTrip.id, "Alex Rivera (You)", "Organizer & Navigator", 0xFF1A5340, "Driver (Subaru Outback, 4 seats)", isReady = true),
        TripParticipant("p2", sampleTrip.id, "Sarah Chen", "Photographer & Flora Lead", 0xFFD6562B, "Passenger (Oakleigh Pick-up)", isReady = true),
        TripParticipant("p3", sampleTrip.id, "Marcus Liam", "First Aider & Safety Lead", 0xFF2E7D32, "Passenger (Carlton Pick-up)", isReady = true),
        TripParticipant("p4", sampleTrip.id, "Elena Rostova", "Camp Chef & Meal Coordinator", 0xFF8E24AA, "Passenger (South Yarra Pick-up)", isReady = false),
        TripParticipant("p5", sampleTrip.id, "Jordan Kim", "Fire & Camp Shelter Lead", 0xFF0288D1, "Self-drive (Meeting at Tidal River)", isReady = true)
    )
    dao.insertParticipants(participants)

    // Shared Gear List
    val gearItems = listOf(
        SharedGearItem("g1", sampleTrip.id, "MSR PocketRocket Deluxe Stove + Gas", "Cooking", "Alex Rivera (You)", isEssential = true, isPacked = true, weightKg = 0.4),
        SharedGearItem("g2", sampleTrip.id, "Wilderness First Aid Trauma Kit + Snake Bandages", "Safety", "Marcus Liam", isEssential = true, isPacked = true, weightKg = 0.9),
        SharedGearItem("g3", sampleTrip.id, "Katadyn BeFree 3L Microfilter Water Purifier", "Safety", "Sarah Chen", isEssential = true, isPacked = true, weightKg = 0.3),
        SharedGearItem("g4", sampleTrip.id, "3-Person Ultralight Tent (Hubba Hubba)", "Shelter", "Jordan Kim", isEssential = true, isPacked = false, weightKg = 1.8),
        SharedGearItem("g5", sampleTrip.id, "2-Person Ultralight Tent (Big Agnes)", "Shelter", "Sarah Chen", isEssential = true, isPacked = true, weightKg = 1.4),
        SharedGearItem("g6", sampleTrip.id, "Camp Cookware Set + Spatulas + Bio Soap", "Cooking", "Elena Rostova", isEssential = false, isPacked = true, weightKg = 0.7),
        SharedGearItem("g7", sampleTrip.id, "Garmin inReach Mini Satellite Messenger / SOS Beacon", "Navigation", "Alex Rivera (You)", isEssential = true, isPacked = true, weightKg = 0.2),
        SharedGearItem("g8", sampleTrip.id, "Trowel + Bio Degradable Waste Bags", "Safety", "Marcus Liam", isEssential = true, isPacked = true, weightKg = 0.2)
    )
    dao.insertGearItems(gearItems)

    // Trip Meals
    val meals = listOf(
        TripMeal("m1", sampleTrip.id, "High-Protein Granola, Chia & Oat Bowls + Instant Pour Over", "Breakfast", "Alex Rivera", "Vegetarian & Dairy-Free options"),
        TripMeal("m2", sampleTrip.id, "Avocado, Cheddar, Hummus & Sourdough Wraps + Trail Mix", "Trail Lunch", "Sarah Chen", "Nut-free packaging for safety"),
        TripMeal("m3", sampleTrip.id, "Dehydrated Red Lentil Curry + Jasmine Rice + Hot Cocoa", "Campfire Dinner", "Elena Rostova", "100% Gluten-free & Hearty"),
        TripMeal("m4", sampleTrip.id, "Electrolyte Chews, Beef Jerky & Dried Mango Strips", "Trail Snacks", "Jordan Kim", "Shared stash for steep climbs")
    )
    dao.insertMeals(meals)

    // Chat Messages
    val chatMessages = listOf(
        ChatMessage(tripId = sampleTrip.id, channel = "TRIP_GROUP", senderName = "Alex Rivera", messageText = "Hey team! Updated the carpool pickup order. Picking Marcus at 6:30, then Sarah at 6:45.", timestamp = "08:15 AM"),
        ChatMessage(tripId = sampleTrip.id, channel = "TRIP_GROUP", senderName = "Marcus Liam", messageText = "Sounds good! I packed the heavy-duty snake bite pressure immobilisation bandages just in case.", timestamp = "08:30 AM"),
        ChatMessage(tripId = sampleTrip.id, channel = "TRIP_GROUP", senderName = "Sarah Chen", messageText = "Awesome. I also tested the water filter flow rate yesterday, all working at 2L/min!", timestamp = "08:45 AM"),
        ChatMessage(tripId = sampleTrip.id, channel = "CAMPSITE_LOCAL", senderName = "Ranger Dave (Tidal River)", messageText = "Tidal crossing at Sealers Cove is lowest at 1:45 PM today. Safe window 12:30 - 3:00 PM.", timestamp = "09:00 AM"),
        ChatMessage(tripId = sampleTrip.id, channel = "CAMPSITE_LOCAL", senderName = "Liam @ Camp Bay 4", messageText = "Fresh drinking water tap at Sealers Cove shelter is flowing crystal clear today.", timestamp = "09:20 AM")
    )
    for (msg in chatMessages) {
        dao.insertChatMessage(msg)
    }

    // 4. Trail Moments / Hazard Reporting
    val moments = listOf(
        TrailMoment("tm1", "trail_wilsons_prom", "LOOKOUT", "Spectacular view of Sealers Cove beach!", "Clear azure waters with calm waves. Perfect spot for 15-min snack break.", "Sarah Chen", "2h ago", 24, true, 10.4, "INFO"),
        TrailMoment("tm2", "trail_wilsons_prom", "WATER_SOURCE", "Fresh spring water station flowing", "Tank at Sealers shelter is 80% full. Filter recommended as usual.", "Alex R.", "3h ago", 18, true, 10.2, "INFO"),
        TrailMoment("tm3", "trail_wilsons_prom", "WILDFLOWER", "Purple Sun Orchids blooming", "Dense cluster of native orchids along the boardwalk on the north flank.", "Elena R.", "5h ago", 31, true, 5.8, "INFO"),
        TrailMoment("tm4", "trail_wilsons_prom", "HAZARD_SNAKE", "Red-bellied black snake basking", "Spotted sunbathing on warm granite boulder at km 12.3. Kept safe distance, moved off into scrub.", "Marcus L.", "45m ago", 42, true, 12.3, "CAUTION"),
        TrailMoment("tm5", "trail_wilsons_prom", "HAZARD_BLOCKED", "Large fallen eucalypt branch across boardwalk", "Easy to step over, but watch slippery moss on the trunk.", "Jordan K.", "1h ago", 15, true, 7.1, "INFO"),
        TrailMoment("tm6", "trail_wilsons_prom", "SUNSET_SPOT", "Mount Oberon panoramic sunset point", "360-degree ocean horizon. Catch the amber glow around 6:10 PM!", "Sarah Chen", "1d ago", 58, true, 17.5, "INFO")
    )
    dao.insertTrailMoments(moments)

    // 5. Species Scan (Field Journal)
    val species = listOf(
        SpeciesScan(
            id = "spec_1",
            commonName = "Crimson Rosella",
            scientificName = "Platycercus elegans",
            category = "BIRD",
            confidence = 98,
            description = "Vibrant native parrot with deep crimson plumage, royal blue cheeks and wings. Frequently found in eucalypt canopy.",
            habitat = "Wet sclerophyll and temperate rain forests of SE Australia",
            isNative = true,
            safetyNote = "Harmless native wildlife. Do not feed human food.",
            foundAtLocation = "Windy Saddle, Wilson's Prom",
            timestamp = "Today, 10:15 AM",
            tripId = sampleTrip.id
        ),
        SpeciesScan(
            id = "spec_2",
            commonName = "Coast Banksia",
            scientificName = "Banksia integrifolia",
            category = "PLANT",
            confidence = 96,
            description = "Hardy native coastal tree bearing upright cylindrical yellow flower spikes rich in nectar for honeyeaters.",
            habitat = "Coastal sand dunes and headlands",
            isNative = true,
            safetyNote = "Non-toxic. Key pollinator species for biodiversity.",
            foundAtLocation = "Sealers Cove Track km 4.5",
            timestamp = "Today, 11:40 AM",
            tripId = sampleTrip.id
        ),
        SpeciesScan(
            id = "spec_3",
            commonName = "Ghost Fungus",
            scientificName = "Omphalotus nidiformis",
            category = "MUSHROOM",
            confidence = 94,
            description = "Bioluminescent fan-shaped mushroom that glows an eerie pale green in total night darkness.",
            habitat = "Decaying eucalypt hardwood and dead stumps",
            isNative = true,
            safetyNote = "TOXIC if ingested. Causes severe vomiting. Safe to observe and photograph.",
            foundAtLocation = "Fern Rainforest Gully",
            timestamp = "Yesterday, 07:30 PM",
            tripId = sampleTrip.id
        ),
        SpeciesScan(
            id = "spec_4",
            commonName = "Eastern Grey Kangaroo Track",
            scientificName = "Macropus giganteus (Track)",
            category = "TRACK",
            confidence = 92,
            description = "Distinctive elongated hind foot print (approx 22-26cm) with prominent central claw indentation in moist sand.",
            habitat = "Coastal grasslands and open woodland clearings",
            isNative = true,
            safetyNote = "Wild macropod. Maintain 10m respectful distance.",
            foundAtLocation = "Oberon Bay Shoreline",
            timestamp = "Today, 02:10 PM",
            tripId = sampleTrip.id
        )
    )
    dao.insertSpeciesScans(species)

    // 6. Adventure Stories (Post-Trip Memory)
    val stories = listOf(
        AdventureStory(
            id = "story_grampians_epic",
            tripId = "trip_grampians_past",
            trailTitle = "Grampians Wonderland Loop",
            dateFormatted = "16 August 2026",
            totalDistanceKm = 18.4,
            totalDurationHours = 6.2,
            participantsSummary = "Alex, Sarah, Marcus, Elena, Jordan, Maya (6 friends)",
            keyMilestones = listOf("The Pinnacle Summit (780m)", "Silent Street Chasm", "Grand Canyon Ascent", "Lake Bellfield Golden Hour"),
            highlightsNarrative = "Conquered the sheer heights of The Pinnacle together with 6 friends on a crisp blue-sky winter day. Sarah spotted a wedge-tailed eagle soaring over the valley, while Elena made warm spiced chai at Sundial lookout.",
            speciesDiscoveredCount = 14,
            elevationGainM = 612
        )
    )
    dao.insertAdventureStories(stories)

    // 7. Community Groups
    val groups = listOf(
        CommunityGroup("g_unimelb", "UniMelb Hiking Club", "University & Youth", 1420, "Active student & alumni outdoor group exploring Victorian peaks, coastal tracks, and beginner-friendly day hikes.", "Mount Feathertop Ridge Run", "Sat, 5 Sep", 12400, isJoined = true),
        CommunityGroup("g_vic_bird", "Birdwatching Victoria Youth", "Fauna & Conservation", 680, "Spotting rare parrots, raptors, and coastal seabirds with binocular walks and ethical photography.", "Sherbrooke Forest Lyrebird Search", "Sun, 30 Aug", 4820, isJoined = true),
        CommunityGroup("g_trail_run", "Weekend Trail Runners Melbourne", "Fitness & Endurance", 950, "Pounding dirt trails across Dandenongs, You Yangs, and Warrandyte with post-run bakery stops.", "Dandenong 21k Sky Loop", "Sat, 29 Aug", 18900, isJoined = false),
        CommunityGroup("g_casual_camp", "Casual Camping & Stargazing", "Social & Campfire", 2100, "Relaxed weekend overnighters with shared cooking gear, astronomy telescope sessions, and acoustic campfires.", "Cathedral Range Overnighter", "Fri, 11 Sep", 8700, isJoined = false)
    )
    dao.insertCommunityGroups(groups)

    // 8. Adventure Challenges (Cooperative & Solo)
    val challenges = listOf(
        AdventureChallenge("c1", "Walk 50 km Outdoors", "Explore at least 50 km of nature trails this month", 34, 50, "km", false, "Monthly Goal", "Trail Voyager"),
        AdventureChallenge("c2", "Discover 10 Native Species", "Photograph and identify 10 indigenous Australian flora/fauna", 7, 10, "species", false, "Field Journal", "Master Naturalist"),
        AdventureChallenge("c3", "Cooperative: Explore 500 km Together", "Cumulative trail distance covered by your Biomate squad", 486, 500, "km", true, "Group Milestone", "Squad Explorer"),
        AdventureChallenge("c4", "Sunrise Summit Chaser", "Reach a designated mountain summit before 07:00 AM", 2, 3, "summits", false, "Adventure Quest", "Dawn Patrol"),
        AdventureChallenge("c5", "Team: 10 Victorian National Parks", "Visit 10 different parks with at least one Biomate partner", 7, 10, "parks", true, "Community Quest", "Park Ranger Elite")
    )
    dao.insertChallenges(challenges)

    // 9. User Profile & Accounts
    val profile = UserProfile()
    dao.insertUserProfile(profile)

    // 10. Multi-User Accounts
    val accounts = listOf(
        UserAccount(
            id = "user_alex",
            email = "alex@biomate.outdoors",
            password = "trail2026",
            name = "Alex Rivera",
            handle = "@alex_outdoors",
            bio = "Trail enthusiast & amateur botanist based in Melbourne. Love alpine scrambles and sunrise summits.",
            avatarInitials = "AR",
            avatarColorHex = 0xFFCD744C, // Terracotta
            fitnessLevel = "Advanced",
            preferredPace = "Moderate (4.5 km/h)",
            preferredVibe = "Exploration & Photography",
            location = "Melbourne, Victoria",
            totalHikes = 42,
            overnightTrips = 6,
            totalKmExplored = 310,
            attendanceRate = 96,
            repeatHikerCount = 11,
            groupTripsOrganized = 8,
            verifiedSkills = listOf("Wilderness First Aid", "Topographic Navigation", "Multi-day Camping", "Leave No Trace"),
            badges = listOf("Trail Master", "Flora Scout", "Reliable Pacer", "Summit Chaser", "Gear Guru"),
            isLoggedIn = true
        ),
        UserAccount(
            id = "user_sarah",
            email = "sarah@trailblazer.io",
            password = "trail2026",
            name = "Sarah Chen",
            handle = "@sarah_runs",
            bio = "Ultra-distance trail runner, orienteering competitor, and weekend ridge scrambler.",
            avatarInitials = "SC",
            avatarColorHex = 0xFF1B4938, // Forest Green
            fitnessLevel = "Endurance / Expert",
            preferredPace = "Fast-paced (6 km/h)",
            preferredVibe = "Training & Peak Bagging",
            location = "Dandenong Ranges, VIC",
            totalHikes = 68,
            overnightTrips = 14,
            totalKmExplored = 720,
            attendanceRate = 100,
            repeatHikerCount = 19,
            groupTripsOrganized = 12,
            verifiedSkills = listOf("Wilderness First Responder", "Alpine Navigation", "Ultra Endurance", "Bushcraft"),
            badges = listOf("Ultra Trailblazer", "Ridge Runner", "Fast Pacer", "Safety First"),
            isLoggedIn = false
        ),
        UserAccount(
            id = "user_marcus",
            email = "marcus@wildsummit.com",
            password = "trail2026",
            name = "Marcus Thorne",
            handle = "@marcus_wilderness",
            bio = "Backpacking guide, camp chef enthusiast, and geology buff. Always carrying good coffee and extra dry bags.",
            avatarInitials = "MT",
            avatarColorHex = 0xFFD97706, // Amber Ochre
            fitnessLevel = "Moderate",
            preferredPace = "Leisurely (3.5 km/h)",
            preferredVibe = "Casual Camping & Stargazing",
            location = "Grampians, VIC",
            totalHikes = 35,
            overnightTrips = 22,
            totalKmExplored = 410,
            attendanceRate = 94,
            repeatHikerCount = 15,
            groupTripsOrganized = 16,
            verifiedSkills = listOf("Campfire Cooking", "Leave No Trace Master", "Emergency Shelter", "Astronavigation"),
            badges = listOf("Campfire Chef", "Star Gazer", "Heavy Packer", "Trail Steward"),
            isLoggedIn = false
        ),
        UserAccount(
            id = "user_maya",
            email = "maya@wanderlust.net",
            password = "trail2026",
            name = "Maya Patel",
            handle = "@maya_captures",
            bio = "Landscape photographer capturing golden hours across Victoria. Easy-going pace with frequent photo breaks.",
            avatarInitials = "MP",
            avatarColorHex = 0xFF4F46E5, // Indigo
            fitnessLevel = "Beginner / Casual",
            preferredPace = "Leisurely (3 km/h)",
            preferredVibe = "Photography & Nature Walks",
            location = "Yarra Valley, VIC",
            totalHikes = 19,
            overnightTrips = 3,
            totalKmExplored = 120,
            attendanceRate = 98,
            repeatHikerCount = 8,
            groupTripsOrganized = 2,
            verifiedSkills = listOf("Nature Photography", "Bird ID", "Trail Safety Basics"),
            badges = listOf("Golden Hour Eye", "Flora Scout", "Weekend Wanderer"),
            isLoggedIn = false
        )
    )
    dao.insertUserAccounts(accounts)
}
