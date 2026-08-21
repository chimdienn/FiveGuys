# Biomate — Complete End-to-End MVP

## Existing Repository

Repository:

`https://github.com/chimdienn/FiveGuys`

You are the lead Android/full-stack engineer responsible for turning the existing Biomate prototype into a complete, working end-to-end MVP.

This is NOT a greenfield project.

Do NOT rebuild the app in React Native, Expo, Flutter, Swift, or another framework.

The existing Android application is the foundation of the MVP.

Your job is to:

1. Audit the existing repository.
2. Preserve useful existing UI/UX and architecture.
3. Replace mock/local-only functionality with real implementations.
4. Connect the application to a real backend.
5. Implement missing features.
6. Make the complete user journey work across multiple user accounts.
7. Test and polish the application.
8. Leave the repository in a runnable, maintainable state.

Do not stop at planning.

Actually modify and implement the code.

---

# 1. Product Vision

Biomate is a social outdoor adventure application.

Biomate helps young adults:

* discover outdoor activities
* find compatible people
* organise adventures together
* prepare safely
* navigate trails
* communicate with their group
* record trail hazards and discoveries
* complete outdoor challenges
* earn BioCoins and badges
* track their outdoor activity history

Primary activities:

* Hiking
* Camping
* Trail running
* Cycling

Architecture should allow additional activities later:

* Skiing
* Climbing
* Kayaking
* Mountain biking
* Walking
* Photography trips

The main product loop is:

```text
Discover
   ↓
Match
   ↓
Connect
   ↓
Plan
   ↓
Prepare
   ↓
Adventure
   ↓
Record
   ↓
Complete
   ↓
Earn
   ↓
Repeat
```

The MVP is successful when this entire loop works.

---

# 2. Existing Technology

Preserve the existing Android architecture unless there is a strong technical reason to refactor something.

The project currently uses or is structured around:

* Kotlin
* Jetpack Compose
* AndroidX
* Compose Navigation
* ViewModel
* Repository pattern
* Room
* Coroutines
* Retrofit
* Moshi
* Firebase libraries

Use this as the foundation.

Do not perform a framework rewrite.

---

# 3. Target Architecture

Use:

## Android

* Kotlin
* Jetpack Compose
* Compose Navigation
* Material 3
* Coroutines
* Flow / StateFlow
* ViewModel

## Local persistence

* Room

## Cloud backend

Use Firebase:

* Firebase Authentication
* Cloud Firestore
* Firebase Storage
* Firebase App Check
* Cloud Functions where server-side authority is required

## Maps

* Google Maps Compose

## GPS

* Google Play Services Location

## Camera

* CameraX

## AI

Use the Firebase AI / Gemini infrastructure already compatible with the application where practical.

All AI functionality must be hidden behind interfaces so the implementation can later be changed.

---

# 4. Architecture Principle

The UI must never directly communicate with Firebase.

Use:

```text
Jetpack Compose UI
        ↓
ViewModel
        ↓
Repository
        ↓
Local + Remote Data Sources
     ↙                 ↘
   Room              Firebase
```

External APIs must also be accessed through service/repository abstractions.

Example:

```kotlin
interface TrailRepository {
    fun observeTrails(): Flow<List<Trail>>
    suspend fun getTrail(id: String): Trail?
    suspend fun saveTrail(id: String)
}
```

Do not scatter Firestore code across Compose screens.

---

# 5. Existing Code Rule

Before building anything:

Inspect the entire repository.

Determine which features already exist as:

* working implementation
* UI-only prototype
* hard-coded data
* mock data
* Room-only implementation
* partially implemented feature
* obsolete code
* duplicate code

Reuse existing screens and components wherever possible.

Do NOT create a second implementation of a feature if the existing implementation can reasonably be extended.

The repository already includes screens/features corresponding to concepts such as:

* Authentication
* Home
* Discover
* HikeMatch
* Trip Planner
* Messages
* OnTrail
* Photo Scan
* Profile
* Communities
* Adventure Memories

Treat existing UI as the visual starting point.

---

# 6. MVP User Journey

This exact journey must work end-to-end.

```text
Create account
      ↓
Complete outdoor profile
      ↓
Open Home
      ↓
See weather + challenges + trails
      ↓
Discover a trail
      ↓
View trail details
      ↓
Find compatible people
      ↓
Send connection request
      ↓
Other user accepts
      ↓
Create a trip
      ↓
Invite connected users
      ↓
Participants join
      ↓
Assign shared equipment
      ↓
Complete readiness checklist
      ↓
Use group chat
      ↓
Start adventure
      ↓
See GPS position on trail
      ↓
Create trail moments
      ↓
Complete trip
      ↓
Complete applicable challenges
      ↓
Receive BioCoins
      ↓
Unlock badges where applicable
      ↓
Profile statistics update
      ↓
Trip appears in history
```

