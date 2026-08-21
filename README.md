# Biomate

An Android app for finding people to go outdoors with, planning the trip together, and
actually walking it.

The loop the app is built around:

```
Discover → Match → Connect → Plan → Prepare → Adventure → Record → Complete → Earn → Repeat
```

---

## Quick start

```bash
git clone https://github.com/chimdienn/FiveGuys.git
cd FiveGuys
./gradlew assembleDebug
./gradlew installDebug     # with a device or emulator attached
```

**No configuration is required to run the app.** With no Firebase project and no API keys
it starts on a local backend, seeds thirteen demo people, eight Victorian trails, trips,
gear, messages and community reports, and offers two sign-in-able demo accounts.

### Development accounts

| Account | Email | Password |
|---|---|---|
| Alex Rivera | `alex@biomate.dev` | `BiomateDemo123!` |
| Sarah Chen | `sarah@biomate.dev` | `BiomateDemo123!` |

Both are listed on the sign-in screen — tapping one fills the form. Sign in as each in
turn to exercise the two-user flow (connection requests, trip invites, group chat).

These accounts exist **only** on the local backend. The seeder never writes credentials
when Firebase is configured, so they cannot leak into a real project.

---

## What runs where

Biomate has two interchangeable backends behind one set of repository interfaces.

| | Local backend (default) | Firebase backend |
|---|---|---|
| Selected when | no `google-services.json` | `google-services.json` present |
| Auth | Room + PBKDF2-HMAC-SHA256, 120k iterations | Firebase Authentication |
| Data | Room | Cloud Firestore |
| Photos | app-private storage | Cloud Storage |
| BioCoin authority | `UNIQUE` index inside a Room transaction | Cloud Functions |
| Multi-user | two accounts on one device | across devices |

`core/AppContainer.kt` makes the choice at startup and logs which backend it picked. No
code above that file knows the difference.

The local backend is a real implementation, not a stub: passwords are salted and hashed,
coin awards are genuinely idempotent, and permissions are genuinely enforced. What it
cannot do is sync between two installs.

---

## Optional configuration

### Google Maps

Without a key everything works except the map tiles, which render blank. GPS tracking,
distance, route progress and Trail Moments are unaffected.

1. Enable **Maps SDK for Android** in a Google Cloud project.
2. Create an API key and restrict it to the package `com.aistudio.biomate.advntr` plus
   your signing certificate's SHA-1.
3. Put it in `.env` (git-ignored) or `local.properties`:

   ```properties
   MAPS_API_KEY=AIza...
   ```

CI can supply it as a `MAPS_API_KEY` environment variable instead. See
`resolveMapsApiKey()` in `app/build.gradle.kts` for the resolution order.

### Firebase

1. Create a Firebase project and add an Android app with the package
   `com.aistudio.biomate.advntr`.
2. Download `google-services.json` into `app/`. **Do not commit it.**
3. Enable Authentication (Email/Password), Cloud Firestore and Cloud Storage.
4. Deploy the rules and functions:

   ```bash
   cd firebase
   firebase deploy --only firestore:rules,storage:rules,functions
   ```

5. Rebuild. `AppContainer` will log `Firebase configured — using cloud backend.`

### AI features

Photo verification and species identification run through **Firebase AI Logic**, which
proxies requests server-side. There is no API key in the app — the prototype's practice of
compiling `GEMINI_API_KEY` into the APK was removed, because a key in an APK is extractable
by anyone who downloads it.

Without Firebase, both fall back to deterministic offline implementations. The photo
verifier deliberately fails roughly one submission in four rather than passing everything,
so the failure path is exercised during development.

---

## Weather

