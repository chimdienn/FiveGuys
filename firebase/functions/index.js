/**
 * Biomate Cloud Functions — the BioCoin authority.
 *
 * The Firestore rules deny every client write to `coinTransactions`, `dailyChallenges`,
 * `challengeSubmissions` verdicts and `users/{uid}/badges`. This file is the only thing
 * that writes them. That split is the whole point: a client can *ask* to be paid, but the
 * decision and the write happen somewhere it cannot reach (spec sections 41, 42 and 63).
 *
 * The Android client mirrors this logic locally in `LocalRewardRepository` for builds with
 * no Firebase project. There, the same guarantee is enforced by a UNIQUE index inside a
 * Room transaction rather than by trust boundaries — see that class for why.
 */

const { onCall, HttpsError } = require("firebase-functions/v2/https");
const { onDocumentCreated } = require("firebase-functions/v2/firestore");
const { setGlobalOptions } = require("firebase-functions/v2");
const admin = require("firebase-admin");

admin.initializeApp();
const db = admin.firestore();

setGlobalOptions({ region: "australia-southeast1", maxInstances: 10 });

/** The reward catalogue. Kept server-side so a client cannot inflate its own payout. */
const CHALLENGES = {
  ch_walk_5km: { type: "DISTANCE", target: 5, reward: 50 },
  ch_walk_3km: { type: "DISTANCE", target: 3, reward: 30 },
  ch_complete_trail: { type: "TRIP_COMPLETE", target: 1, reward: 100 },
  ch_group_trip: { type: "GROUP_TRIP", target: 3, reward: 120 },
  ch_trail_moment: { type: "TRAIL_MOMENT", target: 1, reward: 20 },
  ch_three_moments: { type: "TRAIL_MOMENT", target: 3, reward: 60 },
  ch_photo_mountain: { type: "PHOTO", target: 1, reward: 50 },
  ch_photo_water: { type: "PHOTO", target: 1, reward: 40 },
  ch_photo_tree: { type: "PHOTO", target: 1, reward: 40 },
};

const BADGE_RULES = [
  { id: "FIRST_STEPS", test: (s) => (s.trailsCompleted || 0) >= 1 },
  { id: "EXPLORER", test: (s) => (s.trailsCompleted || 0) >= 5 },
  { id: "TRAIL_REGULAR", test: (s) => (s.totalDistanceKm || 0) >= 50 },
  { id: "SOCIAL_HIKER", test: (s) => (s.groupTripsCompleted || 0) >= 3 },
  { id: "TRAIL_REPORTER", test: (s) => (s.trailMomentsCreated || 0) >= 10 },
  { id: "PREPARED", test: (s) => (s.readinessChecklistsCompleted || 0) >= 5 },
];

/**
 * Awards coins exactly once for a given idempotency key.
 *
 * The transaction reads the key document and the ledger write together, so two concurrent
 * callers cannot both see "not yet awarded". The key document — not application logic —
 * is what makes this safe: `create` on an existing document fails inside the transaction.
 *
 * @returns {Promise<{granted: boolean, amount: number, balance: number}>}
 */
async function awardOnce({ uid, amount, reason, idempotencyKey, challengeId, referenceId }) {
  if (!Number.isInteger(amount) || amount <= 0) {
    throw new HttpsError("invalid-argument", "Award amount must be a positive integer.");
  }
  if (!idempotencyKey) {
    throw new HttpsError("invalid-argument", "An idempotency key is required.");
  }

  const keyRef = db.collection("coinIdempotencyKeys").doc(idempotencyKey);
  const txRef = db.collection("coinTransactions").doc();
  const leaderRef = db.collection("leaderboard").doc(uid);

  return db.runTransaction(async (tx) => {
    const existing = await tx.get(keyRef);
    if (existing.exists) {
      // Already paid. Return the original outcome rather than erroring — a retried
      // request is normal, and the caller needs a usable answer.
      const leader = await tx.get(leaderRef);
      return {
        granted: false,
        amount: existing.data().amount,
        balance: (leader.exists && leader.data().bioCoins) || 0,
      };
    }

    const leader = await tx.get(leaderRef);
    const previousBalance = (leader.exists && leader.data().bioCoins) || 0;

    tx.create(keyRef, {
      uid,
      amount,
      transactionId: txRef.id,
      createdAt: admin.firestore.FieldValue.serverTimestamp(),
    });

    tx.create(txRef, {
      uid,
      amount,
      reason: reason || "Reward",
      challengeId: challengeId || null,
      referenceId: referenceId || null,
      idempotencyKey,
      createdAt: admin.firestore.FieldValue.serverTimestamp(),
    });

    // A denormalised balance for the leaderboard. The ledger stays the source of truth;
    // this is a cache, rebuilt by `recomputeBalance` if it ever drifts.
    tx.set(
      leaderRef,
      {
        uid,
        bioCoins: previousBalance + amount,
        updatedAt: admin.firestore.FieldValue.serverTimestamp(),
      },
      { merge: true }
    );

    return { granted: true, amount, balance: previousBalance + amount };
  });
}

