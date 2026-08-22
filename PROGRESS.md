# Biomate MVP Progress

Phases 0–11 complete. The app builds, runs, and the core journey works end-to-end on one
device. The remaining gap is the Firebase repository layer — see **Missing**.

## Current Architecture

```
Jetpack Compose UI
        ↓
    ViewModel                    ui/viewmodel — 9 feature ViewModels
        ↓
Repository interface             domain/repository — the only contract the UI sees
        ↓
  ┌─────┴─────┐
Room        Firestore            local implementations done; Firestore not yet written
```

`core/AppContainer.kt` picks the backend at startup based on whether `google-services.json`
is present, and logs which one it chose.

## Working

Verified on a Pixel emulator, signed in as the `alex@biomate.dev` demo account.

- **Authentication** — register, sign in, sign out, persistent session across restarts.
  PBKDF2-HMAC-SHA256 with a per-user salt; constant-time verification.
- **Onboarding** — five steps covering identity, interests, fitness, experience, pace,
  social style and skills. Everything but a display name and one interest is optional.
- **Home** — live Open-Meteo weather (verified returning real Melbourne conditions),
  character placeholder with level and BioCoin balance, daily challenges, next trip,
  weather-ranked trail recommendation, trail carousel, derived conditions notes,
  leaderboard.
- **Discover** — search plus activity, difficulty, distance, duration, region and saved
  filters; saved trails per user.
- **Trail detail** — stats, conditions at the trailhead, elevation profile, recent
  community reports with age and staleness treatment, three call-to-actions.
- **HikeMatch** — deterministic weighted compatibility with per-facet breakdown and
  human-readable reasons. Verified producing an 80% / 79% spread on seed data.
- **Connections** — request, accept, decline, remove; a simultaneous cross-request
  converges on one accepted relationship.
- **User search** by display name.
- **Trips** — create, invite, join, leave, cancel; participant limits; organiser-only
  management. Verified against seeded trip and gear.
- **Shared gear** — claim, assign, tick off, remove, with permission rules enforced.
- **Readiness** — nine-item checklist, per-user writable only, confidence and notes.
- **Messaging** — trip and direct conversations, unread state, per-viewer titles,
  membership-gated sending, optimistic send with restore-on-failure.
- **OnTrail** — real fused-location tracking. Verified: 2.29 km travelled and 55% along
  route computed from injected GPS fixes, via haversine accumulation with jitter
  rejection and segment projection onto the trail polyline.
- **Trail Moments** — seven categories, visibility levels, map filters, ownership rules,
  age labelling, current-GPS placement, and deliberate map-pin notes created by tapping
  or long-pressing the live map.
- **Trip completion** — session close, statistics write-back, attendance credit, challenge
  evaluation, badge evaluation, summary screen.
- **Challenges and BioCoins** — deterministic daily assignment, automatic progress from
  real activity, idempotent awards, transaction history.
- **Badges** — six rules, evaluated from derived statistics, awarded once.
- **Camera** — CameraX capture, retake, final-submit confirmation, immutable submissions,
  and Explore-mode identification with a non-dismissible safety notice.
- **Profile** — statistics derived entirely from persisted activity, badges (earned and
  locked), adventure history, BioCoin ledger.
- **Security rules and Cloud Functions** — written and deployable.

## Mocked

- **Photo verification and species identification** fall back to deterministic offline
  implementations without Firebase. The photo verifier fails about one submission in four
  by design so the failure path is exercised.
- **Trail routes** are simplified demonstration polylines, not survey data. Stated in the
  UI and the README.
- **Trail catalogue** is eight hand-written Victorian tracks from public information.

## In Progress

Nothing in flight.

## Missing

1. **Firestore repository implementations.** The interfaces, security rules, Cloud
   Functions, schema and the `AppContainer` switch all exist, but `isFirebaseConfigured`
   currently selects local implementations either way. Until these are written,
   multi-user interaction works between two accounts on **one device** but not across
   devices. This is the biggest remaining gap and the reason the "at least two accounts
   interact" criterion is met only in the single-device sense.