Live conditions come from [Open-Meteo](https://open-meteo.com), a free key-less public API.
All provider-specific detail — URL shape, WMO weather codes, JSON field names — is confined
to `data/weather/OpenMeteoWeatherService.kt`; everything upstream sees only the `Weather`
domain model.

---

## Architecture

```
Jetpack Compose UI
        ↓
    ViewModel                    (ui/viewmodel — one per feature area)
        ↓
Repository interface             (domain/repository — the only thing the UI knows)
        ↓
  ┌─────┴─────┐
Room        Firestore            (data/repository/local, data/remote)
```

```
app/src/main/java/com/example/
├── core/AppContainer.kt         Manual DI; chooses the backend
├── domain/
│   ├── model/                   Pure Kotlin domain types
│   ├── match/Compatibility.kt   Deterministic weighted scoring
│   ├── weather/TrailRanking.kt  Rule-based recommendations
│   ├── challenge/               Challenge catalogue and progress rules
│   ├── badge/BadgeRules.kt      One isolated rule per badge
│   ├── session/                 Geodesy and distance tracking
│   ├── ai/                      Verification and identification interfaces
│   └── repository/              Every repository contract
├── data/
│   ├── local/                   Room entities, DAO, database
│   ├── auth/                    PBKDF2 hashing, session store, local auth
│   ├── repository/local/        Room-backed repository implementations
│   ├── weather/                 Open-Meteo client
│   ├── location/                Fused location provider
│   ├── ai/                      Mock and Gemini implementations
│   ├── mapper/                  Entity ↔ domain conversion
│   └── seed/                    Trails, demo people, dev seeding
└── ui/
    ├── BiomateApp.kt            Navigation graph
    ├── screens/                 One file per screen
    ├── components/              Shared visual language and state components
    ├── viewmodel/               Feature ViewModels
    └── theme/                   Colour, type, shape

firebase/
├── firestore.rules              Authorization model
├── storage.rules                Per-user upload paths
└── functions/index.js           BioCoin authority, badges, stats
```

### Why the domain layer is pure

`Compatibility`, `TrailRanking`, `BadgeRules`, `ChallengeEngine` and `Geo` have no Android
dependencies, no clock and no IO. That is what makes them testable, and what makes two
devices agree on a compatibility score.

---

## Running tests

```bash
./gradlew testDebugUnitTest
```

148 tests covering:

| Area | What is asserted |
|---|---|
| Compatibility | symmetry, weight totals, each facet's effect, explanation quality |
| BioCoins | award once, concurrent awards, replay, per-user scoping, rejections |
| Challenges | deterministic assignment, progress rules, submission immutability |
| Badges | every threshold, boundary conditions, no double-award |
| Trips | organiser vs participant permissions, limits, cancellation, attendance |
| Gear | who may tick off what |
| Readiness | per-user isolation, completion, clamping |
| Trail Moments | ownership, location validity, upvote rules, staleness |
| Geo | haversine accuracy, route projection, GPS jitter rejection |
| Weather ranking | each rule's direction, score bounds |

The BioCoin and submission-immutability tests run against a real in-memory Room database
rather than a mocked DAO, because the guarantees under test live in SQLite constraints.

---

## Security

- **No plaintext passwords anywhere.** The prototype stored a `password` column in Room
  with the default `"trail2026"`; that table is gone. Credentials live in Firebase Auth,
  or as salted PBKDF2 hashes locally.
- **BioCoins are server-authoritative.** No client path writes a balance. Every award
  carries an idempotency key enforced by a unique constraint.
- **Badges and challenge verdicts are server-written.** A client cannot award itself.
- **Photo submissions are immutable** once submitted.
- **Readiness is per-user writable only.**
- **Location privacy**: home location is optional, approximate, and free text — Biomate
  never stores a precise home coordinate. Live GPS is foreground-only and never broadcast
  automatically.

See `firebase/firestore.rules` for the full authorization model.

---

## Current limitations

- **The Firebase repository implementations are not written.** The rules, Cloud Functions,
  schema and the `AppContainer` switch all exist, but every repository currently resolves
  to its local implementation. Cross-device sync therefore does not work yet. This is the
  single largest gap — see `PROGRESS.md`.
- Map tiles need a `MAPS_API_KEY`; without one the map area is blank.
- Trail routes are **simplified demonstration polylines**, not survey-grade navigation
  data. The trail detail screen says so.
- Foreground GPS only. Closing the app stops tracking.
- Communities are not implemented (explicitly P1 in the brief).
- Adventure memories show recorded trip data; there is no generated narrative.
- Distance is trusted from the device. There is no GPS anti-cheat, by design.

---

## Explicitly out of scope

3D character, character marketplace, AR navigation, turn-by-turn navigation, Garmin /
Strava / Apple Health / Health Connect integrations, background GPS, complex communities,
video feed, advanced reputation, subscriptions, marketplace, crypto, AI trip planning,
recommendation ML, dedicated species-recognition models, large-scale moderation, friend
maps, push notifications, and iOS.

---

## Safety

> Outdoor conditions can change quickly. Community reports and Biomate recommendations are
> informational and should not replace official safety information.

AI identification can be wrong. Biomate never states that a plant, fungus, animal, berry or
water source is safe to eat, drink, touch or approach, and every identification carries a
non-dismissible uncertainty note.