/**
 * Claims the reward for a completed daily challenge.
 *
 * The client supplies only the daily-challenge id. The server decides whether it is
 * complete and how much it is worth — a client that lies about either changes nothing.
 */
exports.claimChallengeReward = onCall(async (request) => {
  const uid = request.auth && request.auth.uid;
  if (!uid) throw new HttpsError("unauthenticated", "Sign in first.");

  const dailyChallengeId = request.data && request.data.dailyChallengeId;
  if (!dailyChallengeId) {
    throw new HttpsError("invalid-argument", "dailyChallengeId is required.");
  }

  const dailyRef = db.collection("dailyChallenges").doc(dailyChallengeId);
  const daily = await dailyRef.get();
  if (!daily.exists) throw new HttpsError("not-found", "That challenge is not assigned.");

  const data = daily.data();
  if (data.uid !== uid) {
    throw new HttpsError("permission-denied", "That challenge belongs to someone else.");
  }

  const challenge = CHALLENGES[data.challengeId];
  if (!challenge) throw new HttpsError("failed-precondition", "Unknown challenge.");

  // Completion is judged from the stored progress, which only the server writes.
  if (!data.completedAt || (data.progress || 0) < challenge.target) {
    throw new HttpsError("failed-precondition", "That challenge is not complete yet.");
  }

  const result = await awardOnce({
    uid,
    amount: challenge.reward,
    reason: `Challenge complete: ${data.challengeId}`,
    // Derived from the daily challenge id, so the same challenge can never pay twice
    // however many times this is called.
    idempotencyKey: `challenge:${dailyChallengeId}`,
    challengeId: data.challengeId,
    referenceId: dailyChallengeId,
  });

  if (result.granted) {
    await dailyRef.update({ rewardedAt: admin.firestore.FieldValue.serverTimestamp() });
  }

  return result;
});

/**
 * Records verified activity against a user's daily challenges.
 *
 * Progress is derived from documents the server can see — completed sessions, moments
 * actually written — rather than from a number the client passes in.
 */
exports.syncChallengeProgress = onCall(async (request) => {
  const uid = request.auth && request.auth.uid;
  if (!uid) throw new HttpsError("unauthenticated", "Sign in first.");

  const dateKey = request.data && request.data.dateKey;
  if (!dateKey) throw new HttpsError("invalid-argument", "dateKey is required.");

  const [sessions, moments, dailies] = await Promise.all([
    db.collection("adventureSessions")
      .where("uid", "==", uid)
      .where("status", "==", "COMPLETED")
      .get(),
    db.collection("trailMoments").where("creatorId", "==", uid).get(),
    db.collection("dailyChallenges")
      .where("uid", "==", uid)
      .where("dateKey", "==", dateKey)
      .get(),
  ]);

  const startOfDay = Date.parse(`${dateKey}T00:00:00Z`);
  const inToday = (doc) => {
    const at = doc.get("completedAt") || doc.get("createdAt");
    const millis = at && at.toMillis ? at.toMillis() : 0;
    return millis >= startOfDay && millis < startOfDay + 86400000;
  };

  const todaySessions = sessions.docs.filter(inToday);
  const todayMoments = moments.docs.filter(inToday);

  const signal = {
    distanceKm: todaySessions.reduce((sum, d) => sum + (d.get("distanceKm") || 0), 0),
    tripsCompleted: todaySessions.length,
    groupSize: todaySessions.reduce(
      (max, d) => Math.max(max, (d.get("companionCount") || 0) + 1),
      0
    ),
    momentsCreated: todayMoments.length,
  };

  const updates = [];
  for (const doc of dailies.docs) {
    const data = doc.data();
    const challenge = CHALLENGES[data.challengeId];
    if (!challenge) continue;

    let progress;
    switch (challenge.type) {
      case "DISTANCE": progress = Math.floor(signal.distanceKm); break;
      case "TRIP_COMPLETE": progress = signal.tripsCompleted; break;
      case "GROUP_TRIP": progress = signal.groupSize; break;
      case "TRAIL_MOMENT": progress = signal.momentsCreated; break;
      // Photo challenges advance only when a submission passes verification.
      default: continue;
    }

    progress = Math.min(progress, challenge.target);
    if (progress <= (data.progress || 0)) continue;

    const patch = { progress };
    // Stamped once. A later, larger signal cannot "re-complete" a challenge and so
    // cannot trigger a second payout.
    if (progress >= challenge.target && !data.completedAt) {
      patch.completedAt = admin.firestore.FieldValue.serverTimestamp();
    }
    updates.push(doc.ref.update(patch));
  }

  await Promise.all(updates);
  return { updated: updates.length };
});