The MVP is NOT complete until this works with persistent data between at least two different user accounts.

---

# 7. Authentication

Replace prototype/local authentication.

Use Firebase Authentication.

Implement:

* Create account
* Email/password login
* Persistent authentication session
* Logout
* Password reset
* Loading state
* Authentication errors

Optional after these work:

* Google Sign-In

Do not prioritise:

* Facebook login
* Apple login

unless already easy to support.

---

# 8. Authentication Security

Remove any plaintext password storage from:

* Room
* Firestore
* models
* repository
* test seed data except explicitly isolated development credentials

Passwords belong only to Firebase Authentication.

User records should reference Firebase UID.

Example:

```text
firebase uid
      ↓
profile
      ↓
user-specific app data
```

---

# 9. Onboarding

After registration, new users complete onboarding.

Ask for:

## Basic information

* Display name
* Profile image optional
* Bio optional
* Age or birth year
* Gender optional
* Approximate location optional

Never require precise home location.

---

## Outdoor interests

Allow multi-select:

* Hiking
* Camping
* Trail running
* Cycling

---

## Fitness level

Example:

* Beginner
* Moderate
* Fit
* Very fit

---

## Outdoor experience

Example:

* New
* Beginner
* Intermediate
* Experienced
* Expert

---

## Preferred pace

Example:

* Relaxed
* Moderate
* Fast
* Training

---

## Social style

Multi-select or primary preference:

* Relaxed
* Social
* Photography
* Fast-paced
* Training
* Exploration

---

## Skills

Examples:

* Navigation
* First Aid
* Camping
* Outdoor Cooking
* Trail Running
* Climbing

Save onboarding to the user's cloud profile.

---

# 10. Home Screen

Preserve and improve the current Home screen.

The Home page should contain:

## Header

Example:

```text
Good morning, Alex 👋
Melbourne · 18°C
```

---

## Biomate Character Placeholder

The product will eventually contain a customisable 3D chibi character/pet.

Do NOT implement the real 3D system in MVP.

Create an intentional placeholder.

Example component:

```kotlin
@Composable
fun BiomateCharacter(
    userId: String,
    mood: CharacterMood = CharacterMood.Happy,
    level: Int
)
```

For MVP render:

* illustration/placeholder
* subtle animation
* BioCoin amount
* user level if implemented

Structure the component so the renderer can later be replaced by a 3D model.

---

# 11. Home Content

Below the character show cards.

## Weather

Display:

* current temperature
* weather
* precipitation
* wind
* relevant outdoor warning if available

Use a WeatherService abstraction.

---

## Recommended Adventure

Recommend one suitable trail.

---

## Daily Challenges

Show approximately three challenges.

Example:

```text
Walk 5 km outdoors

3.2 / 5 km

+50 BioCoins
```

---

## Upcoming Trip

Show the user's nearest upcoming trip.

---

## Recommended Trails

Horizontal cards.

---

## Outdoor Updates

Examples:

* suitable hiking conditions
* trail information
* weather changes

Do not build a full news aggregation service for MVP.

---

## Leaderboard

Small BioCoin leaderboard.

Example top 5.

---

# 12. Trail Discovery

Implement a functional Discover screen.

Users can:

* browse trails
* search
* filter
* save trails
* open trail details

Filters:

* Activity
* Difficulty
* Distance
* Duration

Optional:

* Region

---

# 13. Trail Model

Trail should support:

```text
id
name
region
activity type
description
difficulty
distance
elevation gain
estimated duration
start coordinates
route/polyline
image
tags
```

Use realistic seed data.

Do not scrape copyrighted commercial trail databases.

Use public/open trail data where practical or clearly labelled seed/demo routes.

---

# 14. Trail Detail Screen

Display:

* Hero image
* Trail name
* Region
* Difficulty
* Distance
* Elevation gain
* Estimated time
* Activity type
* Description
* Weather
* Map preview
* Route
* Recent community moments

Actions:

* Save
* Create Trip
* Find People

---

# 15. Weather Service

Create:

```kotlin
interface WeatherService {
    suspend fun getWeather(
        latitude: Double,
        longitude: Double
    ): Weather
}
```

Use a free public weather provider such as Open-Meteo where practical.

External service-specific code must remain isolated.

---

# 16. Weather-Based Trail Recommendation

Implement a simple rule-based recommendation engine.

Do not build ML.

Examples:

### Heavy rain

Reduce recommendation score of:

* exposed trails
* difficult trails
* steep trails

### Strong winds

Reduce recommendation score of exposed trails.

### High temperature

Prefer:

* shorter routes
* shaded routes
* less elevation

### Comfortable weather

Normal ranking.

Function example:

