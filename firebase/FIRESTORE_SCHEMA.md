# Firestore schema

The cloud model, and why it is shaped this way. It deliberately does **not** mirror the
Room tables one-for-one — Room is a local cache with foreign keys and joins available,
whereas Firestore charges per document read and cannot join.

Two rules shaped every decision below:

1. **Anything a user could profit from lying about is server-owned.** Balances, badges,
   challenge progress and verdicts live in collections no client can write.
2. **Read shape follows the screen that reads it.** Where a screen needs a list, the data
   is a collection; where it needs one document, it is one document.

---

## `users/{uid}`

The public profile. Readable by any signed-in user, because HikeMatch has to score
candidates client-side.

```
uid                 string
displayName         string
displayNameLower    string        for prefix search
avatarUrl           string?
bio                 string
birthYear           number?
gender              string?
homeArea            string?       approximate locality, free text — never coordinates
fitnessLevel        string        FitnessLevel enum name
experienceLevel     string        ExperienceLevel enum name
preferredPace       string        PreferredPace enum name
socialStyles        array<string>
interests           array<string>
skills              array<string>
avatarColorHex      number
onboardingComplete  boolean
createdAt           timestamp
updatedAt           timestamp
```

There is no password field and no precise coordinate. There is also no `bioCoins`,
`badges` or `stats` key — the rules reject writes containing them, so a compromised client
cannot inflate its own reputation by patching its profile.

### `users/{uid}/stats/current`

Derived statistics, written only by `recomputeStatsAndBadges`.

```
trailsCompleted, totalDistanceKm, totalDurationMinutes, tripsCompleted,
groupTripsCompleted, groupTripsJoined, trailMomentsCreated,
readinessChecklistsCompleted, updatedAt
```

### `users/{uid}/badges/{badgeId}`

```
badgeId   string
earnedAt  timestamp
```

Document id is the badge id, so a badge cannot be earned twice.

### `users/{uid}/savedTrails/{trailId}`

Private to its owner.

---

## `trails/{trailId}`

Reference data. Everyone reads; only an operator writes.

```
id, name, region, stateOrCountry, activityTypes array<string>, description,
difficulty, distanceKm, elevationGainM, estimatedMinutes,
start geopoint, route array<geopoint>, waypoints array<map>,
imageUrl, tags array<string>, isExposed, isShaded, rating, reviewCount,
highlights array<string>, recommendedGear array<string>, createdAt
```

`route` is stored inline rather than as a subcollection: a polyline is always read whole,
and one document read beats fifty.

---

## `connections/{connectionId}`

`connectionId` is the two uids sorted and joined — `uidA__uidB`. Deterministic on purpose:
two people requesting each other simultaneously converge on one document instead of
creating a pair of mirror-image requests that each wait for the other.

```
requesterId  string
addresseeId  string
status       string     PENDING | ACCEPTED | REJECTED
createdAt    timestamp
respondedAt  timestamp?
```

Only `addresseeId` may move the status to ACCEPTED or REJECTED.

---

## `trips/{tripId}`

```
creatorId, trailId, trailName, title, startsAt timestamp,
meetingPoint, participantLimit number?, carpoolNotes, foodNotes,
generalNotes, emergencyNotes, status, createdAt, updatedAt, completedAt?
```

### `trips/{tripId}/members/{uid}`

A subcollection rather than an array, so a roster of any size can be paged and a single
member can be updated without a read-modify-write of the whole trip.

```
uid, displayName, role, status, joinedAt?, attended
```

### `trips/{tripId}/gear/{itemId}`

```
name, category, quantity, assignedToUid?, assignedToName?, isPacked, isEssential
```

### `trips/{tripId}/readiness/{uid}`

Document id is the uid, which is what lets the rules express "only you may write your own"
as a one-line condition.

```
checkedItems array<string>, confidence number?, notes, updatedAt
```

---

## `conversations/{conversationId}`

Ids are deterministic: `direct__uidA__uidB` or `trip__{tripId}`.

```
type            DIRECT | TRIP
memberIds       array<string>     the authorization list
title           string
tripId          string?
lastMessagePreview, lastMessageAt, lastMessageSenderId
lastReadAt      map<uid, timestamp>
```

`memberIds` is both the membership list and the security boundary — the rules read it
directly. Direct conversations carry no meaningful stored title; the client renders the
other person's name per viewer.

### `conversations/{conversationId}/messages/{messageId}`

```
senderId, senderName, text, sentAt, isSystem
```

Immutable once written. A group planning a trip into terrain that can kill them should not
have an editable history.

---

## `trailMoments/{momentId}`

Top-level rather than nested under a trail, because moments are queried three ways — by
trail, by trip and by author — and a subcollection would only serve the first.

```
creatorId, creatorName, trailId, tripId?, latitude, longitude,
category, description, photoUrl?, visibility, createdAt, upvotes
```

---

## `challenges/{challengeId}`

The reward catalogue. Read-only to clients; the authoritative copy also lives in
`functions/index.js` so a client cannot influence a payout by editing what it sends.

---

## `dailyChallenges/{dailyId}`

`dailyId` is `{uid}|{dateKey}|{challengeId}` — deterministic, so assignment is idempotent
and cannot silently reset progress.

```
uid, challengeId, dateKey, progress, target, completedAt?, rewardedAt?
```

Server-written. A client that could set `completedAt` could mint coins.

---

## `challengeSubmissions/{submissionId}`

```
uid, dailyChallengeId, challengeId, photoUrl?, state, confidence?,
explanation?, submittedAt, verifiedAt?
```

Clients may create one in `PENDING`; the verdict is server-written and the document is
never updatable by a client, so a `FAILED` cannot be flipped to `PASSED`.

---

## `coinTransactions/{transactionId}`

The append-only ledger. Balance is the sum of these rows; it is never a directly writable
number, which removes the entire class of bug where a balance and its history disagree.

```
uid, amount, reason, challengeId?, referenceId?, idempotencyKey, createdAt
```

## `coinIdempotencyKeys/{idempotencyKey}`

The mechanism that makes double payment impossible. `awardOnce` creates this document and
the ledger entry in one transaction; a `create` on an existing document fails, so a
replayed award is rejected by Firestore rather than by application logic.

## `leaderboard/{uid}`

```
uid, bioCoins, updatedAt
```

A denormalised cache of the ledger sum, so the Home leaderboard is one query rather than a
full ledger scan per user. `recomputeBalance` rebuilds it from the ledger if it ever
drifts — the ledger always wins.

---

## `adventureSessions/{sessionId}`

The actual outing, as distinct from the plan. A `trip` is what people agreed to do; a
session is what one of them did.

```
uid, tripId?, trailId, startedAt, completedAt?, distanceKm,
durationMinutes, momentCount, companionCount, status
```

Only the running total is stored — individual GPS fixes stay in memory. Persisting every
point would be a needless privacy liability and would flood the database.

---

## Required indexes

```
trailMoments      trailId ASC,   createdAt DESC
trailMoments      tripId ASC,    createdAt DESC
trailMoments      creatorId ASC, createdAt DESC
adventureSessions uid ASC, status ASC, completedAt DESC
dailyChallenges   uid ASC, dateKey ASC
coinTransactions  uid ASC, createdAt DESC
conversations     memberIds ARRAY, lastMessageAt DESC
leaderboard       bioCoins DESC
```

Collection-group indexes are needed on `members` (`uid`) and `readiness` (`uid`) for the
statistics computation in `recomputeStatsAndBadges`.