/**
 * Recomputes a user's derived statistics and awards any newly earned badges.
 *
 * Badges are written only here, which is why the rules forbid clients from touching
 * `users/{uid}/badges` — a self-awarded badge would make every badge meaningless.
 */
exports.recomputeStatsAndBadges = onCall(async (request) => {
  const uid = request.auth && request.auth.uid;
  if (!uid) throw new HttpsError("unauthenticated", "Sign in first.");

  const stats = await computeStats(uid);
  await db.collection("users").doc(uid).collection("stats").doc("current").set(stats, { merge: true });

  const badgesRef = db.collection("users").doc(uid).collection("badges");
  const held = new Set((await badgesRef.get()).docs.map((d) => d.id));

  const newlyEarned = BADGE_RULES.filter((rule) => !held.has(rule.id) && rule.test(stats));
  await Promise.all(
    newlyEarned.map((rule) =>
      badgesRef.doc(rule.id).set({
        badgeId: rule.id,
        earnedAt: admin.firestore.FieldValue.serverTimestamp(),
      })
    )
  );

  return { stats, newBadges: newlyEarned.map((r) => r.id) };
});

async function computeStats(uid) {
  const [sessions, moments, readiness, memberships] = await Promise.all([
    db.collection("adventureSessions")
      .where("uid", "==", uid)
      .where("status", "==", "COMPLETED")
      .get(),
    db.collection("trailMoments").where("creatorId", "==", uid).get(),
    db.collectionGroup("readiness").where("uid", "==", uid).get(),
    db.collectionGroup("members").where("uid", "==", uid).get(),
  ]);

  const completedGroupTrips = sessions.docs.filter((d) => (d.get("companionCount") || 0) >= 1);
  const joinedGroupTrips = memberships.docs.filter((d) => d.get("status") === "JOINED");

  return {
    uid,
    trailsCompleted: sessions.size,
    totalDistanceKm: sessions.docs.reduce((sum, d) => sum + (d.get("distanceKm") || 0), 0),
    totalDurationMinutes: sessions.docs.reduce((sum, d) => sum + (d.get("durationMinutes") || 0), 0),
    tripsCompleted: new Set(sessions.docs.map((d) => d.get("tripId")).filter(Boolean)).size,
    groupTripsCompleted: completedGroupTrips.length,
    groupTripsJoined: joinedGroupTrips.length,
    trailMomentsCreated: moments.size,
    // A checklist counts as complete only when every one of the nine items is ticked.
    readinessChecklistsCompleted: readiness.docs.filter(
      (d) => (d.get("checkedItems") || "").split(";").filter(Boolean).length >= 9
    ).length,
    updatedAt: admin.firestore.FieldValue.serverTimestamp(),
  };
}

/**
 * Rebuilds a leaderboard balance from the ledger.
 *
 * The denormalised `leaderboard` balance is a cache. If it ever disagrees with the sum of
 * `coinTransactions`, the ledger wins — this exists to make that repair explicit and
 * routine rather than a manual console operation.
 */
exports.recomputeBalance = onCall(async (request) => {
  const uid = request.auth && request.auth.uid;
  if (!uid) throw new HttpsError("unauthenticated", "Sign in first.");

  const ledger = await db.collection("coinTransactions").where("uid", "==", uid).get();
  const balance = ledger.docs.reduce((sum, d) => sum + (d.get("amount") || 0), 0);

  await db.collection("leaderboard").doc(uid).set(
    { uid, bioCoins: balance, updatedAt: admin.firestore.FieldValue.serverTimestamp() },
    { merge: true }
  );

  return { balance };
});

/**
 * Keeps the leaderboard in step when a transaction is written by any path.
 *
 * Belt and braces alongside the in-transaction update in `awardOnce`: if a transaction is
 * ever created by an admin script or a migration, the cache still follows.
 */
exports.onCoinTransactionCreated = onDocumentCreated(
  "coinTransactions/{transactionId}",
  async (event) => {
    const snapshot = event.data;
    if (!snapshot) return;
    const uid = snapshot.get("uid");
    if (!uid) return;

    const ledger = await db.collection("coinTransactions").where("uid", "==", uid).get();
    const balance = ledger.docs.reduce((sum, d) => sum + (d.get("amount") || 0), 0);

    await db.collection("leaderboard").doc(uid).set(
      { uid, bioCoins: balance, updatedAt: admin.firestore.FieldValue.serverTimestamp() },
      { merge: true }
    );
  }
);