```kotlin
fun rankTrailsForWeather(
    trails: List<Trail>,
    weather: Weather
): List<TrailRecommendation>
```

Recommendations must not claim to guarantee safety.

---

# 17. HikeMatch

The purpose of HikeMatch is:

> Find compatible people for specific outdoor activities.

It is NOT romantic dating.

Preserve the existing card/swipe experience where appropriate.

A suggested profile should show:

* Profile image/avatar
* Display name
* Approximate region
* Activities
* Fitness
* Experience
* Pace
* Social preferences
* Skills
* Compatibility score
* Reasons for compatibility

Actions:

* Skip
* View Profile
* Connect

---

# 18. Matching Algorithm

Do not use machine learning.

Implement deterministic weighted compatibility.

Starting weights:

```text
Activity overlap        25
Experience compatibility 20
Fitness compatibility    15
Preferred pace           15
Social style             15
Approximate location     10
                         ---
                         100
```

Create a pure function:

```kotlin
fun calculateCompatibility(
    userA: UserProfile,
    userB: UserProfile
): CompatibilityResult
```

Return:

```kotlin
data class CompatibilityResult(
    val score: Int,
    val reasons: List<String>
)
```

Example:

```text
84%

• You both enjoy hiking and camping
• Similar preferred pace
• Both prefer social adventures
```

Write unit tests for this logic.

---

# 19. User Search

Also allow users to search directly.

Search by:

* display name

Optionally:

* username if implemented

Users can:

* open profile
* send connection request

---

# 20. Connections

Implement connection relationships.

States:

```text
pending
accepted
rejected
```

Optional later:

```text
blocked
```

Users should be able to:

* send request
* accept
* reject
* view connections
* remove connection

Do not expose private user data through connections.

---

# 21. Trip Planning

Users can create an outdoor trip.

Trip fields:

```text
title
trail
creator
date
departure time
meeting point
participant limit optional
carpool notes
food notes
general notes
emergency information
status
```

Trip states:

```text
planning
active
completed
cancelled
```

---

# 22. Trip Participants

Users can:

* invite connections
* accept trip invite
* join permitted trips
* leave trips
* view participants

Roles:

```text
organiser
participant
```

The organiser can manage the trip.

---

# 23. Collaborative Gear

Implement shared equipment planning.

Examples:

```text
First Aid Kit
Tent
Stove
Water Filter
Satellite communicator
Cooking equipment
```

Each item:

```text
name
quantity
assigned user
packed/not packed
```

Example UI:

```text
✓ First Aid Kit
  Alex

○ Stove
  Sarah
```

Participants can update their assigned equipment.

Organiser can assign equipment.

---

# 24. Food and Carpool

Keep these intentionally simple.

## Carpool

Trip notes such as:

```text
Alex — 3 seats available
Meet 06:30 at UniMelb
```

## Food

Shared text/checklist.

Do not build logistics optimisation.

---

# 25. PreTrail Readiness

Before starting an adventure, show a preparation checklist.

Suggested items:

* Weather checked
* Water packed
* Suitable footwear
* Navigation available
* First aid available
* Phone charged
* Emergency contact available
* Appropriate clothing
* Food packed where required

Ask:

```text
How prepared do you feel?
```

Optional:

```text
Any issues or injuries to consider?
```

Users complete their own checklist.

Do not allow another participant to falsify their readiness state.

Readiness is guidance only.

Do not claim Biomate guarantees user safety.

---

# 26. Messaging

Messaging is essential.

The Messages screen contains:

## Trips

Group conversations.

Examples:

```text
Wilson's Prom Weekend
Overland Track
Grampians Adventure
```

## People

Direct conversations with accepted connections.

---

# 27. Messaging Backend

Use Cloud Firestore realtime listeners.

Support:

* realtime text messages
* sender
* timestamp
* ordering
* simple unread indication
* loading
* empty state
* errors

Do not build your own websocket server.

---

# 28. Conversation Types

Support:

```text
DIRECT
TRIP
```

A direct conversation belongs to two users.

A trip conversation belongs to trip participants.

Only members may read/write that conversation.

Enforce this through Firebase security rules.

---

# 29. OnTrail Mode

This is one of the MVP's core features.

Preserve the existing OnTrail UI where useful, but replace fake map/location behaviour.

Use:

* Google Maps Compose
* Google Play Services Location

Request foreground location permission.

Do NOT implement continuous background tracking for MVP.

---

# 30. OnTrail Screen

The main focus should be the map.

Display:

* trail route
* user current position
* start point
* end point
* trail moments
* trip progress

Show compact adventure statistics.

Example:

```text
2h 14m

7.8 km

62% complete
```

Actions:

* Add Moment
* Trail Info
* Weather
* Finish Adventure

---

# 31. GPS Tracking

When adventure starts:

1. Ask for foreground location permission.
2. Start location updates.
3. Record necessary session information.
4. Update map marker.
5. Calculate approximate travelled distance.
6. Calculate progress relative to trail.

Use appropriate location update intervals to avoid unnecessary battery drain.

Do not attempt turn-by-turn navigation.

---

# 32. Trail Progress

Estimate progress using:

* current location
* trail polyline
* distance along route

An approximate robust MVP implementation is acceptable.

Do not show impossible precision.

---

# 33. Trail Moments

Users can leave observations at their current physical location.

Moment categories:

```text
Hazard
Note
Photo
Wildlife
Viewpoint
Water
Trail Condition
```

Examples:

```text
Tree blocking trail

Snake spotted here

Water source flowing today

Very muddy section

Great viewpoint

Wildflowers blooming
```

---

# 34. Trail Moment Location Rule

A user may only create a Trail Moment approximately at their current GPS location.

Do not provide arbitrary map-pin placement for MVP.

When created:

```text
moment coordinates = current verified device location
```

Allow reasonable GPS tolerance.

---

# 35. Trail Moment Fields

Store:

```text
id
creator
trail
trip optional
latitude
longitude
category
description
photo optional
visibility
timestamp
```

Visibility:

```text
PUBLIC
CONNECTIONS
TRIP
```

---

# 36. Trail Moment Filters

Map can filter:

* All
* Hazards
* Notes
* Photos
* Wildlife
* Water
* Viewpoints
* Conditions

Also optionally:

* Public
* Trip
* Connections

---

# 37. Time Sensitivity

Hazards and conditions can become outdated.

Display age.

Example:

```text
Tree blocking trail

Reported 2 hours ago
```

or:

```text
Reported 12 days ago
```

Avoid implying old reports are current.

---

# 38. Daily Challenges

Each user receives daily outdoor challenges.

Examples:

```text
Walk 5 km outside
+30 BioCoins

Complete one trail
+100 BioCoins

Record one trail observation
+20 BioCoins

Complete a trip with 3 people
+120 BioCoins

Take a photo of a mountain
+50 BioCoins
```

For MVP assign around three daily challenges.

---

# 39. Challenge Verification

Support types:

```text
DISTANCE
TRIP_COMPLETE
GROUP_TRIP
TRAIL_MOMENT
PHOTO
MANUAL_DEV
```

Automatic verification where practical.

Do not make complex anti-cheat systems.

---

# 40. BioCoins

BioCoins are in-app points.

They are NOT:

* cryptocurrency
* money
* transferable currency
* purchasable currency

For MVP they are earned only through challenge completion.

---

# 41. BioCoin Security

The Android client must NOT be able to arbitrarily increase BioCoins.

Use server-authoritative transactions.

Example:

```text
Challenge completed
       ↓
Server verifies requirement
       ↓
Check reward has not already been issued
       ↓
Create BioCoin transaction
       ↓
Balance changes
```

Use a Cloud Function or equivalent trusted Firebase backend process.

---

# 42. BioCoin Transactions

Store transactions such as:

```text
id
user
amount
reason
challenge id
reference id
created at
```

Make reward operations idempotent.

A challenge may NEVER award twice.

Write tests for duplicate reward prevention.

---

# 43. Leaderboard

Home leaderboard displays users ranked by BioCoins.

Limit MVP to something such as top 10.

Show:

* name
* avatar
* BioCoins
* rank

Avoid overcomplicating leaderboard periods.

---

# 44. Badges

Implement approximately six badges.

Examples:

## First Steps

Complete first trail.

## Explorer

Complete five trails.

## Trail Regular

Explore 50 km.

## Social Hiker

Complete three group trips.

## Trail Reporter

Create ten Trail Moments.

## Prepared

Complete readiness checklist for five trips.

Badge rules should be isolated.

Example:

```kotlin
interface BadgeRule {
    fun evaluate(stats: UserStats): Boolean
}
```

---

# 45. Camera

Use CameraX for real camera functionality.

Support two modes.

---

# 46. Camera Mode 1 — Challenge

Flow:

```text
Open Challenge
      ↓
Camera
      ↓
Take photo
      ↓
Preview
      ↓
Submit
      ↓
AI/mock verification
      ↓
Pass / Fail
      ↓
Reward if passed
```

Users can retake before submission.

Once they perform FINAL SUBMIT:

* submission is immutable
* verification begins
* they cannot replace that submission

---

# 47. Photo Verification Abstraction

Create:

```kotlin
interface PhotoVerificationService {

    suspend fun verify(
        image: ByteArray,
        challenge: AdventureChallenge
    ): VerificationResult

}
```

Provide:

```text
MockPhotoVerificationService

GeminiPhotoVerificationService
```

Development environments must work without paid AI credentials.

---

# 48. Development Photo Verification