2. **Google Sign-In.** Email/password only.
3. **Password reset on the local backend.** Returns an honest failure explaining that it
   needs Firebase, rather than pretending an email was sent.
4. **Communities.** Not implemented; explicitly P1 in the brief.
5. **Profile photo upload.** `PhotoStore` supports it and the Storage rules cover the
   path, but no UI calls it — avatars are initials on a per-user colour.
6. **Trail Moment photos.** The repository and store accept them; the OnTrail add-moment
   dialog does not yet offer a capture step.
7. **Connections/TRIP moment visibility filtering.** The field is stored and enforced on
   write, but reads do not yet filter by relationship.

## Known Issues

- Map tiles are blank without a `MAPS_API_KEY`. Everything else on OnTrail works.
- Distance is trusted from the device; there is no GPS anti-cheat, by design.
- The leaderboard has no time periods — it is all-time only.
- `ExampleRobolectricTest` is a leftover smoke test kept only to prove resources resolve.

## Fixed during this work

- **Missing Gradle wrapper.** `gradlew` and `gradle-wrapper.jar` were absent from the
  repository; regenerated at 9.3.1.
- **Test source set never compiled.** `GreetingScreenshotTest` referenced a `Greeting`
  composable that does not exist, so `./gradlew testDebugUnitTest` had never run.
  Removed, along with a stale assertion expecting the app to be called "My Application".
- **Plaintext passwords** in the Room `user_accounts` table (default `"trail2026"`).
  Table and model deleted.
- **Gemini API key compiled into the APK** and called directly from the client. Replaced
  with Firebase AI Logic.
- **Fake GPS.** `startSimulation()` incremented elapsed time on a two-second timer and
  generated elevation with `(280..420).random()`. Replaced with the fused location
  provider.
- **Fake map.** A hand-drawn Compose canvas replaced with Google Maps Compose.
- **Fabricated profile statistics** (`totalHikes = 42` as a seeded constant). All
  statistics now derive from persisted activity.
- **Client-side challenge progress** (`incrementChallengeProgress(challenge, 5)` called
  straight from the UI). Progress now derives from real activity.
- **Leaked coroutine** — `BiomateRepository.updateUserProfileDetails()` called `.collect{}`
  on a Room `Flow`, which never returns, on every profile save.
- **Dark-on-dark text.** No Compose surface painted a background, so screens outside the
  Scaffold showed the Android window background, which disagreed with the colour scheme.
  The theme now paints its own surface.
- **Doubled status-bar inset** from a Scaffold nested inside the app Scaffold.
- **Content drawn under the status bar** on auth and onboarding.
- **Misleading sign-in failure.** A demo login attempted while first-run seeding was still
  running reported "Incorrect email or password". Authentication now awaits a seeding gate.
- **Seeded trips departing at wall-clock time** (a hike leaving at 02:35).
- **Green progress track with a stray stop-indicator dot** from Material 3 defaults.
- **Body text below a comfortable reading size** (15sp → 16sp).

## Tests

148 unit tests, all passing.

| Suite | Tests |
|---|---|
| TripPermissionsTest | 24 |
| ChallengeEngineTest | 19 |
| CompatibilityTest | 18 |
| GeoTest | 18 |
| ChallengeRewardTest | 17 |
| TrailMomentTest | 14 |
| BadgeRulesTest | 12 |
| TrailRankingTest | 11 |
| RewardIdempotencyTest | 10 |
| UserStatsTest | 4 |
| ExampleRobolectricTest | 1 |

The BioCoin and submission tests run against a real in-memory Room database, because the
guarantees under test are SQLite constraints rather than application logic.

## Next

1. Write the Firestore repository implementations against the existing interfaces, and
   flip `AppContainer` to select them when Firebase is configured.
2. Verify the full two-account journey across two physical devices.
3. Add photo capture to the Trail Moment flow and profile avatar upload.
4. Filter moment reads by visibility and connection state.
5. Add instrumented UI tests for the critical navigation paths.