Mock mode should be deterministic.

Do NOT simply return success 100% of the time.

Example deterministic behaviour based on:

* challenge
* predefined dev images
* fixed development result

This allows testing both pass and fail states.

---

# 49. AI Photo Verification

If configured, call Gemini/Firebase AI through secure architecture.

Do not expose privileged secret API keys directly in the Android app.

Return:

```text
pass/fail
confidence
short explanation
```

The server remains responsible for issuing BioCoins.

---

# 50. Camera Mode 2 — Explore

Users can photograph:

* Plants
* Birds
* Mushrooms
* Animal tracks
* Animals
* Geological formations
* Interesting outdoor objects

Return:

```text
Probable identification

Short explanation

Interesting facts

Confidence

Safety disclaimer
```

Allow user to add result to the current trip as a Trail Moment.

---

# 51. AI Safety

Image recognition can be wrong.

Never tell users a:

* mushroom
* berry
* plant
* animal
* water source
* natural substance

is safe to:

* eat
* drink
* touch
* approach

based solely on AI recognition.

Clearly communicate uncertainty.

---

# 52. User Profile

Profile shows:

## Identity

* Profile photo
* Display name
* Approximate region
* Bio

## Outdoor preferences

* Activities
* Fitness
* Experience
* Pace
* Social style

## Skills

Examples:

```text
Navigation
First Aid
Camping
Trail Running
```

---

# 53. Profile Statistics

Display statistics such as:

```text
12 Trails

84 km

5 Trips

760 BioCoins
```

Additional statistics:

* group trips
* trail moments
* badges
* attendance rate

Only calculate statistics from actual persistent data.

---

# 54. Attendance

When a trip completes, participants can receive attendance credit.

For MVP:

A participant can count as attended if they joined the active/completed trip and were marked present appropriately.

Do not attempt advanced GPS attendance verification.

Attendance rate:

```text
attended group trips
--------------------
joined group trips
```

Handle cancelled trips correctly.

---

# 55. Trip Completion

When user taps:

`Finish Adventure`

perform:

1. Ask for confirmation.
2. Stop foreground location updates.
3. Calculate elapsed duration.
4. Calculate approximate distance.
5. Mark trip activity completed.
6. Store statistics.
7. Evaluate applicable challenges.
8. Award BioCoins where valid.
9. Evaluate badges.
10. Update profile statistics.
11. Show completion screen.

Example:

```text
Adventure Complete 🎉

Wilsons Prom

11.8 km
3h 42m
4 friends
3 trail moments

+120 BioCoins
```

---

# 56. Adventure History

Completed trip appears in:

* Profile
* Trip history

Display:

```text
trail
date
distance
duration
participants
moments
photos
```

Keep architecture ready for richer collaborative Adventure Stories later.

Do not build full social story generation now.

---

# 57. Database / Firestore Model

Design the Firestore model after auditing the current Room models.

Do not blindly duplicate all Room tables.

Target cloud entities should cover:

```text
users
connections
trails
savedTrails
trips
tripMembers
gearItems
readiness
conversations
messages
trailMoments
challenges
dailyChallenges
challengeSubmissions
coinTransactions
badges
userBadges
adventureSessions
```

Use appropriate subcollections where Firestore modelling benefits from them.

Document the final structure.

---

# 58. User Profile Cloud Model

Suggested fields:

```text
uid
displayName
avatarUrl
bio
birthYear optional
gender optional
homeArea optional
fitnessLevel
experienceLevel
preferredPace
socialStyles
interests
skills
createdAt
updatedAt
```

Never publicly expose precise home coordinates.

---

# 59. Trail Cloud Model

Suggested:

```text
id
name
region
activityType
description
difficulty
distanceKm
elevationGainM
estimatedMinutes
latitude
longitude
route
imageUrl
tags
createdAt
```

---

# 60. Trip Cloud Model

Suggested:

```text
id
creatorId
trailId
title
startsAt
meetingLocation
carpoolNotes
foodNotes
emergencyNotes
status
createdAt
updatedAt
```

---

# 61. Adventure Session

Separate a planned trip from an actual tracked activity where appropriate.

Example:

```text
session id
trip id
user id
started at
completed at
distance
duration
status
```

Do not store every raw GPS point in Firestore at extremely high frequency.

Use sensible persistence.

---

# 62. Firebase Storage

Use Storage for:

* profile photos
* Trail Moment photos
* challenge photo submissions

Organise paths securely.

Example:

```text
users/{uid}/profile/

trail-moments/{uid}/...

challenge-submissions/{uid}/...
```

Use Firebase Storage rules.

---

# 63. Firebase Security

Implement proper security rules.

Users can:

* edit only their own private profile fields
* read appropriate public profile data
* manage their own connection requests
* access conversations they belong to
* send messages only to conversations they belong to
* manage trips according to permissions
* update only their own readiness state
* create Trail Moments as themselves
* modify/delete their own moments where permitted
* access only appropriate challenge submissions

Users must NOT be able to:

* modify another user's BioCoin balance
* award themselves badges
* arbitrarily complete challenges
* read private conversations
* change another person's readiness
* expose private location information

---

# 64. Location Privacy

Location is highly sensitive.

Rules:

* Home location is optional.
* Home location shown to others should be approximate.
* Do not expose exact home coordinates.
* Current live GPS location must never be automatically broadcast publicly.
* Trail Moment locations are intentionally shared according to visibility.
* Trip members should not automatically receive continuous live tracking unless explicitly implemented later.

---

# 65. Offline Behaviour

Room should continue providing useful offline/local functionality.

Prioritise caching:

* trails
* active trip
* profile
* recent relevant information

Do not attempt full offline multi-user synchronization for MVP.

If internet disappears during an active hike, OnTrail map/location should remain as useful as practical.

GPS itself must not require Firebase.

---

# 66. Communities

Existing Community UI can remain.

However, persistent community functionality is P1/stretch for this MVP.

Do NOT allow community features to block completion of the core user journey.

If current implementation already works reasonably:

preserve it.

Otherwise:

leave it behind a clearly functioning lightweight implementation or feature boundary.

Core MVP takes priority.

---

# 67. Adventure Stories / Memories

Existing memories/story functionality can remain.

For MVP:

Trip completion history is enough.

If existing Adventure Memories UI is usable, connect it to completed trip data.

Do not build:

* automatic video generation
* complex collaborative editing
* AI story generation

unless core MVP is already complete.

---

# 68. Features Explicitly Postponed

Do NOT spend MVP time implementing:

* Real 3D character
* Character marketplace
* Pet cosmetics
* AR navigation
* Turn-by-turn navigation
* Garmin integration
* Strava integration
* Apple Health
* Google Health Connect
* Background GPS tracking
* Complex communities
* Community network effects
* Video social feed
* Full social media system
* Advanced reputation
* Paid subscriptions
* Marketplace
* Crypto
* AI trip planner
* Dedicated recommendation ML
* Dedicated species recognition ML model
* Large-scale moderation system
* Shared historical exploration maps
* Friend map
* Push notifications unless trivial after core functionality
* iOS

First MVP is Android.

---

# 69. UI/UX Direction

Preserve the visual design language from the existing scaffold where reasonable.

Biomate should feel:

* outdoor
* social
* friendly
* youthful
* playful
* modern
* safe
* adventurous

Avoid making it resemble:

* banking app
* corporate enterprise software
* cryptocurrency platform
* military navigation software
* direct dating-app clone

Use:

* photography
* rounded cards
* readable typography
* maps
* friendly icons
* clear hierarchy
* subtle animations

Do not overanimate important hiking/navigation screens.

---

# 70. Empty States

Every major screen should have good empty states.

Examples:

## Messages

```text
No conversations yet.

Find someone to explore with and start an adventure.
```

## Trips

```text
No upcoming adventures.

Find a trail and create your first trip.
```

## Matches

```text
No strong matches right now.

Try changing your preferences or check again later.
```

---

# 71. Error States

Every asynchronous feature needs:

* loading
* success
* empty
* error
* retry

Do not leave indefinite spinners.

---

# 72. Location Permission Failure

If denied:

Explain why Biomate needs location.

Example:

```text
Biomate uses your location during an adventure to show where you are relative to the trail and to create Trail Moments.

Your location is not automatically shared publicly.
```

Provide:

* Retry
* Open Settings where appropriate

---

# 73. Seed / Development Data

Provide good seed data.

Target approximately:

* 12+ users
* 8+ trails
* multiple fitness levels
* multiple social styles
* connections
* pending connection requests
* upcoming trips
* completed trips
* messages
* Trail Moments
* challenges
* badges

Use Victorian/Australian outdoor context where reasonable.

Examples could include demo data around:

* Wilsons Promontory
* Grampians / Gariwerd
* Cathedral Range
* Dandenong Ranges
* You Yangs
* Great Ocean Walk

Seed content should be clearly treated as development/demo data where necessary.

---

# 74. Development Accounts

Provide at least two development users so multi-user testing is easy.

Example only:

```text
alex@biomate.dev
BiomateDemo123!

sarah@biomate.dev
BiomateDemo123!
```

Only create these in development Firebase environments.

Never seed production authentication with public passwords.

---

# 75. Tests

Maintain existing tests.

Add tests for critical business logic.

At minimum:

## Matching

* high compatibility
* low compatibility
* activity overlap
* pace differences
* social style

## BioCoins

* valid reward
* reward exactly once
* duplicate reward prevented

## Challenges

* completion conditions
* photo submission immutability

## Trips

* creator permissions
* participant behaviour
* cancelled trips

## Trail Moments

* visibility
* ownership

## Badges

* threshold behaviour

---

# 76. Build Validation

Frequently run:

```bash
./gradlew test
```

and:

```bash
./gradlew assembleDebug
```

Also run appropriate lint/static analysis available in the project.

Do not leave the repository knowingly failing compilation.

---

# 77. Claude Code Development Rules

Follow these strictly.

## Rule 1

Do NOT only produce an implementation plan.

Write the code.

---

## Rule 2

Do NOT rewrite working screens merely because you prefer another architecture.

Improve incrementally.

---

## Rule 3

Do NOT create duplicate versions like:

```text
NewHomeScreen
HomeScreen2
FinalHomeScreen
```

Refactor the real implementation.

---

## Rule 4

Work in vertical slices.

For each feature:

```text
Existing implementation
      ↓
Data model
      ↓
Repository
      ↓
Remote/local source
      ↓
ViewModel
      ↓
Existing UI
      ↓
Tests
      ↓
Build
```

---

## Rule 5

Keep the application buildable after every major phase.

---

## Rule 6

If you discover a bug you can reasonably solve yourself:

solve it.

Do not stop and ask the user.

---

## Rule 7

Do not use placeholder TODO implementations and then declare the feature finished.

---

## Rule 8

Avoid unnecessary dependencies.

Use Android/Firebase capabilities already available where suitable.

---

## Rule 9

Do not bypass TypeScript-style safety principles simply because Kotlin allows nullable shortcuts.

Model states correctly.

Avoid widespread `!!`.

---

## Rule 10

Security rules are part of the MVP.

Do not postpone them until "production".

---

# 78. PROGRESS.md

Create and continuously maintain:

`PROGRESS.md`

Structure:

```markdown
# Biomate MVP Progress

## Current Architecture

## Working

## Mocked

## In Progress

## Missing

## Known Issues

## Tests

## Next
```

Update after every significant phase.

---

# 79. README

Update `README.md`.

Include:

* project purpose
* architecture
* Firebase setup
* Google Maps setup
* required environment/configuration
* how to run Android app
* how to seed development environment
* how to run tests
* development accounts
* project structure
* current limitations

---

# 80. Environment Configuration

Do not commit secrets.

Use appropriate:

* `local.properties`
* Firebase configuration
* Gradle secrets handling
* `.gitignore`

Document required configuration.

Potential configuration:

```text
Google Maps API key

Firebase project

AI provider configuration

Weather API configuration if required
```

Public mobile Firebase configuration is not the same as privileged server secrets, but still follow normal Firebase setup practices.

Privileged AI/server credentials must remain server-side.

---

# 81. Phase 0 — Repository Audit

Before feature implementation:

1. Inspect repository.
2. Inspect Gradle files.
3. Inspect app architecture.
4. Inspect every screen.
5. Inspect navigation.
6. Inspect ViewModels.
7. Inspect Room entities.
8. Inspect DAOs.
9. Inspect repositories.
10. Inspect Firebase setup.
11. Inspect tests.
12. Inspect AndroidManifest.
13. Inspect permissions.
14. Search for hard-coded/mock data.
15. Search for plaintext passwords.
16. Search TODO/FIXME.
17. Run existing tests.
18. Build debug APK.

Create/update `PROGRESS.md`.

Do not rewrite features before understanding them.

---

# 82. Phase 1 — Foundation

Complete:

* repository cleanup where necessary
* Firebase integration
* authentication
* Firestore architecture
* repository abstractions
* proper app/session state

Verify:

```text
register
login
restart app
session remains
logout
```

---

# 83. Phase 2 — Profile and Onboarding

Implement:

* onboarding
* cloud profile
* profile editing
* interests
* fitness
* experience
* pace
* social style
* skills

Verify across two accounts.

---

# 84. Phase 3 — Trails and Home

Implement:

* trail repository
* seed trail data
* Discover
* search
* filters
* Trail Detail
* saved trails
* weather
* recommendations
* Home

---

# 85. Phase 4 — HikeMatch

Implement:

* compatibility algorithm
* candidate queries
* HikeMatch UI connection
* profile detail
* connection requests
* accepted connections
* user search

Verify with two users.

---

# 86. Phase 5 — Trips

Implement:

* create trip
* invite
* join
* leave
* participants
* gear
* food notes
* carpool
* readiness

Verify data synchronises across multiple users.

---

# 87. Phase 6 — Messaging

Implement:

* direct conversations
* trip conversations
* Firestore realtime
* unread state
* message ordering
* permissions

Verify:

User A sends message.

User B sees it without restarting.

---

# 88. Phase 7 — OnTrail

Implement:

* location permission
* foreground GPS
* Google Map
* trail polyline
* current user marker
* statistics
* route progress
* moments
* moment filters

Test both:

* real device location
* emulator/mock development location

---

# 89. Phase 8 — Trip Completion

Implement:

* finish adventure
* elapsed duration
* distance
* completion status
* attendance
* profile statistics
* trip history

---

# 90. Phase 9 — Challenges and BioCoins

Implement:

* daily challenge assignment
* progress
* automatic completion where applicable
* Cloud Function reward authority
* BioCoin transaction history
* leaderboard
* badges

Test idempotency aggressively.

---

# 91. Phase 10 — Camera

Implement:

* CameraX
* capture
* preview
* Storage upload
* final submission
* mock verification
* Gemini verification abstraction
* Explore Camera
* Trail Moment integration

---

# 92. Phase 11 — Polish

Improve:

* loading states
* empty states
* errors
* navigation bugs
* keyboard handling
* accessibility
* spacing
* animations
* form validation
* map interactions
* performance

Remove obsolete mock UI/data no longer required.

---

# 93. Phase 12 — End-to-End Verification

Perform the complete MVP test.

Use two development accounts.

## User A

Registers.

Completes profile.

Finds trail.

Finds User B.

Sends connection request.

## User B

Accepts.

## User A

Creates trip.

Invites User B.

Adds shared gear.

## User B

Joins.

Updates their gear.

Completes readiness.

Sends group message.

## User A

Receives group message.

Starts trip.

Map opens.

Current location appears.

Creates Trail Moment.

Completes trip.

Challenge completion triggers.

BioCoins are awarded once.

Profile stats change.

Completed trip appears in history.

## User B

Can see appropriate completed trip information.

If any part of this workflow does not work:

the MVP is not finished.

---

# 94. Definition of Done

Biomate MVP is complete only when:

### Authentication

* Real Firebase authentication works.

### Profiles

* Real persistent profiles work.

### Trails

* Trails are browsable and persistent.

### Matching

* Compatibility works.

### Connections

* Connection requests work between users.

### Trips

* Multi-user trip planning works.

### Gear

* Shared gear works.

### Readiness

* Per-user readiness works.

### Messaging

* Realtime chat works.

### Maps

* Real map displays.

### GPS

* Real device location works.

### Trail Moments

* Location-based moments work.

### Trip Completion

* Adventure tracking can be completed.

### Challenges

* Challenge logic works.

### BioCoins

* BioCoins are server-authoritative and idempotent.

### Camera

* Real camera capture works.

### Profile

* Statistics update from actual activity.

### Persistence

* Closing/restarting app does not destroy cloud data.

### Multi-user

* At least two accounts interact successfully.

### Build

* Debug build succeeds.

### Tests

* Critical automated tests pass.

---

# 95. Final Deliverables

When finished provide:

1. Working Android project.
2. Working Firebase integration.
3. Firestore schema/documentation.
4. Firestore security rules.
5. Storage security rules.
6. Cloud Functions.
7. Room migrations where required.
8. Seed/development data tooling.
9. Development account instructions.
10. Updated README.
11. Updated PROGRESS.md.
12. Automated tests.
13. Successful build.
14. Known limitations.
15. Explicit postponed-feature list.
16. Instructions for running the complete demo.

---

# 96. Product Safety Message

Where relevant, communicate:

> Outdoor conditions can change quickly. Community reports and Biomate recommendations are informational and should not replace official safety information.

AI identification should similarly communicate uncertainty.

---

# 97. Scope Priority

If time or implementation complexity creates tradeoffs, prioritise this exact order:

```text
1. Authentication
2. Profiles
3. Trails
4. Connections
5. Trips
6. Messaging
7. GPS / OnTrail
8. Trail Moments
9. Trip Completion
10. Challenges
11. BioCoins
12. Camera
13. Badges
14. Polish
15. Communities
16. Adventure Memories
17. Everything else
```

Never sacrifice the complete core journey to implement a stretch feature.

---

# 98. Final Instruction

Start now.

Do NOT reply with only a plan.

First:

```text
1. Inspect the repository
2. Run the build
3. Run tests
4. Audit existing functionality
5. Create/update PROGRESS.md
```

Then begin implementing the first incomplete vertical slice.

Continue sequentially through the MVP.

When an existing feature already works:

verify it and reuse it.

When it is mocked:

replace the mock.

When it is incomplete:

finish it.

When it is broken:

repair it.

When something is unnecessary for the MVP:

do not spend time on it.

The goal is not to create another Biomate prototype.

The goal is to turn the existing `FiveGuys` repository into a genuinely functional **Biomate Android MVP** that demonstrates the complete:

**Discover → Match → Plan → Prepare → Adventure → Record → Earn → Repeat**

journey end-to-end.
